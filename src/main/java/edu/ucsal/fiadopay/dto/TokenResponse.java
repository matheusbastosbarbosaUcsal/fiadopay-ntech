package edu.ucsal.fiadopay.dto;
public record TokenResponse(String access_token, String token_type, long expires_in) {}
