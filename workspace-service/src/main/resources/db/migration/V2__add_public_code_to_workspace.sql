ALTER TABLE workspace ADD COLUMN public_code VARCHAR(8) UNIQUE;

UPDATE workspace SET public_code = LEFT(MD5(RANDOM()::TEXT), 8) WHERE public_code IS NULL;

ALTER TABLE workspace ALTER COLUMN public_code SET NOT NULL;

CREATE INDEX idx_workspace_public_code ON workspace (public_code);
