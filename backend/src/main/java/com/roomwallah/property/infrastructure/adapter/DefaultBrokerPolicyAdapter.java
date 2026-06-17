package com.roomwallah.property.infrastructure.adapter;

import com.roomwallah.property.domain.port.BrokerPolicyPort;
import com.roomwallah.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultBrokerPolicyAdapter implements BrokerPolicyPort {

    @Value("${roomwallah.property.default-listing-limit:3}")
    private int defaultListingLimit;

    @Override
    public int getMaxActiveListings(User owner) {
        // Scalable policy: verified owners get twice the default listing limit
        if (owner.isIdentityVerified()) {
            return defaultListingLimit * 2;
        }
        return defaultListingLimit;
    }

    @Override
    public boolean isBrokerRiskFlagged(User owner, int activeListingsCount) {
        return activeListingsCount > getMaxActiveListings(owner);
    }
}
