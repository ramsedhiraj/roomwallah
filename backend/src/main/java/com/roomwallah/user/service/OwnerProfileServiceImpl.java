package com.roomwallah.user.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.ObjectStoragePort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerProfileServiceImpl implements OwnerProfileService {

    private final UserRepository userRepository;
    private final ObjectStoragePort objectStoragePort;

    @Override
    public PublicOwnerProfileResponse getPublicOwnerProfile(UUID ownerId) {
        log.info("Fetching public owner profile for user: {}", ownerId);

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with ID: " + ownerId));

        if (user.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("User with ID: " + ownerId + " is not a property owner");
        }

        String avatarUrl = user.getAvatarKey() != null ? objectStoragePort.getPublicUrl(user.getAvatarKey()) : null;

        return PublicOwnerProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getFullName())
                .avatarUrl(avatarUrl)
                .joinDate(user.getCreatedAt())
                .verifiedOwner(user.isIdentityVerified())
                .trustScore(85) // Placeholder trust score
                .listingsCount(3) // Placeholder listings count
                .build();
    }
}
