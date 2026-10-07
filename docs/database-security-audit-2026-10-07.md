# Production database security audit — 7 October 2026

Production still has security and integrity gaps. The highest priorities are deploying the shared security-state migration safely, restricting the application's database privileges, and rejecting ratings that can crash analytics.

## Remediation status

Forward-only code and migration changes are prepared locally for the five findings below. No production database schema or answer rows have been changed. V11 preserves current answer values and historical NULL timestamps, but it still needs to pass on a restored Neon branch before the controlled production migration and Render credential switch.

## Scope and evidence

Inspected the signed-in Neon console: project `survey-db` (`summer-sound-33594945`), branch `production` (`br-hidden-fog-aomg025z`), database `neondb`, primary endpoint `ep-silent-mode-aojy4v2t`. The server reported PostgreSQL 18.6. SQL inspections ran around 21:28–21:35 Asia/Manila in explicit read-only transactions, with a temporary 15-second statement timeout. Only catalogs and aggregate counts were retrieved. No participant identifiers, response contents, passwords, or image contents were exported. No production data, schema, roles, or settings were changed.

Application findings below were checked against the current workspace. The exact deployed backend build and its connection settings were not independently verified. The SQL editor connected as `neondb_owner`; that alone does not establish which credentials Render uses. No attack payloads or load tests were sent to production.

| Observed data | Count |
| --- | ---: |
| Answer rows | 6,425 |
| Distinct non-null participant identifiers | 61 |
| Menu items | 73 |
| Questions / options | 7 / 17 |
| Uploaded images | 145 |

Participant identifiers are anonymous session identifiers, not proof of 61 distinct people.

## Findings

### 1. High: production lacks the migration and tables required by the security fixes

`to_regclass` returned NULL for `public.flyway_schema_history`, `public.security_rate_limits`, and `public.revoked_admin_tokens`. Production therefore does not have the database support for the new shared throttling and logout revocation implementation. Deploying the current backend against this schema without preparation would also encounter Flyway's existing-schema adoption requirement.

There is material schema drift: live `answers.question_id` is nullable, `answers.response` is VARCHAR(255), `created_at` has no default, the live foreign keys use default deletion behavior, and only primary-key indexes exist. Repository V1 specifies a non-null question, TEXT response, a timestamp default, different foreign-key deletion behavior, and secondary indexes. The presence of description/image columns is not proof that all migrations ran.

**Action:** preserve a recovery point/export and verify a restore; compare the complete live schema with V1–V9; then reconcile differences with a reviewed, forward-only migration on a copy of current production data. Keep all existing answers and keep submissions running through compatible additive changes. Do not delete, truncate, drop, or rewrite existing answers; do not invent missing historical timestamps; do not blindly baseline at version 9 or replay the initial schema against existing data. Compare answers by primary key and stored fields after rollout before retiring the recovery point. V10 itself only creates security tables and indexes. See `docs/security-deployment.md`.

### 2. High if used by the backend: no restricted application login exists

The only login roles returned were `neondb_owner` and provider-managed `cloud_admin` / `neon_service`. `neondb_owner` owns every application table and has CREATEDB, CREATEROLE, and BYPASSRLS. A compromised owner credential can read, change, or destroy survey data and manage database roles. The active-connection snapshot contained only the SQL editor, so the backend's actual login remains unverified.

