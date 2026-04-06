ALTER TABLE task
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE task
    ADD COLUMN completed_by_auth_id UUID;

ALTER TABLE task
    ADD COLUMN completed_at TIMESTAMP;

CREATE INDEX idx_task_completed_by_auth_id
    ON task (completed_by_auth_id);

CREATE INDEX idx_task_workspace_status
    ON task (workspace_id, status);

CREATE INDEX idx_task_assignee_due_date
    ON task (assignee_auth_id, due_date);
