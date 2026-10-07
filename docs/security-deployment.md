# Security deployment

The security fixes are in the repository. Apply the settings below to the actual Render service and redeploy both backend and frontend. Local configuration does not change Render, Vercel, or Neon.

## Backend production settings

Use `backend/.env.production.example` as a settings reference; retain the real database credentials, admin BCrypt hash, and randomly generated JWT secret in Render's secret environment variables. Do not commit credentials.

| Setting | Production value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_SSL_MODE` | `verify-full` |
| `DB_SSL_ROOT_CERT` | Container path to the PEM CA certificate trusted for the database server |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` |
| `DB_USERNAME` | `survey_runtime` (restricted runtime role) |
| `FLYWAY_USER` | Separate schema-owner role used only for migrations |
| `SEEDERS_ENABLED` | `false` |
| `SURVEY_COOKIE_SECURE` | `true` |
| `JWT_EXPIRATION_MS` | `900000` or a shorter positive duration |
| `CORS_ALLOWED_ORIGINS` | Exact frontend origins, comma separated; e.g. `https://survey-xi-ten.vercel.app` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `none` |
| `SECURITY_TRUSTED_PROXIES_ENABLED` | `true` |
| `SECURITY_TRUSTED_PROXY_ADDRESSES` | Verified immediate upstream proxy addresses/CIDRs, including other trusted hops in the forwarding chain |
| `SECURITY_MAINTENANCE_ENABLED` | `false` |

The production profile rejects weak SSL modes (including overrides in the JDBC URL), automatic Hibernate updates, non-Secure participant cookies, and unconditionally rewritten peer addresses. Supply the database CA through a trusted certificate source and mount or bundle that public certificate at `DB_SSL_ROOT_CERT`. `verify-full` checks both the certificate chain and hostname. `require` encrypts traffic without authenticating the server and is rejected in production.

### Verify proxy trust

Determine the peer addresses used by your Render deployment from the platform/network configuration; do not guess or trust every IPv4/IPv6 address. The resolver accepts `X-Forwarded-For` only from the configured immediate peer, scans right to left, and stops at the first untrusted hop. It ignores `CF-Connecting-IP` and `X-Real-IP`.

The edge must replace incoming forwarding headers or append the real peer address after any supplied values. Restrict direct access when the proxy architecture requires it. Keep Spring forwarded-header processing disabled so the resolver can check the original connection peer.

Verify that two different client networks resolve to different client IPs. Five failed logins from one network must not lock out another. Verify that adding forged `CF-Connecting-IP`, `X-Real-IP`, and an `X-Forwarded-For` prefix does not change the resolved identity. The local Compose stack pins Nginx to its explicit trusted address and overwrites the forwarding header. Its published backend/database ports bind only to localhost.

## Database migration rollout

### Preserve existing and incoming survey data

Preserving every existing answer and continuing to accept new submissions is a release requirement. Do not run `DELETE`, `TRUNCATE`, table drops, destructive resets, or column changes that discard or truncate existing answers as part of security remediation. Do not invent historical timestamps or rewrite participant identifiers, option selections, or response values to make a constraint pass. Hibernate schema updates remain disabled; Flyway migrations must be explicit and reviewed.

Before any production schema change, take a recovery point and verify it by restoring it to a disposable staging branch or database. Run the proposed migration against that restored copy while exercising survey submissions, then verify existing answers remain intact and newly submitted answers persist. Record per-table row counts and key ranges before and after; also compare the restored answer rows by primary key and their stored fields so equal counts cannot hide overwritten responses. Check that menu items, questions, options, and uploaded images remain available. Keep the submission service running through backward-compatible additive changes; if safe compatibility cannot be demonstrated, postpone the migration while submissions continue.

For legacy rows with NULL `created_at`, add a timestamp default for future inserts while preserving the legacy NULLs until a truthful remediation is available. Do not require a fabricated timestamp for existing rows. The V11 preflight checks that every existing answer has a participant and question before making those two columns non-null; the response and menu-item/option references stay nullable for legitimate answers.

The Spring Boot 4 Flyway starter now runs migrations before Hibernate validation. Flyway owns schema changes; seeders only insert data and no longer create tables.

On a database with valid Flyway history, V10 creates shared throttle counters and revoked-token hashes. These tables survive restarts and are shared by all replicas; expired security-state rows are removed each minute. V10 does not modify or delete survey answers.

A deployment previously relying on Hibernate may have a nonempty schema without `flyway_schema_history`. The V11 migration is a reviewed, forward-only reconciliation for the audited production layout: it restores the V10 security tables if the schema is baselined at version 10, adds the missing answer indexes and integrity constraints, creates the restricted runtime role, and preserves existing answer IDs and values. It widens `response` from `VARCHAR(255)` to `TEXT` without changing stored response values. It adds a timestamp default for future rows while leaving historical NULL timestamps untouched. It does not delete, truncate, or backfill survey data.

