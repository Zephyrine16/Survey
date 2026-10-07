# Security hardening

Admin authentication remains username/password with signed bearer tokens. MFA is not required.

## Controls

- Protected APIs require a signed, unexpired token for the configured admin. Tokens must include subject, issue time, and expiry. Saved dashboard sessions are verified by the server before loading protected data.
- Login attempts reserve their rate-limit slot atomically before BCrypt runs. Unknown usernames use the same BCrypt hash and work factor. Credentials are bounded, including BCrypt's 72-byte password limit. Token responses use `Cache-Control: no-store`.
- Survey cooldowns reserve their slot atomically and apply only to POST, so preflights do not consume submissions. Public answers must reference existing menu items and questions; an option must belong to its question. Invalid references receive HTTP 400 rather than being silently replaced.
- JSON bodies are limited before deserialization, including chunked requests: 16 KiB for login, 1 MiB elsewhere. Submissions contain 1–1000 non-null answers. Text and demographic fields have length limits; whitespace does not bypass them. Multipart uploads retain their separate 5 MiB limit and image signature checks.
- CORS allows exact configured origins, with no wildcard hosts, paths, credentials in URLs, queries, or fragments. Error responses omit internal exception messages.
- Vercel and nginx serve a CSP blocking inline scripts, embedded objects, framing, and base URL changes, alongside nosniff, referrer, and permissions headers. Vue's inline styles and image previews remain supported.
- The backend container runs as UID/GID 10001. Frontend container builds use the dependency lockfile through `npm ci`.

## Deployment

Deploy the backend and frontend together. The new dashboard session endpoint needs the patched backend. Vercel/nginx headers take effect only after deployment through those configurations.

`SERVER_FORWARD_HEADERS_STRATEGY` now defaults to `none`. Behind a trusted reverse proxy, set it to `framework` only if the proxy strips/replaces client-supplied forwarding headers and direct access to the backend is prevented. Without trusted forwarding, users behind the same proxy can share an IP-based cooldown. Set `SECURITY_TRUSTED_PROXIES_ENABLED=true` only when its supported IP headers are also overwritten by that proxy.

Use exact HTTPS frontend origins in production. The CSP permits HTTPS API connections and images; nginx additionally permits the documented local API at `http://localhost:8080`. Other HTTP API configurations need an explicit development policy.

## Verification and scope

Regression tests exercise parallel login/submission attempts, forged/expired/incomplete tokens, oversized JSON without a content-length header, cross-question options, invalid item/question references, whitespace length checks, CORS rejection, and valid authenticated menu access. Frontend tests, type checking, and a production build verify the dependency changes.

The npm production audits cover the root and frontend installations. Development-only ESLint dependencies still contain the `braces` advisory GHSA-vfj7-8cjw-p6xm; npm's proposed remedy downgrades the ESLint configuration and is not applied. These dependencies are not served in the browser bundle. Spring Boot is updated within the 4.0 maintenance line; this is not a full transitive Java dependency audit.

Verification passed: 54 backend tests, 45 frontend tests, frontend type checking, and the production build. Both npm production audits report zero vulnerabilities. Docker/nginx runtime verification was unavailable because the Docker daemon is not running; container and deployment header changes were reviewed in source only.

Rate-limit state is local to each backend process and resets on restart. Multiple replicas require shared rate-limit storage or limits at the proxy. Bearer tokens remain in browser local storage and logout clears the browser session without revoking a copied token. Infrastructure permissions, production secrets, proxy behavior, and the live deployment have not been penetration-tested by this source review.

References: [OWASP authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [Vue advisory](https://github.com/advisories/GHSA-g2v6-rqmx-r4w6), [remaining development dependency advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm).
