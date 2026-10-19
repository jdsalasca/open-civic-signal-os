CREATE TABLE community_proposals (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    related_signal_id UUID NULL REFERENCES signals(id) ON DELETE SET NULL,
    title VARCHAR(160) NOT NULL,
    template_key VARCHAR(60) NOT NULL DEFAULT 'STANDARD_COMMUNITY_PROPOSAL',
    status VARCHAR(40) NOT NULL DEFAULT 'PROPOSED',
    problem_statement TEXT NOT NULL,
    proposed_solution TEXT NOT NULL,
    estimated_cost TEXT NOT NULL,
    beneficiaries_summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE community_proposal_links (
    proposal_id UUID NOT NULL REFERENCES community_proposals(id) ON DELETE CASCADE,
    position_index INTEGER NOT NULL,
    url VARCHAR(1200) NOT NULL,
    PRIMARY KEY (proposal_id, position_index)
);

CREATE INDEX idx_community_proposals_community_updated
    ON community_proposals (community_id, updated_at DESC);
