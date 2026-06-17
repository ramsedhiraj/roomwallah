package com.roomwallah.user.presentation.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    private LocalDate dateOfBirth;

    @Size(max = 50, message = "Gender must not exceed 50 characters")
    private String gender;

    // Preferences
    private Boolean darkModePreferred;
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean marketingNotificationsEnabled;
    private String preferredLanguage;
    private String preferredContactMethod;
}
