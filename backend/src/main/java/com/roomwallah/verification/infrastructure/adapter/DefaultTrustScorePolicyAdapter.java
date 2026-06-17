package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.verification.domain.port.TrustScorePolicyPort;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DefaultTrustScorePolicyAdapter implements TrustScorePolicyPort {

    @Value("${roomwallah.trust.weights.identity:25}")
    private int identityPoints;

    @Value("${roomwallah.trust.weights.phone:10}")
    private int phonePoints;

    @Value("${roomwallah.trust.weights.email:5}")
    private int emailPoints;

    @Value("${roomwallah.trust.weights.property:30}")
    private int propertyPoints;

    @Value("${roomwallah.trust.weights.video:10}")
    private int videoWalkthroughPoints;

    @Value("${roomwallah.trust.weights.reviews:20}")
    private int reviewPoints;

    @Value("${roomwallah.trust.weights.penalty:-50}")
    private int fraudPenalty;

    @Value("${roomwallah.trust.thresholds.bronze:30}")
    private int bronzeThreshold;

    @Value("${roomwallah.trust.thresholds.silver:50}")
    private int silverThreshold;

    @Value("${roomwallah.trust.thresholds.gold:75}")
    private int goldThreshold;

    @Value("${roomwallah.trust.thresholds.diamond:90}")
    private int diamondThreshold;
}
