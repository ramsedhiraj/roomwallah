package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visit_calendars")
@Getter
@Setter
public class VisitCalendar extends BaseEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "recurrence_rules_json", columnDefinition = "TEXT")
    private String recurrenceRulesJson;

    @Column(name = "blackout_dates_json", columnDefinition = "TEXT")
    private String blackoutDatesJson;

    @Column(name = "vacation_start")
    private Instant vacationStart;

    @Column(name = "vacation_end")
    private Instant vacationEnd;
}
