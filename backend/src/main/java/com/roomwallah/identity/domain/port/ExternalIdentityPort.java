package com.roomwallah.identity.domain.port;

public interface ExternalIdentityPort {
    boolean supports(String provider);
    ExternalUserInfoDto fetchUserInfo(String token);
}
