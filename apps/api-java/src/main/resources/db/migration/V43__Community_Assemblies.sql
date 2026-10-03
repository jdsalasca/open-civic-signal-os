CREATE TABLE community_assemblies (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    scheduled_for TIMESTAMP NOT NULL,
    location VARCHAR(200) NULL,
    -- The frozen ranking the assembly will deliberate over. Nullable because an assembly can be
    -- scheduled before its evidence is captured, and pretending otherwise would force a fake snapshot.
    snapshot_id UUID NULL REFERENCES explainability_snapshots(id) ON DELETE SET NULL,
    convened_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    opened_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    minutes TEXT NULL
);

CREATE INDEX idx_assemblies_community_scheduled
    ON community_assemblies (community_id, scheduled_for DESC);

CREATE TABLE assembly_decisions (
    id UUID PRIMARY KEY,
    assembly_id UUID NOT NULL REFERENCES community_assemblies(id) ON DELETE CASCADE,
    -- The proposal or signal the decision is about. Nullable because an assembly can decide something
    -- that is not yet a proposal, and forcing a link would make people create placeholder records.
    subject_id UUID NULL,
    subject_type VARCHAR(20) NULL,
    decision VARCHAR(30) NOT NULL,
    rationale TEXT NOT NULL,
    recorded_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assembly_decisions_assembly
    ON assembly_decisions (assembly_id, recorded_at ASC);

COMMENT ON COLUMN community_assemblies.snapshot_id IS
    'The frozen ranking the assembly deliberated over. Null means the evidence was not captured, which is reported rather than hidden.';
COMMENT ON COLUMN community_assemblies.minutes IS
    'Free-text minutes. The platform does not generate them and does not treat them as the official record.';
COMMENT ON TABLE community_assemblies IS
    'A record of what a community deliberated and decided. It is NOT the official minutes: the platform records what happened in the room, and the authoritative record remains whatever the community is legally required to keep.';