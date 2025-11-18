package edu.ucsal.fiadopay.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
    @NotBlank String paymentId
) {}
