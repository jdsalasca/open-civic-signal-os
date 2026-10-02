CREATE TABLE community_rooms (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    project_board_id UUID NULL REFERENCES community_project_boards(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    topic VARCHAR(280) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_community_rooms_community_archived
    ON community_rooms (community_id, archived, created_at DESC);

CREATE TABLE community_room_messages (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES community_rooms(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_room_messages_room_created
    ON community_room_messages (room_id, created_at DESC);

CREATE TABLE community_room_mentions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES community_room_messages(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES community_rooms(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    mentioned_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL
);

CREATE INDEX idx_community_room_mentions_user_read
    ON community_room_mentions (mentioned_user_id, read_at, created_at DESC);

CREATE INDEX idx_community_room_mentions_room
    ON community_room_mentions (room_id, created_at DESC);

CREATE TABLE community_room_mutes (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES community_rooms(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_community_room_mutes_room_user
    ON community_room_mutes (room_id, user_id);