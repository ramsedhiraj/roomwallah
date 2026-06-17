package com.roomwallah.property;

import com.roomwallah.property.infrastructure.adapter.DefaultBrokerPolicyAdapter;
import com.roomwallah.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyListingLimitsTest {

    private DefaultBrokerPolicyAdapter brokerPolicyAdapter;

    @BeforeEach
    public void setUp() {
        brokerPolicyAdapter = new DefaultBrokerPolicyAdapter();
        ReflectionTestUtils.setField(brokerPolicyAdapter, "defaultListingLimit", 3);
    }

    @Test
    public void testMaxActiveListingsForUnverifiedUser() {
        User user = new User();
        user.setIdentityVerified(false);

        int maxListings = brokerPolicyAdapter.getMaxActiveListings(user);
        assertEquals(3, maxListings);
    }

    @Test
    public void testMaxActiveListingsForVerifiedUser() {
        User user = new User();
        user.setIdentityVerified(true);

        int maxListings = brokerPolicyAdapter.getMaxActiveListings(user);
        assertEquals(6, maxListings);
    }

    @Test
    public void testBrokerRiskFlagged() {
        User user = new User();
        user.setIdentityVerified(false);

        // Limit is 3. 2 listings should not flag risk
        assertFalse(brokerPolicyAdapter.isBrokerRiskFlagged(user, 2));

        // 3 listings should not flag risk (count is <= max)
        assertFalse(brokerPolicyAdapter.isBrokerRiskFlagged(user, 3));

        // 4 listings should flag risk (count > max)
        assertTrue(brokerPolicyAdapter.isBrokerRiskFlagged(user, 4));
    }
}