Before adopting a database without Flyway history, restore a current Neon recovery point to a disposable branch and run V11 there first. Confirm the preflight passes, compare every answer's primary key and stored values, verify existing menu/questions/options/images, then submit a new survey and confirm those new answers receive timestamps and remain readable. V11 aborts transactionally if it finds missing participant/question values, mismatched option/question references, duplicate selected-option keys, or blank optionless answers. Its lock and statement timeouts also make a busy/slow migration fail without partially applying DDL. Keep production submissions running until the restored-copy checks pass.

For the audited production schema with no Flyway history, first create the restricted login through psql (not Neon role management, which can add provider-admin membership) while connected as the migration owner:

```sql
CREATE ROLE survey_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
    NOREPLICATION NOBYPASSRLS;
```

Then run psql's interactive `\password survey_runtime` and store that secret directly in Render's `DB_PASSWORD`. Set `DB_USERNAME=survey_runtime`, `FLYWAY_USER`, and `FLYWAY_PASSWORD` separately before the app's first startup so Flyway can migrate with the owner and Hibernate can connect with the restricted role after migration. After the restored-copy checks and a verified recovery point, set `FLYWAY_BASELINE_ON_MIGRATE=true` and `FLYWAY_BASELINE_VERSION=10` for that controlled startup so Flyway records the known pre-V11 baseline and applies V11. Do not baseline any other schema/version by guesswork. Remove `FLYWAY_BASELINE_ON_MIGRATE` immediately after success. V11 grants the application role no DELETE privilege on answers or survey definitions; in production the individual delete endpoints are also disabled. Do not put passwords in this repository or chat. The Flyway credential remains the schema owner; the application credential has no role-management or table-deletion rights. V11 also sets 15-second statement, 5-second lock, and 60-second idle-transaction timeouts for the runtime role.

After production rollout, compare the saved answer inventory against the live table by answer ID and stored field values, and confirm new submissions are present. A row-count match alone is insufficient. Keep the verified recovery point until those checks pass and restore remains possible.

After migration, confirm startup reports version 11 and successful Hibernate validation. Verify the live runtime session is `survey_runtime`, has no elevated role attributes or unexpected memberships, and can insert a survey response while failing a DELETE against answers and survey definitions. Compare the saved answer inventory against the live table by answer ID and stored field values, and confirm a new submission has a non-null timestamp. Deploy the frontend and backend together. Existing long-lived/legacy tokens are rejected; administrators must log in again.

## Admin session behavior

Bearer tokens are held only in browser memory, expire within 15 minutes, and are never written to browser storage. Refreshing or reopening the dashboard requires login. Logout calls `/api/admin/logout` before clearing local authorization and persists the token hash until expiration, so replay is denied across replicas. A failed revocation request leaves the session available for retry.

CSRF remains disabled because authentication uses an explicit Authorization header and the backend does not authenticate admin requests from cookies. If cookie authentication is introduced later, add CSRF protection as part of that change.

Bulk response/menu deletion routes are absent under `prod`. The individual menu-item, question, and option delete endpoints also return 403 while maintenance is disabled; production startup rejects `SECURITY_MAINTENANCE_ENABLED=true`. In other profiles, deletes require `SECURITY_MAINTENANCE_ENABLED=true`. Frontend bulk controls additionally require a development build and `VITE_ENABLE_MAINTENANCE=true`. Authenticated create and update operations remain available.

## Survey integrity

Submissions reject duplicate item/question/option answers, empty answers, invalid references, and more than `SURVEY_ITEMS_PER_PARTICIPANT` menu items. Different dimensions of one matrix question remain valid. Keep this setting aligned with frontend `VITE_SURVEY_ITEM_LIMIT` (default 10).

A transaction-scoped PostgreSQL advisory lock serializes respondent cap checks and inserts across replicas. Concurrent retries of one session insert once; concurrent participants cannot exceed the global or per-item limits. The public availability list is no longer the only enforcement of item capacity.

The survey is anonymous: client session UUIDs identify survey sessions, not verified people. Throttling and validation reduce abuse but do not prove one response per human. That policy would require invitation tokens or participant verification.

## Verification

Run backend unit tests with `mvnw.cmd test` on Windows (or `./mvnw test` elsewhere). Run frontend `npm run type-check` and `npm run test:unit -- --run`.

The PostgreSQL regression suite also verifies application startup, all Flyway migrations, concurrent shared throttling, revocation across store instances, idempotent retries, and both respondent caps. To enable it, create a **disposable** local PostgreSQL database named `survey_security_regression` owned by `survey_security_test` with local test access, then set:

```powershell
$env:SURVEY_SECURITY_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55432/survey_security_regression'
.\mvnw.cmd test
```

The suite clears records in that disposable database between tests. It refuses URLs for other hosts/database names. Without the variable, those integration tests are skipped.
