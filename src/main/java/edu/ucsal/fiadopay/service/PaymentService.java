package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.annotations.PaymentCalculator;
import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.dto.PaymentRequest;
import edu.ucsal.fiadopay.dto.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
  private final MerchantService merchantService;
  private final WebhookDeliveryService webhookDeliveryService;
  private final PaymentProcessor paymentProcessor;
  private final PaymentRepository payments;

    public PaymentService(MerchantService merchantService, WebhookDeliveryService webhookDeliveryService, PaymentProcessor paymentProcessor, PaymentRepository payments) {
        this.merchantService = merchantService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.paymentProcessor = paymentProcessor;
        this.payments = payments;
    }

    @Transactional
  public PaymentResponse createPayment(String auth, String idemKey, PaymentRequest req){
    var merchant = merchantService.merchantFromAuth(auth);
    var mid = merchant.getId();

    if (idemKey != null) {
      var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, mid);
      if(existing.isPresent()) return toResponse(existing.get());
    }

    BigDecimal total = calcularTotalComJuros(req);
    Double interest = (!total.equals(req.amount()) ? 1.0 : null);

    var payment = Payment.builder()
        .id("pay_"+UUID.randomUUID().toString().substring(0,8))
        .merchantId(mid)
        .method(req.method().toUpperCase())
        .amount(req.amount())
        .currency(req.currency())
        .installments(req.installments()==null?1:req.installments())
        .monthlyInterest(interest)
        .totalWithInterest(total)
        .status(Payment.Status.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .idempotencyKey(idemKey)
        .metadataOrderId(req.metadataOrderId())
        .build();

    payments.save(payment);

    webhookDeliveryService.webhookAsync(() -> paymentProcessor.processAndWebhook(payment.getId()));

    return toResponse(payment);
  }

  public BigDecimal calcularTotalComJuros(PaymentRequest req) {
      PaymentCalculator calculator = new PaymentCalculator();

      for (Method m : calculator.getClass().getDeclaredMethods()) {
          if (m.isAnnotationPresent(PaymentMethod.class)) {
              PaymentMethod annot = m.getAnnotation(PaymentMethod.class);
              if (annot.type().equalsIgnoreCase(req.method())) {
                  try {
                      return (BigDecimal) m.invoke(calculator, req);
                  } catch (IllegalAccessException | InvocationTargetException e) {
                      throw new RuntimeException(e);
                  }
              }
          }

      }

      return req.amount();
  }

  public PaymentResponse getPayment(String id){
    return toResponse(payments.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public Map<String,Object> refund(String auth, String paymentId){
    var merchant = merchantService.merchantFromAuth(auth);
    var p = payments.findById(paymentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!merchant.getId().equals(p.getMerchantId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    p.setStatus(Payment.Status.REFUNDED);
    p.setUpdatedAt(Instant.now());
    payments.save(p);
    webhookDeliveryService.sendWebhook(p);
    return Map.of("id","ref_"+UUID.randomUUID(),"status","PENDING");
  }

  protected static String hmac(String payload, String secret){
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
      return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
    } catch (Exception e){ return ""; }
  }

  private PaymentResponse toResponse(Payment p){
    return new PaymentResponse(
        p.getId(), p.getStatus().name(), p.getMethod(),
        p.getAmount(), p.getInstallments(), p.getMonthlyInterest(),
        p.getTotalWithInterest()
    );
  }
}
