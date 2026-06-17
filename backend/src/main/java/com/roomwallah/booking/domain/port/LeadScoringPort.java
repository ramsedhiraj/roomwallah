package com.roomwallah.booking.domain.port;

import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;

import java.util.UUID;

public interface LeadScoringPort {
    LeadScoreExplanation calculateLeadScore(UUID tenantId, UUID ownerId);
}
