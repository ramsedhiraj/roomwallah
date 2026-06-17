package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;

public interface LoginService {
    AuthResponse login(String identity, String credentials, AuthType authType, String deviceName, String browser, String os, String ipAddress);
}