**Action:** use separate migration-owner and application roles; grant the application only required table/sequence privileges and no schema creation, ownership, role management, or RLS bypass. Inventory admin editing and seeding operations before narrowing grants. Remove runtime DDL first: `UploadedImageRepository.java:18–28` still creates a table during startup. Creating another role through the Neon console would grant administrator membership by default; limited roles should be created through SQL with explicit grants. [Neon role documentation](https://github.com/neondatabase/website/blob/main/content/docs/manage/roles.md).

Provider-internal roles are expected and should not be altered.

### 3. High, locally reproduced: a stored oversized rating can break analytics

The real `SurveyService.mapAndSanitizeRows` accepts `Comfort: 999999999999999999` for an existing item/question with no selected option. Its text length is below the allowed 250 characters. The real `AnalyticsService.buildMoodAnalytics` then throws `NumberFormatException` on that response. The same unchecked integer parsing occurs in item statistics, recent responses, and statistical rating loading.

Evidence: an isolated Java probe invoked the compiled application classes with synthetic repository proxies. It performed no database connections or writes and printed:

```text
Actual SurveyService accepts oversized numeric rating.
Actual AnalyticsService throws NumberFormatException on that stored response.
No database connections or writes performed.
```

References: `SurveyService.java:148`, `AnalyticsService.java:113,231,337`, `StatisticalAnalysisService.java:836`. Existing production ratings contained **zero integer overflows** and **zero values outside 1–5** among 6,303 matching rows. This is an exploitable application path, not evidence that someone has already used it.

**Action:** validate question type, selected dimension, and a bounded integer 1–5 before storing a rating; derive dimension labels from server-side options. Make analytics tolerate invalid legacy rows without failing the whole request. A typed rating column plus a CHECK constraint would enforce this at the database boundary more reliably than parsing arbitrary text.

### 4. Medium: answer integrity relies on application checks

Live answers have no business-key uniqueness or CHECK constraints. `user_id`, `question_id`, and `created_at` are nullable. Separate question/option foreign keys ensure both references exist but do not ensure the option belongs to the submitted question. Application checks reduce current exposure, but imports, alternate write paths, or regressions can bypass them.

Analytics also identifies dimensions from client-provided response labels. The service validates option ownership but does not bind those labels to the option; a client can mislabel ratings or repeat one reported dimension under distinct valid option IDs.

**Action:** define the valid answer shape for each question type, enforce mandatory participant/question fields, and add a composite option/question relationship and appropriate uniqueness. Preserve legitimate matrix dimensions and demographic rows; nullable menu/option references require deliberate constraint design. Validate demographic values against supported choices as well.

Current checks found **zero missing participant/question references, empty answers, orphan references, option/question mismatches, exact duplicates, or repeated non-null option keys**. Multiple matrix answers with null option IDs must not be mistaken for duplicates.

### 5. Medium: missing indexes and weak resource limits increase availability risk

The live database has only six primary-key indexes. All answer foreign-key/participant and V3 analytics indexes are missing. A plain EXPLAIN of the per-item distinct-participant query showed a sequential scan, sort, and aggregate. EXPLAIN ANALYZE was not used. Current data volume is small; this is a scaling and abuse risk, not evidence of present database overload.

The SQL-editor session's original statement and lock timeouts were 0, with no relevant persisted role/database overrides. Its idle-transaction timeout was five minutes. The 15-second timeout used in this audit was temporary and did not harden production. Backend session-level timeouts were not observed. Public GET paths are not covered by the repository's submission-only rate limiter; direct image GETs load complete database blobs, up to about 3 MB in the current data, on each origin request.

**Action:** reconcile/install the intended indexes, add suitable application-role query/lock timeouts, and protect expensive public reads through caching and request limits. Do not assume a cache response header stops direct origin traffic.

### 6. Medium integrity issue: every answer lacks its submission timestamp

All **6,425** `created_at` values are NULL. The database column is nullable and has no default. The JDBC batch insert omits that column, so the entity's `@CreationTimestamp` annotation never runs for this path. This prevents reliable submission chronology and time-based investigation or retention.

**Action:** supply a database/server timestamp for future inserts through a migration and enforce the intended nullability after dealing explicitly with legacy rows. Do not fabricate historical submission times; recover them only from reliable existing evidence.

### 7. Medium hardening gap: no IP restrictions

The production overview reported **IP restrictions: None set**. Anyone who obtains a valid credential can attempt to connect from outside the application network. This does not mean anonymous users can read the database.

**Action:** restrict connections to verified backend egress and necessary administrative access if the account plan supports it, or evaluate private connectivity. Neon documents IP allowlists as a Scale-plan feature; this project is on the Free plan. [Neon security documentation](https://github.com/neondatabase/website/blob/main/content/docs/security/security-overview.md).

### 8. Operational gaps: short recovery history and unused image storage

The overview showed a **six-hour history retention window**. Independent backups were not verified. Corruption discovered after that window may require another backup source. Uploaded images occupy **57,644,793 bytes**; **72 of 145** are not referenced by `menu_items.image_name`. No image exceeded 5 MiB or had an unsupported MIME type in the checked metadata. This is an accumulation/storage issue, not proof those files are malicious or safe to delete.

**Action:** establish and test recovery beyond six hours, and implement image retention/cleanup with a grace period and a complete reference inventory.

## Checks that passed and interpretation limits

- No PUBLIC table grants; PUBLIC has schema USAGE but not CREATE. Database CONNECT/TEMPORARY grants do not grant anonymous data access.
- No application-schema SECURITY DEFINER functions were found.
- The Data API displayed its initial Enable Data API screen and is not enabled. RLS is disabled on the application tables, but that alone is not a public-exposure finding for a backend-only database.
- Existing foreign keys are validated. The live answer/question relationship does **not** use ON DELETE CASCADE; repository migration semantics differ.
- The password-encryption setting is SCRAM-SHA-256. Password contents and stored hashes were not inspected.
- `pg_stat_ssl` showed false inside the SQL editor. This does not establish an unencrypted Internet connection: Neon enforces TLS at its connection proxy. Certificate validation on the actual backend JDBC connection still needs separate verification. [Neon security documentation](https://github.com/neondatabase/website/blob/main/content/docs/security/security-overview.md).
- This audit does not establish absence of compromise, audit the entire deployed application, validate every uploaded image's bytes, or constitute an exhaustive engine/extension CVE scan.

## Suggested order of work

1. Fix and test rating validation/parser resilience; remove the remaining runtime image-table DDL.
2. Preserve recovery evidence and prepare a schema-reconciliation migration, including timestamps and intended indexes; establish Flyway history safely and apply V10.
3. Deploy with separate restricted application credentials and production security settings; verify shared throttling and logout end to end.
4. Add carefully designed data constraints, read-side resource limits, network restrictions where available, and tested backup/image-retention policies.

Reproducible read-only checks are in `docs/database-security-audit.sql`. The synthetic application probe used for this audit is saved locally as `.security-test/RatingOverflowProbe.java`.
