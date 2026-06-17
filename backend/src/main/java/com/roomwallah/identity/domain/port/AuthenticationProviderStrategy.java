package com.roomwallah.identity.domain.port;

import com.roomwallah.user.entity.User;

public interface AuthenticationProviderStrategy {
    boolean supports(AuthType type);
    User authenticate(String identity, String credentials);
}
