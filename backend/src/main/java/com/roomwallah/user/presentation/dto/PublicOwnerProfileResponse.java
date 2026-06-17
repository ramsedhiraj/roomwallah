package com.roomwallah.user.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOwnerProfileResponse {
    private UUID id;
    private String displayName;
    private String avatarUrl;
    private Instant joinDate;
    
    // Placeholders/future metrics
    private boolean verifiedOwner;
    private int trustScore;
    private int listingsCount;
}
