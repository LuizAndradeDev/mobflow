CREATE TABLE user_profile
(
    id           UUID PRIMARY KEY,

    auth_id      UUID         NOT NULL UNIQUE,

    display_name VARCHAR(100) NOT NULL,
    bio          VARCHAR(500),
    avatar_url   TEXT,
    phone        VARCHAR(20),

    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_user_profile_auth_id ON user_profile (auth_id);