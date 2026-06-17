package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;

public interface PasswordManagementService {
    void changePassword(User user, ChangePasswordRequest request);
}
