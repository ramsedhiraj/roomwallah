package com.roomwallah.identity.application.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;

public interface RegistrationService {
    User register(String fullName, String email, String phone, String password, UserRole role);
}
