CREATE TABLE workspace_invite
(
    id                 UUID PRIMARY KEY,
    workspace_id       UUID        NOT NULL REFERENCES workspace (id),
    target_auth_id     UUID        NOT NULL,
    invited_by_auth_id UUID        NOT NULL,
    status             VARCHAR(20) NOT NULL,
    created_at         TIMESTAMP   NOT NULL,
    responded_at       TIMESTAMP
);

CREATE INDEX idx_workspace_invite_workspace ON workspace_invite (workspace_id);
CREATE INDEX idx_workspace_invite_target ON workspace_invite (target_auth_id);
