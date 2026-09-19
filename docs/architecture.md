# Architecture

## Components

```mermaid
flowchart LR
    subgraph Client["Clients"]
        Browser["Browser (React SPA)"]
        Mobile["Flutter app"]
    end

    subgraph Edge["Edge (dev: Vite proxy · prod: nginx)"]
        Proxy["/api reverse proxy\n(same-origin, cookies stay first-party)"]
        Static["Static SPA files"]
    end

    subgraph API["Spring Boot API"]
        Auth["auth module"]
        Task["task module"]
        Shared["shared module (OPEN)"]
    end

    DB[("MySQL 8.4")]

    Browser -->|"HTML/JS/CSS"| Static
    Browser -->|"/api/* (cookies + X-XSRF-TOKEN)"| Proxy
    Proxy --> API
    Mobile -->|"/api/* (Authorization: Bearer)"| API
    Auth --> Shared
    Task --> Shared
    Auth --> DB
    Task --> DB
```

The web client never calls the API cross-origin: in development Vite proxies `/api` to
`http://localhost:8080`, in production nginx serves the built SPA and reverse-proxies `/api` to the backend
Cloud Run service. This keeps the `SameSite=Strict` auth cookies and the CSRF cookie first-party. The
mobile app has no same-origin constraint and talks to the API directly with a Bearer token. See
`docs/deployment.md` for the production topology.

## Modules (Spring Modulith)

The backend is one Spring Boot application split into three `@ApplicationModule`s under
`io.github.navysama.taskmanager`:

| Module | Type | Contains | Allowed dependencies |
|---|---|---|---|
| `shared` | `OPEN` | `ProblemDetail` error model (`ErrorCode`, `GlobalExceptionHandler`), `PageResponse`/`PageRequests`, `CurrentUser` (reads the caller's id from the `Jwt` principal), cross-cutting `@Configuration` (OpenAPI, `Clock`) | — (leaf; every module may use it) |
| `auth` | named | Accounts, BCrypt password hashing, JWT issuing/validation (`JwtConfig`, `TokenService`), refresh-token rotation, cookies (`AuthCookies`), the Spring Security filter chain (`SecurityConfig`), `AuthController` | `shared` |
| `task` | named | Task CRUD, filter/search `Specification`s, `TaskController` | `shared` |

Each module keeps its implementation under an `internal` sub-package (e.g.
`auth.internal.security`, `task.internal.repository`); classes in `internal` are package-private or
otherwise inaccessible from outside the module, so `task` cannot reach into `auth`'s persistence layer even
by accident. `task` identifies the task owner by the `Long ownerId` carried in every `Task` row and read
from the JWT subject claim — there is no JPA relationship to `auth`'s `User` entity, which is what lets the
two modules stay decoupled. A `ModularityTest` (`backend/src/test/java/.../ModularityTest.java`) runs
`ApplicationModules.of(...).verify()` on every build and fails if a boundary is violated.

## Request flow: an authenticated web request

Example: the SPA lists tasks after the access-token cookie has expired.

```mermaid
sequenceDiagram
    participant SPA as Browser (SPA)
    participant Proxy as nginx / Vite proxy
    participant Sec as Security filter chain
    participant Ctrl as TaskController
    participant DB as MySQL

    SPA->>Proxy: GET /api/tasks (cookie access_token expired)
    Proxy->>Sec: forward request
    Sec-->>Proxy: 401 UNAUTHENTICATED (ProblemDetail)
    Proxy-->>SPA: 401 UNAUTHENTICATED
    SPA->>Proxy: POST /api/auth/refresh (cookie refresh_token, X-XSRF-TOKEN)
    Proxy->>Sec: forward request
    Sec->>Ctrl: AuthController.refresh
    Ctrl->>DB: rotate refresh token (consume old, issue new)
    DB-->>Ctrl: ok
    Ctrl-->>Proxy: 200 + Set-Cookie access_token, refresh_token, XSRF-TOKEN
    Proxy-->>SPA: 200
    SPA->>Proxy: GET /api/tasks (replay, new access_token cookie)
    Proxy->>Sec: forward request
    Sec->>Ctrl: TaskController.list(ownerId from Jwt)
    Ctrl->>DB: SELECT ... WHERE owner_id = ? ORDER BY updated_at DESC
    DB-->>Ctrl: page of tasks
    Ctrl-->>Proxy: 200 PageResponse<TaskResponse>
    Proxy-->>SPA: 200
```

The client-side half of this (single-flight refresh, replay-once) is implemented in
`frontend/src/lib/api-client.ts`. See `docs/security.md` for the full login/refresh/logout sequences and
the reuse-detection path.

## Data model

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : owns
    USERS {
        bigint id PK
        varchar_254 email UK
        varchar_100 password_hash
        datetime_6 created_at
        datetime_6 updated_at
    }
    TASKS {
        bigint id PK
        bigint owner_id "FK-like; no JPA relation (see auth/task decoupling)"
        varchar_200 title
        varchar_2000 description "nullable"
        varchar_20 status "TODO | IN_PROGRESS | DONE"
        datetime_6 created_at
        datetime_6 updated_at
    }
    REFRESH_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar_64 token_hash UK "SHA-256 hex, never the raw token"
        datetime_6 expires_at
        datetime_6 used_at "nullable; set on rotation"
        datetime_6 revoked_at "nullable; set on logout or reuse detection"
        datetime_6 created_at
    }
```

`tasks.owner_id` references `users.id` at the database level (`ON DELETE CASCADE`) but is deliberately not
a JPA `@ManyToOne` — see the module boundary note above. Indexes: `tasks(owner_id, status, updated_at)` and
`tasks(owner_id, updated_at)` serve the list endpoint's filter and sort; `refresh_tokens(user_id)` serves
the "revoke all sessions" reuse-detection path. Full DDL:
`backend/src/main/resources/db/migration/V1__init_schema.sql`.

## Profiles and configuration

| Profile | Used for | Notable settings |
|---|---|---|
| `dev` (default) | `./mvnw spring-boot:run` against the docker-compose MySQL | `app.auth.cookie-secure=false` (plain HTTP locally), debug logging |
| `test` | `@SpringBootTest` / `@ActiveProfiles("test")` | H2 in MySQL mode (`MODE=MySQL`), a throwaway JWT secret, Flyway still runs the real migration |
| `prod` | Cloud Run | `server.forward-headers-strategy=framework` (trusts `X-Forwarded-*` behind the TLS-terminating proxy), `app.auth.cookie-secure=true` |

All three read `spring.datasource.*`, `app.jwt.secret`, `app.jwt.issuer`, `app.jwt.access-token-ttl`
(`15m`), `app.jwt.refresh-token-ttl` (`7d`) and `app.cors.allowed-origins` from environment variables
(`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`), loaded
either from `backend/.env` locally (`spring.config.import=optional:file:.env[.properties]`) or injected
directly in CI/Cloud Run. See `backend/README.md` for the full table.
