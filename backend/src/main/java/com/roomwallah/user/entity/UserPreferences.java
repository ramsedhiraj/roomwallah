package com.roomwallah.user.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
public class UserPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "dark_mode_preferred", nullable = false)
    private boolean darkModePreferred = false;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled", nullable = false)
    private boolean pushNotificationsEnabled = true;

    @Column(name = "marketing_notifications_enabled", nullable = false)
    private boolean marketingNotificationsEnabled = false;

    @Column(name = "preferred_language", nullable = false, length = 50)
    private String preferredLanguage = "en";

    @Column(name = "preferred_contact_method", nullable = false, length = 50)
    private String preferredContactMethod = "EMAIL";
}
