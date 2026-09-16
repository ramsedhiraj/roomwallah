package com.roomwallah.identity.application.service;

public interface PasswordResetService {
    void forgotPassword(String email);
    void resetPassword(String email, String code, String newPassword, String confirmPassword);
}
