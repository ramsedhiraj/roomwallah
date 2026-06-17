package com.roomwallah.payment.application.exception;

public class BalancedLedgerException extends RuntimeException {
    public BalancedLedgerException(String message) {
        super(message);
    }
}
