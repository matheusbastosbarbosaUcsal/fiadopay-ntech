package edu.ucsal.fiadopay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository deliveries;
    private final MerchantService merchantService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    @Value("${fiadopay.webhook-secret}") String secret;

    public WebhookDeliveryService(WebhookDeliveryRepository deliveries, MerchantService merchantService, ObjectMapper objectMapper, ExecutorService executorService) {
        this.deliveries = deliveries;
        this.merchantService = merchantService;
        this.objectMapper = objectMapper;
        this.executorService = executorService;
    }

    protected void tryDeliver(Long deliveryId){
        var d = deliveries.findById(deliveryId).orElse(null);
        if (d==null) return;
        try {
            var client = HttpClient.newHttpClient();
            var req = HttpRequest.newBuilder(URI.create(d.getTargetUrl()))
                    .header("Content-Type","application/json")
                    .header("X-Event-Type", d.getEventType())
                    .header("X-Signature", d.getSignature())
                    .POST(HttpRequest.BodyPublishers.ofString(d.getPayload()))
                    .build();
            var res = client.send(req, HttpResponse.BodyHandlers.ofString());
            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(res.statusCode()>=200 && res.statusCode()<300);
            deliveries.save(d);
            if(!d.isDelivered() && d.getAttempts()<5){
                Thread.sleep(1000L * d.getAttempts());
                tryDeliver(deliveryId);
            }
        } catch (Exception e){
            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(false);
            deliveries.save(d);
            if (d.getAttempts()<5){
                try {
                    Thread.sleep(1000L * d.getAttempts());
                } catch (InterruptedException ignored) {}
                tryDeliver(deliveryId);
            }
        }
    }

    protected void sendWebhook(Payment p){
        var merchant = merchantService.merchantId(p);
        if (merchant==null || merchant.getWebhookUrl()==null || merchant.getWebhookUrl().isBlank()) return;

        String payload;
        try {
            var data = Map.of(
                    "paymentId", p.getId(),
                    "status", p.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            );
            var event = Map.of(
                    "id", "evt_"+ UUID.randomUUID().toString().substring(0,8),
                    "type", "payment.updated",
                    "data", data
            );
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            // fallback mínimo: não envia webhook se falhar a serialização
            return;
        }

        var signature = PaymentService.hmac(payload, secret);

        var delivery = deliveries.save(WebhookDelivery.builder()
                .eventId("evt_"+UUID.randomUUID().toString().substring(0,8))
                .eventType("payment.updated")
                .paymentId(p.getId())
                .targetUrl(merchant.getWebhookUrl())
                .signature(signature)
                .payload(payload)
                .attempts(0)
                .delivered(false)
                .lastAttemptAt(null)
                .build());

        webhookAsync(() ->  tryDeliver(delivery.getId()));
    }

    protected void webhookAsync(Runnable t){
        executorService.submit(t);
    }

}
