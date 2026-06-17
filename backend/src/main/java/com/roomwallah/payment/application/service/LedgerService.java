package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.LedgerEntry;
import java.util.List;

public interface LedgerService {
    void postTransaction(String description, List<LedgerEntry> entries);
}
