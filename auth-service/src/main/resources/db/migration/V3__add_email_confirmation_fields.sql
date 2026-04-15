ALTER TABLE user_credential
    ADD COLUMN IF NOT EXISTS confirmation_token VARCHAR(200),
    ADD COLUMN IF NOT EXISTS confirmation_token_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS email_confirmed_at TIMESTAMP;
