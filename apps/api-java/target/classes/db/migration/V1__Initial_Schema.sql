-- V1__Initial_Schema.sql
-- Professional baseline for Signal OS

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    roles VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE signals (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    urgency INT DEFAULT 3,
    impact INT DEFAULT 3,
    affected_people INT DEFAULT 0,
    community_votes INT DEFAULT 0,
    priority_score DOUBLE PRECISION DEFAULT 0.0,
    status VARCHAR(50) DEFAULT 'NEW',
    moderation_reason TEXT,
    author_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signal_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE votes (
    id UUID PRIMARY KEY, -- Fixed: Change to UUID to match Java Entity
    user_id UUID NOT NULL,
    signal_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_vote_signal FOREIGN KEY (signal_id) REFERENCES signals(id),
    CONSTRAINT unique_user_signal_vote UNIQUE (user_id, signal_id)
);

CREATE TABLE signal_merged_from (
    signal_id UUID NOT NULL,
    merged_from UUID NOT NULL,
    CONSTRAINT fk_merged_signal FOREIGN KEY (signal_id) REFERENCES signals(id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    channel VARCHAR(50),
    message TEXT,
    recipient_group VARCHAR(100),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
