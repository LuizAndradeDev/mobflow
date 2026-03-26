CREATE TABLE board
(
    id           UUID PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    color        VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    position     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_board_workspace_id ON board (workspace_id);

CREATE TABLE task_list
(
    id         UUID PRIMARY KEY,
    board_id   UUID         NOT NULL REFERENCES board (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_task_list_board_id ON task_list (board_id);

CREATE TABLE task
(
    id                 UUID PRIMARY KEY,
    list_id            UUID         NOT NULL REFERENCES task_list (id) ON DELETE CASCADE,
    workspace_id       UUID         NOT NULL,
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    priority           VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    assignee_auth_id   UUID,
    created_by_auth_id UUID         NOT NULL,
    due_date           DATE,
    position           INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);

CREATE INDEX idx_task_list_id      ON task (list_id);
CREATE INDEX idx_task_workspace_id ON task (workspace_id);
CREATE INDEX idx_task_assignee     ON task (assignee_auth_id);
CREATE INDEX idx_task_due_date     ON task (due_date);
