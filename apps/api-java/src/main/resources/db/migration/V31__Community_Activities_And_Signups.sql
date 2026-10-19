CREATE TABLE community_activities (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    location_label VARCHAR(160) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    signup_opens_at TIMESTAMP NOT NULL,
    signup_closes_at TIMESTAMP NOT NULL,
    signup_capacity INTEGER NOT NULL,
    organizer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_community_activities_community_start
    ON community_activities (community_id, starts_at);

CREATE TABLE community_activity_signups (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES community_activities(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    volunteer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    attendance_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    attended_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_community_activity_signups_activity_volunteer
    ON community_activity_signups (activity_id, volunteer_id);

CREATE INDEX idx_community_activity_signups_activity_status
    ON community_activity_signups (activity_id, status);