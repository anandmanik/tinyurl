## TinyURL Service – Detailed Specification

### 1) Overview
- Build a URL shortener with anonymous usage and user-scoped “My URLs” lists identified by a user-provided `userId`.
- Global deduplication of long URLs; idempotent create-or-return API.
- System-generated, random 7-character short codes in lowercase Base36.

### 2) Architecture
- **Backend**: Java Spring Boot microservice (Maven).
- **Frontend**: React + Tailwind.
- **Database**: MySQL.
- **Cache**: Redis.
- **Domains**:
  - Short/SPA/API host: `https://amtinyurl.com`
  - Redirect route: `GET https://amtinyurl.com/{code}`
  - API reserved under `https://amtinyurl.com/api/...`

### 3) Authentication and Identity
- **Anonymous model**: user inputs `userId`, receives a permanent stateless JWT.
- **JWT**:
  - Transport: `Authorization: Bearer <token>`
  - Signing: HS256 using `JWT_SECRET`
  - No expiry (permanent)
  - Claims: `sub=<userIdLower>`, `iat`, `iss=amtinyurl`
- **UserId rules**:
  - Length: 6 characters
  - Charset: alphanumeric only
  - Case-insensitive; normalized to lowercase in backend
  - Multiple clients may reuse the same `userId` (shared lists)

### 4) URL Normalization and Validation
- **Accepted schemes**: `https` only
- **Auto-prefix**: if scheme missing, prefix `https://`
- **Normalization for dedup**:
  - Lowercase scheme and host only
  - Preserve path and query exactly (no trailing slash changes, no param reordering, no stripping)
- **Max length**: 2,048 characters
- **Blocklist**: reject long URLs pointing to `amtinyurl.com` (prevent loops)
- **No SSRF protections**: do not block localhost or private networks
- **Validation**: syntactic only (no DNS/reachability checks)

### 5) Short Code Generation
- **Format**: lowercase Base36 `[a-z0-9]{7}`, case-insensitive handling on lookup
- **Method**: random generation with collision checks
- **Collision retries**: 3 attempts; on exhaustion:
  - Status: `503`
  - Body: `{ "error": "collision_retries_exhausted", "code": "COLLISION_RETRY_EXHAUSTED" }`
- **Reserved codes**: none beyond `/api` routing reservation (pattern prevents collision)

### 6) Caching
- **Redis entries**:
  - code → normalized_url
  - normalized_url → code
- **TTL**: 5 minutes
- **Capacity**: ~100 entries for each direction
- **Eviction**: LRU
- **Logging**: log cache hit/miss for create/get/redirect

### 7) Redirect Behavior
- **Route match**: any path matching `^/[a-z0-9]{7}$` is treated as a short code
- **Redirect**: `301` Permanent
- **Headers**: `Cache-Control: max-age=100, public`
- **Not found**: return API-style JSON error with 404

### 8) API
- Base path: `https://amtinyurl.com/api`

- `POST /api/token`
  - Body: `{ "userId": "AbC123" }`
  - Response: `{ "token": "<jwt>", "userId": "abc123" }`
  - Notes: lowercases `userId`, issues permanent JWT

- `POST /api/urls` (idempotent, requires Bearer)
  - Body: `{ "url": "https://Example.com/Path?a=1&b=2" }`
  - Behavior:
    - Normalize URL (lowercase scheme/host). Deduplicate globally by normalized URL.
    - If mapping exists: associate with user if not already associated, return existed=true
    - Else: generate code with collision checks, insert, associate with user
  - 201 if newly created, 200 if already existed
  - Response: `{ "code", "shortUrl", "url", "createdAt", "existed" }`
    - `url` is the normalized URL
    - `shortUrl` = `BASE_URL` + `"/" + code`

- `GET /api/urls` (requires Bearer)
  - Returns user’s URLs, newest-first
  - No pagination
  - Response: array of `{ "code", "shortUrl", "url", "createdAt" }`

- `DELETE /api/urls/{code}` (requires Bearer)
  - Deletes only the association for the authenticated user
  - 204 on success, 404 if no association

- `GET /api/healthz`
  - Deep checks: app up, MySQL connectivity, Redis connectivity
  - 200 with check details; 503 if any dependency fails

- Error format (all API errors):
  - JSON: `{ "error": string, "code": string }`
  - Common statuses:
    - 400: invalid URL, invalid userId format
    - 401: missing/invalid token
    - 404: short code not found (including redirect path)
    - 503: collision retries exhausted, or health failures

