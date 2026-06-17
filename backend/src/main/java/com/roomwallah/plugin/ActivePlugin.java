package com.roomwallah.plugin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "active_plugins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivePlugin {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "plugin_name", nullable = false, length = 255)
    private String pluginName;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // ACTIVE, INACTIVE, ERROR

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
