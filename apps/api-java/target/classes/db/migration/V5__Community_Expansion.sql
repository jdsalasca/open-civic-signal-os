-- V5__Community_Expansion.sql
-- Community memberships, scoped RBAC, threads, blog, and unified feed support

CREATE TABLE communities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE community_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    community_id UUID NOT NULL,
    role VARCHAR(64) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_membership_community FOREIGN KEY (community_id) REFERENCES communities(id),
    CONSTRAINT fk_membership_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT unique_user_community_membership UNIQUE (user_id, community_id)
);

CREATE TABLE community_membership_audit (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    target_user_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    previous_role VARCHAR(64),
    new_role VARCHAR(64) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_audit_community FOREIGN KEY (community_id) REFERENCES communities(id),
    CONSTRAINT fk_membership_audit_target_user FOREIGN KEY (target_user_id) REFERENCES users(id),
    CONSTRAINT fk_membership_audit_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE TABLE community_threads (
    id UUID PRIMARY KEY,
    source_community_id UUID NOT NULL,
    target_community_id UUID NOT NULL,
    related_signal_id UUID,
    title VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_thread_source_community FOREIGN KEY (source_community_id) REFERENCES communities(id),
    CONSTRAINT fk_thread_target_community FOREIGN KEY (target_community_id) REFERENCES communities(id),
    CONSTRAINT fk_thread_signal FOREIGN KEY (related_signal_id) REFERENCES signals(id),
    CONSTRAINT fk_thread_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE community_thread_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    author_id UUID NOT NULL,
    source_community_id UUID NOT NULL,
    content TEXT NOT NULL,
    hidden BOOLEAN DEFAULT FALSE,
    moderation_reason TEXT,
    hidden_by UUID,
    hidden_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_thread_message_thread FOREIGN KEY (thread_id) REFERENCES community_threads(id),
    CONSTRAINT fk_thread_message_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_thread_message_source_community FOREIGN KEY (source_community_id) REFERENCES communities(id),
    CONSTRAINT fk_thread_message_hidden_by FOREIGN KEY (hidden_by) REFERENCES users(id)
);

CREATE TABLE community_blog_posts (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    author_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status_tag VARCHAR(100) NOT NULL,
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_blog_community FOREIGN KEY (community_id) REFERENCES communities(id),
    CONSTRAINT fk_blog_author FOREIGN KEY (author_id) REFERENCES users(id)
);

ALTER TABLE signals ADD COLUMN community_id UUID;
ALTER TABLE signals ADD CONSTRAINT fk_signal_community FOREIGN KEY (community_id) REFERENCES communities(id);

CREATE INDEX idx_signals_community_id ON signals(community_id);
CREATE INDEX idx_membership_community_user ON community_memberships(community_id, user_id);
CREATE INDEX idx_thread_source_target ON community_threads(source_community_id, target_community_id);
CREATE INDEX idx_blog_community_published_at ON community_blog_posts(community_id, published_at DESC);
