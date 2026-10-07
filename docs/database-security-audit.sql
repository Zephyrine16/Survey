-- Read-only catalog and aggregate checks used in the 2026-10-07 Neon audit.
-- Run on the intended production branch/database. No participant contents returned.
BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';

SELECT current_database() AS database, current_user AS audit_login,
       version(), current_setting('transaction_read_only') AS read_only;

SELECT rolname, rolsuper, rolcreatedb, rolcreaterole, rolbypassrls, rolconnlimit
FROM pg_roles WHERE rolcanlogin ORDER BY rolname;

SELECT n.nspname, c.relname, pg_get_userbyid(c.relowner) AS owner,
       c.relrowsecurity, c.relacl
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r';

SELECT nspname, pg_get_userbyid(nspowner) AS owner, nspacl
FROM pg_namespace WHERE nspname = 'public';

SELECT to_regclass('public.flyway_schema_history') AS flyway_history,
       to_regclass('public.security_rate_limits') AS shared_throttles,
       to_regclass('public.revoked_admin_tokens') AS revoked_tokens;

SELECT c.conrelid::regclass AS table_name, c.conname, c.contype,
       c.convalidated, pg_get_constraintdef(c.oid) AS definition
FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
WHERE n.nspname = 'public';

SELECT tablename, indexname, indexdef FROM pg_indexes WHERE schemaname = 'public';

SELECT table_name, column_name, data_type, is_nullable,
       character_maximum_length, column_default, is_identity
FROM information_schema.columns
WHERE table_schema = 'public' ORDER BY table_name, ordinal_position;

SELECT c.relname AS table_name, a.privilege_type AS public_privilege
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
CROSS JOIN LATERAL aclexplode(COALESCE(c.relacl, acldefault('r', c.relowner))) a
WHERE n.nspname = 'public' AND c.relkind = 'r' AND a.grantee = 0;

SELECT n.nspname, p.proname, pg_get_userbyid(p.proowner) AS owner, p.proconfig, p.proacl
FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE p.prosecdef AND n.nspname NOT LIKE 'pg_%' AND n.nspname <> 'information_schema';

SELECT name, setting, reset_val, source FROM pg_settings
WHERE name IN ('statement_timeout','lock_timeout','idle_in_transaction_session_timeout',
               'search_path','password_encryption');

SELECT r.rolname, d.datname, v AS setting
FROM pg_db_role_setting s LEFT JOIN pg_roles r ON r.oid = s.setrole
LEFT JOIN pg_database d ON d.oid = s.setdatabase
CROSS JOIN LATERAL unnest(s.setconfig) v
WHERE v ~ '^(statement_timeout|lock_timeout|idle_in_transaction_session_timeout|search_path)=';

SELECT count(*) AS answers, count(DISTINCT user_id) AS participant_identifiers,
       count(*) FILTER (WHERE user_id IS NULL OR btrim(user_id) = '') AS missing_user,
       count(*) FILTER (WHERE question_id IS NULL) AS missing_question,
       count(*) FILTER (WHERE created_at IS NULL) AS missing_timestamp,
       count(*) FILTER (WHERE option_id IS NULL AND (response IS NULL OR btrim(response) = '')) AS empty_answer,
       count(*) FILTER (WHERE length(response) > 250) AS over_250_characters
FROM answers;

SELECT count(*) AS option_question_mismatches FROM answers a
JOIN options o ON o.id = a.option_id WHERE a.question_id IS DISTINCT FROM o.question_id;

SELECT count(*) FILTER (WHERE a.menu_item_id IS NOT NULL AND m.id IS NULL) AS missing_menu,
       count(*) FILTER (WHERE a.question_id IS NOT NULL AND q.id IS NULL) AS missing_question,
       count(*) FILTER (WHERE a.option_id IS NOT NULL AND o.id IS NULL) AS missing_option
FROM answers a LEFT JOIN menu_items m ON m.id = a.menu_item_id
LEFT JOIN questions q ON q.id = a.question_id LEFT JOIN options o ON o.id = a.option_id;

SELECT count(*) AS duplicate_groups, coalesce(sum(n - 1), 0) AS extra_rows
FROM (SELECT count(*) n FROM answers
      GROUP BY user_id, menu_item_id, question_id, option_id, response HAVING count(*) > 1) d;

SELECT count(*) AS duplicate_option_groups, coalesce(sum(n - 1), 0) AS extra_rows
FROM (SELECT count(*) n FROM answers WHERE option_id IS NOT NULL
      GROUP BY user_id, menu_item_id, question_id, option_id HAVING count(*) > 1) d;

SELECT count(*) AS matching_ratings,
       count(*) FILTER (WHERE n NOT BETWEEN 1 AND 5) AS outside_1_to_5,
       count(*) FILTER (WHERE n > 2147483647) AS integer_overflow
FROM (SELECT (regexp_match(btrim(response), '^(.*?):\s*([0-9]+)(?:\s*\((.*?)\))?$'))[2]::numeric n
      FROM answers WHERE length(response) <= 1000) r WHERE n IS NOT NULL;

SELECT count(*) AS images, sum(octet_length(content)) AS bytes,
       max(octet_length(content)) AS largest_bytes,
       count(*) FILTER (WHERE octet_length(content) > 5242880) AS over_5_mib,
       count(*) FILTER (WHERE content_type NOT IN ('image/jpeg','image/png','image/webp')) AS unsupported_type,
       count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1 FROM menu_items m WHERE m.image_name = uploaded_images.filename)) AS unreferenced
FROM uploaded_images;

EXPLAIN (FORMAT JSON) SELECT count(DISTINCT user_id) FROM answers WHERE menu_item_id = 1;
COMMIT;
