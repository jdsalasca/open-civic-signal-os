CREATE TABLE community_trust_pulse_responses (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    respondent_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- The closed month the response is about, as YYYY-MM. A pulse is a judgement about a period,
    -- not a timestamped event, so the period is the key and the submission time is metadata.
    period_key VARCHAR(7) NOT NULL,
    trust_score INTEGER NOT NULL,
    responsiveness_score INTEGER NOT NULL,
    transparency_score INTEGER NOT NULL,
    comment TEXT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One response per person per period. A resident changing their mind updates their answer rather
-- than stacking a second one, because a pulse that counts the loudest respondent twice is not a
-- measure of the community.
CREATE UNIQUE INDEX idx_trust_pulse_respondent_period
    ON community_trust_pulse_responses (community_id, respondent_id, period_key);

CREATE INDEX idx_trust_pulse_community_period
    ON community_trust_pulse_responses (community_id, period_key);

COMMENT ON COLUMN community_trust_pulse_responses.trust_score IS
    'Self-reported 1-5. Deliberately coarse: a finer scale invites false precision from a small sample.';
COMMENT ON COLUMN community_trust_pulse_responses.comment IS
    'Optional free text. Never published verbatim; it is a prompt for a human to read, not a dataset.';
COMMENT ON TABLE community_trust_pulse_responses IS
    'Resident-reported trust, kept separate from the computed trust metrics. The computed cards measure what the platform did; this measures what residents think of it, and conflating the two would let the platform grade its own homework.';