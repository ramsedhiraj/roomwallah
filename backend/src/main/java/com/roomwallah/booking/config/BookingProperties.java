package com.roomwallah.booking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "roomwallah.booking")
public class BookingProperties {
    private boolean instantBookingEnabled = false;
    private Features features = new Features();
    private Reminder reminder = new Reminder();
    private Expiry expiry = new Expiry();

    @Getter
    @Setter
    public static class Features {
        private boolean remindersEnabled = true;
        private boolean expiryEnabled = true;
        private boolean waitlistEnabled = true;
    }

    @Getter
    @Setter
    public static class Reminder {
        private List<Integer> retryDelays = List.of(1, 2, 4, 8, 16, 32);
        private int maxAttempts = 6;
    }

    @Getter
    @Setter
    public static class Expiry {
        private int ttlMinutes = 30;
    }
}
