-- V13__Community_Hierarchy.sql
-- Parent-child community hierarchy and breadcrumb navigation support

ALTER TABLE communities ADD COLUMN parent_community_id UUID;
ALTER TABLE communities
    ADD CONSTRAINT fk_community_parent
    FOREIGN KEY (parent_community_id) REFERENCES communities(id);

CREATE INDEX idx_communities_parent ON communities(parent_community_id);
