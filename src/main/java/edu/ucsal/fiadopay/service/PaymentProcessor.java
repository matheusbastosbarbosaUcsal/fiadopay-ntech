package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PaymentProcessor {
    private final WebhookDeliveryService webhookDeliveryService;
    private final PaymentRepository payments;

    @Value("${fiadopay.processing-delay-ms}") long delay;
    @Value("${fiadopay.failure-rate}") double failRate;

    public PaymentProcessor(WebhookDeliveryService webhookDeliveryService, PaymentRepository payments) {
        this.webhookDeliveryService = webhookDeliveryService;
        this.payments = payments;
    }

    protected void processAndWebhook(String paymentId){
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {

        }
        var p = payments.findById(paymentId).orElse(null);
        if (p==null) return;

        var approved = Math.random() > failRate;
        p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        webhookDeliveryService.sendWebhook(p);
    }
}
