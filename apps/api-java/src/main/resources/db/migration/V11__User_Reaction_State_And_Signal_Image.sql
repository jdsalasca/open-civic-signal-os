-- V11__User_Reaction_State_And_Signal_Image.sql
-- Adds per-user reaction state and image URL support for civic signals.

ALTER TABLE signals
    ADD COLUMN IF NOT EXISTS image_url TEXT;

CREATE TABLE IF NOT EXISTS user_reactions (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL,
    parent_type VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL,
    reaction_type VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_reactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_reactions_parent_user UNIQUE (parent_id, parent_type, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_reactions_parent ON user_reactions(parent_type, parent_id);
CREATE INDEX IF NOT EXISTS idx_user_reactions_user ON user_reactions(user_id);
