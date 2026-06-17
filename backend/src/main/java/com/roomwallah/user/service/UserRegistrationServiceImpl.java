package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserPreferences;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.event.UserRegisteredEvent;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final UserRepository userRepository;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public User registerUser(String fullName, String email, String phone, String passwordHash, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setDeleted(false);

        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        preferences.setDarkModePreferred(false);
        preferences.setEmailNotificationsEnabled(true);
        preferences.setPushNotificationsEnabled(true);
        preferences.setMarketingNotificationsEnabled(false);
        preferences.setPreferredLanguage("en");
        preferences.setPreferredContactMethod("EMAIL");
        user.setPreferences(preferences);

        User savedUser = userRepository.save(user);

        // Publish registration domain event
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .registeredAt(Instant.now(clock))
                .build();
        eventPublisher.publish(event);

        return savedUser;
    }
}
