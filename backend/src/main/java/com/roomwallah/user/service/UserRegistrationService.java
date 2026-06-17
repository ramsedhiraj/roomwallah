package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;

public interface UserRegistrationService {
    User registerUser(String fullName, String email, String phone, String password, UserRole role);
}
