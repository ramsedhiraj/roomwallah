package com.roomwallah.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseLockService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean acquireLock(String name, int lockDurationSeconds) {
        Instant now = Instant.now();
        Instant lockUntil = now.plusSeconds(lockDurationSeconds);
        String lockedBy = "instance-" + UUID.randomUUID();

        Timestamp tsNow = Timestamp.from(now);
        Timestamp tsLockUntil = Timestamp.from(lockUntil);

        // Try to update existing lock if it is expired
        int updated = jdbcTemplate.update(
                "UPDATE trust_shedlock SET lock_until = ?, locked_at = ?, locked_by = ? WHERE name = ? AND (lock_until IS NULL OR lock_until < ?)",
                tsLockUntil, tsNow, lockedBy, name, tsNow
        );

        if (updated > 0) {
            log.debug("Acquired lock '{}' by updating expired lock", name);
            return true;
        }

        // Try to insert new lock
        try {
            int inserted = jdbcTemplate.update(
                    "INSERT INTO trust_shedlock (name, lock_until, locked_at, locked_by) VALUES (?, ?, ?, ?) ON CONFLICT (name) DO UPDATE SET lock_until = EXCLUDED.lock_until, locked_at = EXCLUDED.locked_at, locked_by = EXCLUDED.locked_by WHERE trust_shedlock.lock_until < ?",
                    name, tsLockUntil, tsNow, lockedBy, tsNow
            );
            if (inserted > 0) {
                log.debug("Acquired lock '{}' by inserting new lock", name);
                return true;
            }
        } catch (Exception e) {
            log.debug("Lock '{}' is already held by another instance", name);
        }

        return false;
    }

    @Transactional
    public void releaseLock(String name) {
        jdbcTemplate.update("UPDATE trust_shedlock SET lock_until = ? WHERE name = ?", Timestamp.from(Instant.now()), name);
        log.debug("Released lock '{}'", name);
    }
}
