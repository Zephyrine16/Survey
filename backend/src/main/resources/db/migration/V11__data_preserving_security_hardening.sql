-- Forward-only reconciliation for the existing production schema.
-- No survey rows are deleted or backfilled; existing answer values are preserved.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM answers WHERE user_id IS NULL OR btrim(user_id) = '' OR question_id IS NULL) THEN
        RAISE EXCEPTION 'V11 stopped: answer rows are missing a participant or question; preserve and review those rows first';
    END IF;
    IF EXISTS (
        SELECT 1 FROM answers a JOIN options o ON o.id = a.option_id
        WHERE a.question_id IS DISTINCT FROM o.question_id
    ) THEN
        RAISE EXCEPTION 'V11 stopped: answer option/question references do not match; preserve and review those rows first';
    END IF;
    IF EXISTS (
        SELECT 1 FROM answers WHERE option_id IS NOT NULL
        GROUP BY user_id, menu_item_id, question_id, option_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V11 stopped: duplicate selected-option answers exist; preserve and review those rows first';
    END IF;
END $$;

-- V10 may have been skipped when the existing schema is explicitly baselined at 10.
-- IF NOT EXISTS makes this safe after V10 has already run on a fresh/migrated database.
CREATE TABLE IF NOT EXISTS security_rate_limits (
    bucket_key VARCHAR(64) PRIMARY KEY,
    attempts INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_security_rate_limits_expiry ON security_rate_limits(expires_at);

CREATE TABLE IF NOT EXISTS revoked_admin_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_revoked_admin_tokens_expiry ON revoked_admin_tokens(expires_at);

-- These metadata changes preserve all old values. NULL historical timestamps remain NULL;
-- the default applies only to future inserts that omit created_at.
ALTER TABLE answers ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE answers ALTER COLUMN response TYPE TEXT USING response::TEXT;
ALTER TABLE answers ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE answers ALTER COLUMN question_id SET NOT NULL;

ALTER TABLE options
    ADD CONSTRAINT uq_options_id_question_id UNIQUE (id, question_id);
ALTER TABLE answers
    ADD CONSTRAINT fk_answers_option_question
    FOREIGN KEY (option_id, question_id)
    REFERENCES options (id, question_id) NOT VALID;
ALTER TABLE answers VALIDATE CONSTRAINT fk_answers_option_question;
ALTER TABLE answers
    ADD CONSTRAINT ck_answers_has_response
    CHECK (option_id IS NOT NULL OR (response IS NOT NULL AND btrim(response) <> '')) NOT VALID;
ALTER TABLE answers VALIDATE CONSTRAINT ck_answers_has_response;
ALTER TABLE answers
    ADD CONSTRAINT ck_answers_user_id_nonblank
    CHECK (btrim(user_id) <> '') NOT VALID;
ALTER TABLE answers VALIDATE CONSTRAINT ck_answers_user_id_nonblank;

CREATE INDEX IF NOT EXISTS idx_answers_menu_item_id ON answers(menu_item_id);
CREATE INDEX IF NOT EXISTS idx_answers_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_answers_option_id ON answers(option_id);
CREATE INDEX IF NOT EXISTS idx_answers_user_id ON answers(user_id);
CREATE INDEX IF NOT EXISTS idx_answers_menu_question ON answers(menu_item_id, question_id);
CREATE INDEX IF NOT EXISTS idx_answers_menu_user ON answers(menu_item_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_answers_user_item_question_option
    ON answers(user_id, menu_item_id, question_id, option_id)
    WHERE option_id IS NOT NULL;

-- A SQL-created role receives no Neon administrator membership. The separate LOGIN
-- role stays passwordless until an operator sets its secret interactively with psql \password.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'survey_runtime_access') THEN
        CREATE ROLE survey_runtime_access NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOREPLICATION NOBYPASSRLS;
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'survey_runtime_access'
          AND (rolcanlogin OR rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    ) THEN
        RAISE EXCEPTION 'V11 stopped: survey_runtime_access already exists with unexpected privileges';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_auth_members
        WHERE member = (SELECT oid FROM pg_roles WHERE rolname = 'survey_runtime_access')
    ) THEN
        RAISE EXCEPTION 'V11 stopped: survey_runtime_access inherits another role; review memberships first';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'survey_runtime') THEN
        CREATE ROLE survey_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOREPLICATION NOBYPASSRLS;
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'survey_runtime'
          AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    ) THEN
        RAISE EXCEPTION 'V11 stopped: survey_runtime already exists with unexpected privileges';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_auth_members
        WHERE member = (SELECT oid FROM pg_roles WHERE rolname = 'survey_runtime')
          AND roleid <> (SELECT oid FROM pg_roles WHERE rolname = 'survey_runtime_access')
    ) THEN
        RAISE EXCEPTION 'V11 stopped: survey_runtime has unexpected role memberships; review them first';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_auth_members
        WHERE roleid = (SELECT oid FROM pg_roles WHERE rolname = 'survey_runtime_access')
          AND member <> (SELECT oid FROM pg_roles WHERE rolname = 'survey_runtime')
    ) THEN
        RAISE EXCEPTION 'V11 stopped: unexpected logins inherit survey_runtime_access';
    END IF;

    -- Enforce resource limits for every runtime connection, including code paths
    -- that do not use Spring's per-query timeout settings.
    EXECUTE format('ALTER ROLE survey_runtime IN DATABASE %I SET statement_timeout = %L', current_database(), '15s');
    EXECUTE format('ALTER ROLE survey_runtime IN DATABASE %I SET lock_timeout = %L', current_database(), '5s');
    EXECUTE format('ALTER ROLE survey_runtime IN DATABASE %I SET idle_in_transaction_session_timeout = %L', current_database(), '60s');
END $$;

GRANT survey_runtime_access TO survey_runtime;
GRANT USAGE ON SCHEMA public TO survey_runtime_access;
GRANT SELECT, INSERT ON answers TO survey_runtime_access;
GRANT SELECT, INSERT, UPDATE ON menu_items, questions, options TO survey_runtime_access;
GRANT SELECT, INSERT, UPDATE ON uploaded_images TO survey_runtime_access;
GRANT SELECT, INSERT, UPDATE, DELETE ON security_rate_limits TO survey_runtime_access;
GRANT SELECT, INSERT, DELETE ON revoked_admin_tokens TO survey_runtime_access;
DO $$
DECLARE
    sequence_name TEXT;
    sequence_table TEXT;
BEGIN
    FOREACH sequence_table IN ARRAY ARRAY['answers', 'menu_items', 'questions', 'options'] LOOP
        sequence_name := pg_get_serial_sequence(format('public.%I', sequence_table), 'id');
        IF sequence_name IS NOT NULL THEN
            EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %s TO survey_runtime_access', sequence_name);
        END IF;
    END LOOP;
END $$;
