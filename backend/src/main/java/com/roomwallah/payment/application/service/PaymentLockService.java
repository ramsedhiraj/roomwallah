package com.roomwallah.payment.application.service;

import java.util.function.Supplier;

public interface PaymentLockService {
    <T> T executeWithLock(String lockKey, long leaseTimeSeconds, Supplier<T> task);
}
