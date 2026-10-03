CREATE TABLE institutional_ticket_handoffs (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    signal_id UUID NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
    ticket_ref VARCHAR(40) NOT NULL,
    external_category_code VARCHAR(80) NOT NULL,
    -- The institution's own reference, supplied by whoever sent the export. Nullable because a
    -- community may hand over a file and never learn the city's id for it.
    external_ticket_id VARCHAR(120) NULL,
    handed_off_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    handed_off_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- The SLA the community is holding the institution to, captured at handoff so a later change
    -- does not retroactively rewrite whether a past handoff was late.
    sla_target_days INTEGER NOT NULL,
    acknowledged_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    last_checked_at TIMESTAMP NULL,
    note TEXT NULL
);

CREATE INDEX idx_ticket_handoffs_community_handed
    ON institutional_ticket_handoffs (community_id, handed_off_at DESC);

CREATE INDEX idx_ticket_handoffs_signal
    ON institutional_ticket_handoffs (signal_id);

-- One open handoff per signal. Re-sending the same report to the same institution would create a
-- second clock for the same complaint, and then nobody could say which one was late.
CREATE UNIQUE INDEX idx_ticket_handoffs_open_per_signal
    ON institutional_ticket_handoffs (signal_id, ticket_ref);

COMMENT ON COLUMN institutional_ticket_handoffs.external_ticket_id IS
    'The institution''s reference, when the community learns it. The platform cannot read the city system, so this is recorded rather than fetched.';
COMMENT ON COLUMN institutional_ticket_handoffs.sla_target_days IS
    'Captured at handoff. A later change to the community SLA must not retroactively rewrite whether a past handoff was late.';
COMMENT ON TABLE institutional_ticket_handoffs IS
    'What the platform handed to an institution, and what it knows about the outcome. The platform cannot observe the institution''s queue, so every status here is either supplied by a person or derived from our own signal lifecycle, never inferred from the city system.';