package com.roomwallah.identity.application.service;

import com.roomwallah.user.entity.User;

public interface EmailVerificationService {
    void createAndSendVerification(User user);
    void verifyEmailToken(String rawToken);
    void resendVerificationEmail(String email);
}
