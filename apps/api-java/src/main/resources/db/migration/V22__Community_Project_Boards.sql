CREATE TABLE community_project_boards (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    linked_proposal_id UUID NULL REFERENCES community_proposals(id) ON DELETE SET NULL,
    title VARCHAR(160) NOT NULL,
    summary TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_project_boards_community_updated
    ON community_project_boards (community_id, updated_at DESC);

CREATE TABLE community_project_tasks (
    id UUID PRIMARY KEY,
    project_board_id UUID NOT NULL REFERENCES community_project_boards(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    details TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    assignee_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    due_date DATE NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_project_tasks_board_status_sort
    ON community_project_tasks (project_board_id, status, sort_order, created_at);

CREATE TABLE community_project_task_comments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES community_project_tasks(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_project_task_comments_task_created
    ON community_project_task_comments (task_id, created_at ASC);
