-- Flyway Migration: Booking, Visit, and Leads Schema (V16)

-- 1. Visit Slots (Supports optimistic locking via version column)
CREATE TABLE visit_slots (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    max_bookings INT NOT NULL,
    current_bookings INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_visit_slots_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

CREATE INDEX idx_visit_slots_property ON visit_slots(property_id);
CREATE INDEX idx_visit_slots_time ON visit_slots(start_time, end_time);

-- 2. Visit Calendars
CREATE TABLE visit_calendars (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    recurrence_rules_json TEXT,
    blackout_dates_json TEXT,
    vacation_start TIMESTAMP WITH TIME ZONE,
    vacation_end TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_visit_calendars_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_visit_calendars_owner ON visit_calendars(owner_id);

-- 3. Property Visits (Supports optimistic locking via version column)
CREATE TABLE property_visits (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    visit_slot_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_property_visits_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT fk_property_visits_tenant FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_property_visits_slot FOREIGN KEY (visit_slot_id) REFERENCES visit_slots(id) ON DELETE CASCADE
);

CREATE INDEX idx_property_visits_property ON property_visits(property_id);
CREATE INDEX idx_property_visits_tenant ON property_visits(tenant_id);
CREATE INDEX idx_property_visits_slot ON property_visits(visit_slot_id);

-- 4. Bookings (Supports optimistic locking via version column)
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    price_amount DECIMAL(15, 2) NOT NULL,
    price_currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    notes TEXT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    event_version BIGINT NOT NULL DEFAULT 0,
    last_event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT '1970-01-01 00:00:00+00',
    CONSTRAINT fk_bookings_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_tenant FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_bookings_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_bookings_property ON bookings(property_id);
CREATE INDEX idx_bookings_tenant ON bookings(tenant_id);
CREATE INDEX idx_bookings_owner ON bookings(owner_id);

-- 5. Booking History
CREATE TABLE booking_history (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    status_from VARCHAR(50),
    status_to VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_booking_history_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE INDEX idx_booking_history_booking ON booking_history(booking_id);

-- 6. Booking Waitlist (for visit slots)
CREATE TABLE booking_waitlist (
    id UUID PRIMARY KEY,
    visit_slot_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_booking_waitlist_slot FOREIGN KEY (visit_slot_id) REFERENCES visit_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_waitlist_tenant FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_booking_waitlist_slot ON booking_waitlist(visit_slot_id);
CREATE INDEX idx_booking_waitlist_tenant ON booking_waitlist(tenant_id);

-- 7. Leads (Supports optimistic locking via version column)
CREATE TABLE leads (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    inquiry_text TEXT,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    lead_score INT NOT NULL DEFAULT 0,
    lead_score_explanation TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_leads_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_tenant FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_leads_property ON leads(property_id);
CREATE INDEX idx_leads_tenant ON leads(tenant_id);
CREATE INDEX idx_leads_owner ON leads(owner_id);

-- 8. Lead Notes
CREATE TABLE lead_notes (
    id UUID PRIMARY KEY,
    lead_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_lead_notes_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_notes_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_lead_notes_lead ON lead_notes(lead_id);

-- 9. Lead Activity
CREATE TABLE lead_activity (
    id UUID PRIMARY KEY,
    lead_id UUID NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_lead_activity_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE
);

CREATE INDEX idx_lead_activity_lead ON lead_activity(lead_id);

-- 10. Lead Assignments
CREATE TABLE lead_assignments (
    id UUID PRIMARY KEY,
    lead_id UUID NOT NULL,
    assignee_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_lead_assignments_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_assignments_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_lead_assignments_lead ON lead_assignments(lead_id);
CREATE INDEX idx_lead_assignments_assignee ON lead_assignments(assignee_id);

-- 11. Booking Reminders
CREATE TABLE booking_reminders (
    id UUID PRIMARY KEY,
    booking_id UUID,
    visit_id UUID,
    trigger_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_booking_reminders_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_reminders_visit FOREIGN KEY (visit_id) REFERENCES property_visits(id) ON DELETE CASCADE
);

CREATE INDEX idx_booking_reminders_booking ON booking_reminders(booking_id);
CREATE INDEX idx_booking_reminders_visit ON booking_reminders(visit_id);
CREATE INDEX idx_booking_reminders_trigger ON booking_reminders(trigger_at);

-- 12. Booking Outbox
CREATE TABLE booking_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_booking_outbox_status ON booking_outbox(status);
