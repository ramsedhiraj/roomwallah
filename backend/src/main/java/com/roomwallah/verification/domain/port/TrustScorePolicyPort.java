package com.roomwallah.verification.domain.port;

public interface TrustScorePolicyPort {
    int getIdentityPoints();
    int getPhonePoints();
    int getEmailPoints();
    int getPropertyPoints();
    int getVideoWalkthroughPoints();
    int getReviewPoints();
    int getFraudPenalty();
    
    int getBronzeThreshold();
    int getSilverThreshold();
    int getGoldThreshold();
    int getDiamondThreshold();
}
