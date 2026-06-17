package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRegistrationService userRegistrationService;
    private final PasswordEncoderPort passwordEncoderPort;

    // Password policy regex: min 8 chars, 1 upper, 1 lower, 1 digit, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Override
    public User register(String fullName, String email, String phone, String password, UserRole role) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long and contain at least " +
                    "one uppercase letter, one lowercase letter, one digit, and one special character."
            );
        }

        String hashed = passwordEncoderPort.encode(password);

        return userRegistrationService.registerUser(fullName, email, phone, hashed, role);
    }
}
