-- V7__Add_Engagement_And_Reaction_Tables.sql
-- Align persistence schema with engagement entities and reaction maps.

CREATE TABLE IF NOT EXISTS blog_reactions (
    blog_id UUID NOT NULL,
    reaction_type VARCHAR(100) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_blog_reactions PRIMARY KEY (blog_id, reaction_type),
    CONSTRAINT fk_blog_reactions_blog FOREIGN KEY (blog_id) REFERENCES community_blog_posts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS signal_reactions (
    signal_id UUID NOT NULL,
    reaction_type VARCHAR(100) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_signal_reactions PRIMARY KEY (signal_id, reaction_type),
    CONSTRAINT fk_signal_reactions_signal FOREIGN KEY (signal_id) REFERENCES signals(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS civic_comments (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL,
    parent_type VARCHAR(50) NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_civic_comment_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS signal_status_history (
    id UUID PRIMARY KEY,
    signal_id UUID NOT NULL,
    status_from VARCHAR(50) NOT NULL,
    status_to VARCHAR(50) NOT NULL,
    changed_by VARCHAR(255),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signal_status_history_signal FOREIGN KEY (signal_id) REFERENCES signals(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_blog_reactions_blog_id ON blog_reactions(blog_id);
CREATE INDEX IF NOT EXISTS idx_signal_reactions_signal_id ON signal_reactions(signal_id);
CREATE INDEX IF NOT EXISTS idx_civic_comments_parent ON civic_comments(parent_id, parent_type);
CREATE INDEX IF NOT EXISTS idx_signal_status_history_signal ON signal_status_history(signal_id, created_at DESC);
