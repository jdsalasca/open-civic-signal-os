CREATE TABLE community_resources (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    location_label VARCHAR(160) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    min_notice_hours INTEGER NOT NULL DEFAULT 0,
    max_booking_hours INTEGER NOT NULL DEFAULT 8,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_community_resources_community_archived
    ON community_resources (community_id, archived, created_at DESC);

CREATE TABLE community_resource_bookings (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES community_resources(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(280) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    decision_note VARCHAR(280) NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,
    decided_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMP NULL
);

CREATE INDEX idx_community_resource_bookings_resource_window
    ON community_resource_bookings (resource_id, starts_at, ends_at);

CREATE INDEX idx_community_resource_bookings_requester
    ON community_resource_bookings (requester_id, requested_at DESC);