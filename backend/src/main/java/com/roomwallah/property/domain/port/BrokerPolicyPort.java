package com.roomwallah.property.domain.port;

import com.roomwallah.user.entity.User;

public interface BrokerPolicyPort {
    int getMaxActiveListings(User owner);
    boolean isBrokerRiskFlagged(User owner, int activeListingsCount);
}
