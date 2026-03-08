CREATE TABLE user_credential
(
    id                    UUID PRIMARY KEY,

    username_credential   VARCHAR(50)  NOT NULL UNIQUE,
    email_credential      VARCHAR(150) NOT NULL UNIQUE,
    password_credential   TEXT         NOT NULL,

    authority_credential  VARCHAR(50)  NOT NULL,

    account_enable        BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT                   DEFAULT 0,
    account_non_locked    BOOLEAN               DEFAULT TRUE,

    lock_time             TIMESTAMP,

    created_time          TIMESTAMP    NOT NULL,
    updated_time          TIMESTAMP    NOT NULL,
    last_login            TIMESTAMP
);

CREATE INDEX idx_user_username ON user_credential (username_credential);
CREATE INDEX idx_user_email ON user_credential (email_credential);