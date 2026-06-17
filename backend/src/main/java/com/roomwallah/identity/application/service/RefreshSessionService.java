package com.roomwallah.identity.application.service;

import com.roomwallah.identity.presentation.dto.AuthResponse;

public interface RefreshSessionService {
    AuthResponse refresh(String rawRefreshToken);
}
