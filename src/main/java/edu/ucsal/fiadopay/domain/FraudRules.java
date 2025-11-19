package edu.ucsal.fiadopay.domain;

import edu.ucsal.fiadopay.annotations.AntiFraud;
import edu.ucsal.fiadopay.dto.PaymentRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FraudRules {
    @AntiFraud(name = "BoletoAltoRisco", limit = 5000.00)
    public boolean verificarBoleto(PaymentRequest req) {
        if ("BOLETO".equalsIgnoreCase(req.method()) && 
            req.amount().compareTo(new BigDecimal("5000.00")) > 0) {
            return true;
        }
        return false;
    }

    @AntiFraud(name = "CartaoMuitoAlto", limit = 10000.00)
    public boolean verificarCartao(PaymentRequest req) {
        if ("CARD".equalsIgnoreCase(req.method()) && 
            req.amount().compareTo(new BigDecimal("10000.00")) > 0) {
            return true;
        }
        return false;
    }
    
    @AntiFraud(name = "PixMuitoAlto", limit = 10000.00)
    public boolean verificarPix(PaymentRequest req) {
        if ("PIX".equalsIgnoreCase(req.method()) &&
            req.amount().compareTo(new BigDecimal("10000.00")) > 0) {
            return true;
        }
        return false;
    }
}
