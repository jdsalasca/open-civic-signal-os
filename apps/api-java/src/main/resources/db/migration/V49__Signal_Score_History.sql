-- Score history, so a report about a closed month can show the score that month had.
--
-- priority_score is mutable and had no history, so the transparency report showed today's score for a
-- past period. That limitation was disclosed in the response rather than hidden, which is the honest
-- half; this is the other half.
--
-- Three runtime operations change a score, and all three are in PrioritizationServiceImpl: creating a
-- signal, a citizen supporting it, and a duplicate merge summing votes. Recording at those three
-- points is a statement of fact about what happened, not a policy about which score counts: the read
-- side applies the same "as of the period end" rule the report already applies to statuses.
--
-- The components travel with the score so the history explains itself rather than being a column of
-- numbers nobody can interpret later.

CREATE TABLE signal_score_entries (
    id UUID PRIMARY KEY,
    signal_id UUID NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
    priority_score DOUBLE PRECISION NOT NULL,
    urgency INTEGER NOT NULL,
    impact INTEGER NOT NULL,
    affected_people INTEGER NOT NULL,
    community_votes INTEGER NOT NULL,
    formula_version VARCHAR(20) NOT NULL,
    cause VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMP NOT NULL
);

-- The read path asks for the newest entry at or before a moment, so this is the index that matters.
CREATE INDEX idx_signal_score_entries_signal_recorded
    ON signal_score_entries (signal_id, recorded_at DESC);