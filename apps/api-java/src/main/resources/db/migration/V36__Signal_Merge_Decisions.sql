CREATE TABLE signal_merge_decisions (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    target_signal_id UUID NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
    decided_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    similarity_threshold DOUBLE PRECISION NOT NULL,
    suggested_similarities TEXT NOT NULL,
    merged_signal_ids TEXT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    note TEXT NULL
);

CREATE INDEX idx_signal_merge_decisions_community_decided
    ON signal_merge_decisions (community_id, decided_at DESC);

CREATE INDEX idx_signal_merge_decisions_target
    ON signal_merge_decisions (target_signal_id, decided_at DESC);

COMMENT ON COLUMN signal_merge_decisions.suggested_similarities IS
    'Similarity scores the algorithm proposed, kept verbatim so a reviewer can be shown what the suggestion was based on after the fact.';
COMMENT ON COLUMN signal_merge_decisions.decision IS
    'APPROVED when the merge was applied, REJECTED when the suggestion was declined, SPLIT when an earlier merge was undone.';