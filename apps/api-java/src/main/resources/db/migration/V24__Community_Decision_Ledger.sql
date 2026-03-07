CREATE TABLE community_decisions (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    linked_proposal_id UUID NULL REFERENCES community_proposals(id) ON DELETE SET NULL,
    governance_document_id UUID NULL REFERENCES governance_documents(id) ON DELETE SET NULL,
    project_board_id UUID NULL REFERENCES community_project_boards(id) ON DELETE SET NULL,
    decided_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    execution_owner_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    decision_type VARCHAR(40) NOT NULL,
    decision_status VARCHAR(40) NOT NULL,
    approval_basis_type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary TEXT NOT NULL,
    approval_basis_summary TEXT NOT NULL,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_decisions_community_decided
    ON community_decisions (community_id, decided_at DESC, updated_at DESC);
