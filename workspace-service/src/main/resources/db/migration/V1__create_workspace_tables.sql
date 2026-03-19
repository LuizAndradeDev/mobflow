CREATE TABLE workspace
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    owner_auth_id UUID         NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE TABLE workspace_member
(
    id           UUID PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspace (id),
    auth_id      UUID        NOT NULL,
    role         VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMP   NOT NULL,
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, auth_id)
);

CREATE INDEX idx_workspace_owner ON workspace (owner_auth_id);
CREATE INDEX idx_workspace_member_workspace ON workspace_member (workspace_id);
CREATE INDEX idx_workspace_member_auth ON workspace_member (auth_id);
