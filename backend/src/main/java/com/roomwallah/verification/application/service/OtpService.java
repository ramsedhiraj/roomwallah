package com.roomwallah.verification.application.service;

import com.roomwallah.notification.port.NotificationSenderPort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.verification.domain.entity.UserVerificationLog;
import com.roomwallah.verification.domain.repository.UserVerificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final UserVerificationLogRepository logRepository;
    private final NotificationSenderPort notificationSenderPort;
    private final Clock clock;

    private static final String OTP_VALUE_PREFIX = "otp_value:";
    private static final String OTP_COOLDOWN_PREFIX = "otp_cooldown:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void generateOtp(UUID userId, String target, String type) {
        String cooldownKey = OTP_COOLDOWN_PREFIX + type + ":" + userId;
        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            log.warn("MFA cooldown active for user: {}, type: {}", userId, type);
            throw new IllegalStateException("Please wait 60 seconds before requesting a new OTP.");
        }

        // Generate 6-digit random code
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        Instant now = Instant.now(clock);

        // Save to Redis
        String valueKey = OTP_VALUE_PREFIX + type + ":" + userId;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + type + ":" + userId;

        redisTemplate.opsForValue().set(valueKey, otpCode, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(attemptsKey, 0, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);

        // Update active verification log to EXPIRED if any exists in pending
        Optional<UserVerificationLog> activeLogOpt = logRepository
                .findFirstByUserIdAndVerificationTypeOrderByCreatedAtDesc(userId, type);
        if (activeLogOpt.isPresent()) {
            UserVerificationLog activeLog = activeLogOpt.get();
            if ("PENDING".equals(activeLog.getStatus())) {
                activeLog.setStatus("EXPIRED");
                logRepository.save(activeLog);
            }
        }

        // Save new log record
        UserVerificationLog verificationLog = new UserVerificationLog();
        verificationLog.setUserId(userId);
        verificationLog.setVerificationType(type);
        verificationLog.setStatus("PENDING");
        verificationLog.setAttempts(0);
        verificationLog.setRequestedAt(now);
        verificationLog.setVersion(0L);
        logRepository.save(verificationLog);

        // Send OTP
        log.info("Sending OTP for user: {}, type: {}, code: {}", userId, type, otpCode);
        if ("EMAIL_OTP".equalsIgnoreCase(type)) {
            notificationSenderPort.sendEmail(target, "RoomWallah Email Verification", "Your verification code is: " + otpCode);
        } else if ("MOBILE_OTP".equalsIgnoreCase(type)) {
            notificationSenderPort.sendSms(target, "RoomWallah Verification Code: " + otpCode);
            // Optionally, also send whatsapp
            try {
                notificationSenderPort.sendWhatsApp(target, "RoomWallah Verification Code: " + otpCode);
            } catch (Exception e) {
                log.warn("Failed to send WhatsApp verification code: {}", e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Unknown OTP verification type: " + type);
        }
    }

    @Transactional
    public boolean verifyOtp(UUID userId, String code, String type) {
        Instant now = Instant.now(clock);

        Optional<UserVerificationLog> activeLogOpt = logRepository
                .findFirstByUserIdAndVerificationTypeOrderByCreatedAtDesc(userId, type);
        if (activeLogOpt.isEmpty()) {
            log.warn("No verification log found for user: {}, type: {}", userId, type);
            return false;
        }

        UserVerificationLog verificationLog = activeLogOpt.get();
        if (!"PENDING".equals(verificationLog.getStatus())) {
            log.warn("Verification request status is not PENDING (Status: {}) for user: {}, type: {}", 
                    verificationLog.getStatus(), userId, type);
            return false;
        }

        String valueKey = OTP_VALUE_PREFIX + type + ":" + userId;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + type + ":" + userId;
        String cooldownKey = OTP_COOLDOWN_PREFIX + type + ":" + userId;

        String storedOtp = (String) redisTemplate.opsForValue().get(valueKey);
        if (storedOtp == null) {
            log.warn("OTP expired or not found in Redis for user: {}, type: {}", userId, type);
            verificationLog.setStatus("EXPIRED");
            logRepository.save(verificationLog);
            return false;
        }

        Integer attemptsObj = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsObj != null ? attemptsObj : 0;
        if (attempts >= 3) {
            log.warn("Too many failed OTP verification attempts ({}) for user: {}, type: {}", attempts, userId, type);
            verificationLog.setStatus("FAILED");
            logRepository.save(verificationLog);
            redisTemplate.delete(valueKey);
            redisTemplate.delete(attemptsKey);
            redisTemplate.delete(cooldownKey);
            return false;
        }

        if (storedOtp.equals(code)) {
            // Success! Clean up keys
            redisTemplate.delete(valueKey);
            redisTemplate.delete(attemptsKey);
            redisTemplate.delete(cooldownKey);

            // Update user status
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            if ("EMAIL_OTP".equalsIgnoreCase(type)) {
                user.setEmailVerified(true);
                user.setEmailVerifiedAt(now);
            } else if ("MOBILE_OTP".equalsIgnoreCase(type)) {
                user.setPhoneVerified(true);
                user.setPhoneVerifiedAt(now);
            }
            userRepository.save(user);

            // Update log
            verificationLog.setStatus("VERIFIED");
            verificationLog.setVerifiedAt(now);
            verificationLog.setAttempts(attempts + 1);
            logRepository.save(verificationLog);

            log.info("OTP verification successful for user: {}, type: {}", userId, type);
            return true;
        } else {
            // Mismatch
            attempts++;
            redisTemplate.opsForValue().increment(attemptsKey);
            verificationLog.setAttempts(attempts);

            if (attempts >= 3) {
                log.warn("OTP attempts threshold reached for user: {}, type: {}", userId, type);
                verificationLog.setStatus("FAILED");
                redisTemplate.delete(valueKey);
                redisTemplate.delete(attemptsKey);
                redisTemplate.delete(cooldownKey);
            }
            logRepository.save(verificationLog);
            log.warn("Invalid OTP code submitted for user: {}, type: {}, attempt: {}", userId, type, attempts);
            return false;
        }
    }
}
