-- V58: neighbours can confirm a report they also saw. One confirmation per person and signal.

CREATE TABLE signal_confirmations (
    id UUID PRIMARY KEY,
    signal_id UUID NOT NULL,
    user_id UUID NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_signal_confirmations_user_signal UNIQUE (signal_id, user_id)
);

CREATE INDEX idx_signal_confirmations_signal
    ON signal_confirmations (signal_id);
