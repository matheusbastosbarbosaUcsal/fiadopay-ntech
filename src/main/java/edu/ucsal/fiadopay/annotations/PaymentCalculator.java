package edu.ucsal.fiadopay.annotations;

import edu.ucsal.fiadopay.controller.PaymentRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentCalculator {

    @PaymentMethod(type = "CARD")
    public BigDecimal calcularCard(PaymentRequest req) {
        BigDecimal base = req.amount();
        BigDecimal factor = new BigDecimal("1.01").pow(req.installments());
        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @PaymentMethod(type = "PIX")
    public BigDecimal calcularPix(PaymentRequest req) {
        return req.amount();
    }

    @PaymentMethod(type = "BOLETO")
    public BigDecimal calcularBoleto(PaymentRequest req) {
        return req.amount();
    }

    @PaymentMethod(type = "DEBIT")
    public BigDecimal calcularDebito(PaymentRequest req) {
        return req.amount();
    }
}
