CREATE TABLE governance_documents (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    summary TEXT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    current_version_number INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_governance_documents_community_updated
    ON governance_documents (community_id, updated_at DESC);

CREATE TABLE governance_document_tags (
    document_id UUID NOT NULL REFERENCES governance_documents(id) ON DELETE CASCADE,
    position_index INT NOT NULL,
    tag VARCHAR(120) NOT NULL,
    PRIMARY KEY (document_id, position_index)
);

CREATE INDEX idx_governance_document_tags_tag
    ON governance_document_tags (tag);

CREATE TABLE governance_document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES governance_documents(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    content TEXT NOT NULL,
    change_summary TEXT NOT NULL,
    source_url VARCHAR(1200) NULL,
    effective_date DATE NULL,
    meeting_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_governance_document_versions_doc_version
    ON governance_document_versions (document_id, version_number);
