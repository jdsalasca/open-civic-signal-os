-- V8__Add_Message_Reactions_Table.sql
-- Align community thread message reactions map with schema validation.

CREATE TABLE IF NOT EXISTS message_reactions (
    message_id UUID NOT NULL,
    reaction_type VARCHAR(100) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_message_reactions PRIMARY KEY (message_id, reaction_type),
    CONSTRAINT fk_message_reactions_message FOREIGN KEY (message_id) REFERENCES community_thread_messages(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_message_reactions_message_id ON message_reactions(message_id);
