-- BE-P2-09: Versioned Database Schema

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS signals (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    urgency INT NOT NULL,
    impact INT NOT NULL,
    affected_people INT DEFAULT 0,
    community_votes INT DEFAULT 0,
    priority_score DOUBLE PRECISION DEFAULT 0.0,
    status VARCHAR(50) NOT NULL,
    moderation_reason TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS signal_merged_from (
    signal_id UUID NOT NULL REFERENCES signals(id),
    merged_from UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    recipient_group VARCHAR(100) NOT NULL,
    sent_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS votes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    signal_id UUID NOT NULL REFERENCES signals(id),
    UNIQUE(user_id, signal_id)
);
