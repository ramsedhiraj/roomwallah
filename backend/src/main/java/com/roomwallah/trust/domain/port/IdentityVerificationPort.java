package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.valueobject.VerificationMetadata;
import java.util.UUID;

public interface IdentityVerificationPort {
    VerificationMetadata verifyIdentity(UUID userId, OwnerVerification verification);
}
