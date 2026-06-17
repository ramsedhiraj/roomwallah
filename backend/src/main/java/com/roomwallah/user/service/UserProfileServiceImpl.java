package com.roomwallah.user.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.ObjectStoragePort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserPreferences;
import com.roomwallah.user.event.UserProfileUpdatedEvent;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final EventPublisherPort eventPublisher;
    private final AuditPort auditPort;
    private final ObjectStoragePort objectStoragePort;

    @Override
    public UserProfileResponse getProfile(User user) {
        UserPreferences prefs = user.getPreferences();
        String avatarUrl = user.getAvatarKey() != null ? objectStoragePort.getPublicUrl(user.getAvatarKey()) : null;

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .bio(user.getBio())
                .avatarUrl(avatarUrl)
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .identityVerified(user.isIdentityVerified())
                .darkModePreferred(prefs != null && prefs.isDarkModePreferred())
                .emailNotificationsEnabled(prefs != null && prefs.isEmailNotificationsEnabled())
                .pushNotificationsEnabled(prefs != null && prefs.isPushNotificationsEnabled())
                .marketingNotificationsEnabled(prefs != null && prefs.isMarketingNotificationsEnabled())
                .preferredLanguage(prefs != null ? prefs.getPreferredLanguage() : "en")
                .preferredContactMethod(prefs != null ? prefs.getPreferredContactMethod() : "EMAIL")
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", user.getEmail());

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        UserPreferences prefs = user.getPreferences();
        if (prefs == null) {
            prefs = new UserPreferences();
            prefs.setUser(user);
            user.setPreferences(prefs);
        }

        if (request.getDarkModePreferred() != null) {
            prefs.setDarkModePreferred(request.getDarkModePreferred());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            prefs.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getPushNotificationsEnabled() != null) {
            prefs.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getMarketingNotificationsEnabled() != null) {
            prefs.setMarketingNotificationsEnabled(request.getMarketingNotificationsEnabled());
        }
        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()) {
            prefs.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getPreferredContactMethod() != null && !request.getPreferredContactMethod().isBlank()) {
            prefs.setPreferredContactMethod(request.getPreferredContactMethod());
        }

        User savedUser = userRepository.save(user);

        // Publish event
        UserProfileUpdatedEvent event = UserProfileUpdatedEvent.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .updatedAt(Instant.now())
                .build();
        eventPublisher.publish(event);

        // Audit log
        auditPort.log(
                "USER_PROFILE_UPDATE",
                savedUser.getId().toString(),
                "0.0.0.0",
                Map.of(
                        "email", savedUser.getEmail(),
                        "fullName", savedUser.getFullName(),
                        "bioLength", savedUser.getBio() != null ? String.valueOf(savedUser.getBio().length()) : "0"
                )
        );

        return getProfile(savedUser);
    }
}
