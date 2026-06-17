package com.roomwallah.identity.domain.port;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalUserInfoDto {
    private final String externalId;
    private final String email;
    private final String fullName;
}
