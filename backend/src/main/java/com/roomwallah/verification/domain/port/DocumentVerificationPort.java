package com.roomwallah.verification.domain.port;

public interface DocumentVerificationPort {
    boolean verifyDocument(String documentNumber, String documentType, String correlationId);
}
