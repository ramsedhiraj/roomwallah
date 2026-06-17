package com.roomwallah.trust.presentation.dto;

import com.roomwallah.trust.domain.entity.VerificationLevel;
import com.roomwallah.trust.domain.entity.VerificationProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class VerificationSubmissionRequest {
    @NotNull(message = "Verification level is required")
    private VerificationLevel level;

    @NotNull(message = "Verification provider is required")
    private VerificationProvider provider;

    private List<UUID> mediaIds;
}
