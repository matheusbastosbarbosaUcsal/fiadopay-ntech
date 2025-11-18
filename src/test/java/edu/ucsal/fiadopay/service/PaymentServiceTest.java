package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.dto.PaymentRequest;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private MerchantService merchantService;

    @Mock
    private PaymentRepository payments;

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    @Mock
    private PaymentProcessor paymentProcessor;


    @Test
    void testCreatePaymentCard() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        when(merchantService.merchantFromAuth("auth123")).thenReturn(merchant);

        PaymentRequest req = new PaymentRequest("CARD", "BRL", new BigDecimal("100.00"), 2, "order123");

        var response = paymentService.createPayment("auth123", null, req);

        assertNotNull(response);
        assertEquals("CARD", response.method());
        assertTrue(response.total().compareTo(req.amount()) > 0);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captor.capture());
        assertEquals("CARD", captor.getValue().getMethod());

        verify(webhookDeliveryService).webhookAsync(any());
    }

    @Test
    void testCreatePaymentPix() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        when(merchantService.merchantFromAuth("auth123")).thenReturn(merchant);

        PaymentRequest req = new PaymentRequest("PIX", "BRL", new BigDecimal("100.00"), 1, "orderPix");

        var response = paymentService.createPayment("auth123", null, req);

        assertNotNull(response);
        assertEquals("PIX", response.method());
        assertEquals(req.amount(), response.total());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captor.capture());
        assertEquals("PIX", captor.getValue().getMethod());

        verify(webhookDeliveryService).webhookAsync(any());
    }


}