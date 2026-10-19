ALTER TABLE community_proposals
    ADD COLUMN vote_mode VARCHAR(30) NOT NULL DEFAULT 'YES_NO';

ALTER TABLE community_proposals
    ADD COLUMN vote_visibility VARCHAR(30) NOT NULL DEFAULT 'COMMUNITY';

ALTER TABLE community_proposals
    ADD COLUMN vote_eligibility VARCHAR(30) NOT NULL DEFAULT 'VERIFIED_MEMBERS';

ALTER TABLE community_proposals
    ADD COLUMN voting_opens_at TIMESTAMP NULL;

ALTER TABLE community_proposals
    ADD COLUMN voting_closes_at TIMESTAMP NULL;

CREATE TABLE community_proposal_votes (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES community_proposals(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    voter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voter_username VARCHAR(120) NOT NULL,
    membership_role VARCHAR(40) NOT NULL,
    verified_member BOOLEAN NOT NULL DEFAULT FALSE,
    vote_mode VARCHAR(40) NOT NULL,
    choice VARCHAR(20) NULL,
    score_value INTEGER NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_proposal_voter UNIQUE (proposal_id, voter_id)
);

CREATE INDEX idx_proposal_votes_proposal_created
    ON community_proposal_votes (proposal_id, created_at DESC);

CREATE TABLE community_proposal_vote_audit_events (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES community_proposals(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_proposal_vote_audit_proposal_created
    ON community_proposal_vote_audit_events (proposal_id, created_at DESC);
