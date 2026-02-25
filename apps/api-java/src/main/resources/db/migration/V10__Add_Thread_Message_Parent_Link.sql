-- V10__Add_Thread_Message_Parent_Link.sql
-- Enables nested thread replies (message-to-message conversation).

ALTER TABLE community_thread_messages
    ADD COLUMN IF NOT EXISTS parent_message_id UUID;

ALTER TABLE community_thread_messages
    ADD CONSTRAINT fk_thread_message_parent
    FOREIGN KEY (parent_message_id)
    REFERENCES community_thread_messages(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_thread_message_thread_parent
    ON community_thread_messages(thread_id, parent_message_id);
