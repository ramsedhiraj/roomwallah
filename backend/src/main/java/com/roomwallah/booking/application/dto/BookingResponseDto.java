package com.roomwallah.booking.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for exposing booking details to the presentation layer.
 */
public class BookingResponseDto {
    private UUID id;
    private UUID propertyId;
    private UUID ownerId;
    private UUID tenantId;
    private Instant start;
    private Instant end;
    private String notes;
    private String status;

    public BookingResponseDto() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPropertyId() { return propertyId; }
    public void setPropertyId(UUID propertyId) { this.propertyId = propertyId; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
