CREATE TABLE community_proposal_deliberation_entries (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES community_proposals(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_type VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    supporting_link VARCHAR(1200) NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    moderation_reason VARCHAR(500) NULL,
    hidden_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    hidden_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_proposal_deliberation_proposal_created
    ON community_proposal_deliberation_entries (proposal_id, created_at ASC);
