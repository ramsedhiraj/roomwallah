package com.roomwallah.property.domain.entity;

public enum PropertyStatus {
    DRAFT,
    PENDING_VERIFICATION,
    ACTIVE,
    PAUSED,
    ARCHIVED,
    REJECTED,
    SOLD,
    RENTED;

    public boolean canTransitionTo(PropertyStatus target) {
        switch (this) {
            case DRAFT:
                return target == PENDING_VERIFICATION || target == ARCHIVED;
            case PENDING_VERIFICATION:
                return target == ACTIVE || target == REJECTED || target == ARCHIVED;
            case ACTIVE:
                return target == PAUSED || target == ARCHIVED || target == SOLD || target == RENTED;
            case PAUSED:
                return target == ACTIVE || target == ARCHIVED;
            case REJECTED:
                return target == DRAFT || target == ARCHIVED;
            case ARCHIVED:
                return false; // Terminal state
            case SOLD:
            case RENTED:
                return target == ARCHIVED;
            default:
                return false;
        }
    }
}
