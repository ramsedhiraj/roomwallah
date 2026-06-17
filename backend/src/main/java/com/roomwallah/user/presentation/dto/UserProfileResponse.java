package com.roomwallah.user.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String bio;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean identityVerified;
    
    // Preferences
    private boolean darkModePreferred;
    private boolean emailNotificationsEnabled;
    private boolean pushNotificationsEnabled;
    private boolean marketingNotificationsEnabled;
    private String preferredLanguage;
    private String preferredContactMethod;
}
