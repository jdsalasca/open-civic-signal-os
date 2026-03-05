ALTER TABLE community_blog_posts
    ADD COLUMN official BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE community_blog_posts
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE community_blog_posts
    ADD COLUMN archived_by UUID;

ALTER TABLE community_blog_posts
    ADD COLUMN archived_at TIMESTAMP;

ALTER TABLE community_blog_posts
    ADD CONSTRAINT fk_blog_archived_by FOREIGN KEY (archived_by) REFERENCES users(id);

CREATE INDEX idx_blog_community_official_active
    ON community_blog_posts(community_id, pinned DESC, published_at DESC);

CREATE INDEX idx_blog_community_archived_at
    ON community_blog_posts(community_id, archived_at DESC);