### 9) Data Model (MySQL)
- `urls`
  - `code` CHAR(7) PRIMARY KEY
  - `normalized_url` VARCHAR(2048) UNIQUE, case-sensitive (binary collation)
  - `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `user_urls`
  - `user_id_lower` CHAR(6)
  - `code` CHAR(7)
  - `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  - PRIMARY KEY (`user_id_lower`, `code`)
  - FOREIGN KEY (`code`) REFERENCES `urls`(`code`)
  - Index on `user_id_lower`
- Migrations: Flyway SQL in `db/migration` (e.g., `V1__init.sql`)

### 10) Logging
- **Structured JSON to stdout**
- Fields: `timestamp`, `level`, `requestId`, `userId`, `userAgent`, `route`, `action`, `code`, `url`, `cache` (hit|miss), `status`
- **Request ID**: accept `X-Request-Id` header; if absent, generate UUIDv4
- Actions examples: `create`, `get`, `redirect`, `delete`, `health`

### 11) Frontend (React + Tailwind)
- **Token screen**:
  - Input `userId` (6 alphanumeric, case-insensitive). Submit to `POST /api/token`.
  - Store JWT in `sessionStorage`.
- **Create form**:
  - Input URL; auto-prefix `https://` if missing; send `POST /api/urls`.
  - Show result: `shortUrl`, `createdAt`, `existed` flag.
- **My URLs list** (no pagination; newest-first):
  - Columns: `shortUrl` (copy), `URL`, `createdAt`, actions: Copy, Open in new tab, Delete
- **Routing**:
  - SPA served for non-`/api` paths except `^/[a-z0-9]{7}$` (handled by backend redirect route)

### 12) CORS
- Allow all origins (wildcard)

### 13) Environment and Config
- `API_PORT=8082`
- `FRONTEND_PORT=3000`
- `MYSQL_URL=localhost:3306`
- `MYSQL_USER=root`
- `MYSQL_PASSWORD=admin`
- `REDIS_URL=redis://127.0.0.1:6379`
- `JWT_SECRET=1fe2275ec12ed522e57b743c64facf12`
- `BASE_URL`:
  - Production: `https://amtinyurl.com`
  - Local dev: `https://localhost`

### 14) Deployment

#### Docker
- **Production Dockerfiles**: Multi-stage builds for API and frontend
- **docker-compose.yml**: Local development with API (Spring Boot), MySQL, Redis, React dev server
- **Global network**: `tinyurl-global-network` for container communication
- **Health checks**: MySQL, Redis, backend API, and frontend
- **Persistent volumes**: MySQL data and Redis persistence

#### CI/CD Pipeline (Jenkins)
- **Multibranch pipeline** with GitHub integration using PAT authentication
- **Docker-in-Docker builds** using Maven 3.9 and Node 18 containers
- **Parallel builds**: Backend and frontend built simultaneously
- **Dependency caching**: Persistent Maven (.m2) and NPM cache volumes
- **Integration testing**: Database connectivity and health checks
- **Service management**: Intelligent container reuse with health verification
- **Image cleanup**: Automatic removal of old builds (keeps latest 3)
- **Branch controls**: Dev branch builds can be paused/resumed
- **Post-deployment**: Services remain running for manual verification

#### Configuration Management
- **jenkins.properties**: Centralized CI/CD configuration
- **env.properties**: Local development defaults with environment variable overrides
- **application.properties**: Spring Boot with `${VAR:default}` pattern for flexibility

#### Headers/Security
- Set HSTS on production domain
- Return `Cache-Control: max-age=100, public` on 301 redirect

### 15) Implementation Notes
- Use a unique index on `normalized_url` and on `code`.
- Create flow (transactional):
  - Normalize URL
  - Check Redis `url→code`; on miss, check DB; on found, associate user (upsert) and return
  - If not found: generate random code; attempt DB insert into `urls`
    - On duplicate key (code): retry up to 3 times
    - After insert: insert into `user_urls`
  - Populate Redis both directions with TTL 5 minutes
- Redirect flow:
  - Normalize code to lowercase; validate `[a-z0-9]{7}`
  - Check Redis `code→url`; on miss, fetch from DB; if found, set cache and 301 redirect with `Location` and `Cache-Control`
- No rate limits or analytics.


