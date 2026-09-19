# Task Manager — backend

Spring Boot 4.1.1 REST API (Java 21, Spring Modulith, MySQL). See the [root README](../README.md) for the
full-stack picture and [`docs/api-contract.md`](../docs/api-contract.md) for the endpoint reference.

## Run locally

1. Start MySQL from the repository root: `docker compose up -d` (see the root README if you don't use
   Docker — any local MySQL 8.4 works, just point `DATABASE_URL` at it).
2. Copy the env file and fill in the secret:
   ```
   cp backend/.env.example backend/.env
   ```
   Generate `JWT_SECRET` with `openssl rand -base64 32` (or
   `node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"`) and paste it in.
3. Run the app:
   ```
   ./mvnw spring-boot:run
   ```
   API on `http://localhost:8080`, Swagger UI on `http://localhost:8080/swagger-ui.html`.

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Configuration

Read from environment variables (or `backend/.env` locally — git-ignored, loaded via
`spring.config.import=optional:file:.env[.properties]`).

| Variable | Required | Default | Notes |
|---|---|---|---|
| `DATABASE_URL` | yes | — | JDBC URL, e.g. `jdbc:mysql://localhost:3306/taskmanager` |
| `DATABASE_USERNAME` | yes | — | |
| `DATABASE_PASSWORD` | yes | — | |
| `JWT_SECRET` | yes | — | Base64-encoded HMAC key, must decode to ≥ 32 bytes; the app refuses to start otherwise |
| `CORS_ALLOWED_ORIGINS` | no | empty | Comma-separated origins; leave empty when the SPA is same-origin (the default deployment) |
| `PORT` | no | `8080` | |

Fixed (not env-configurable) but relevant to know: `app.jwt.access-token-ttl=15m`,
`app.jwt.refresh-token-ttl=7d`, `app.jwt.issuer=task-manager-api`.

## Profiles

| Profile | Activated by | Purpose |
|---|---|---|
| `dev` (default, `spring.profiles.default`) | nothing — active unless overridden | Local run against docker-compose MySQL; `app.auth.cookie-secure=false` so cookies work over plain HTTP; debug logging |
| `test` | `@ActiveProfiles("test")` in `IntegrationTest` | H2 in MySQL mode (`MODE=MySQL`) with Flyway running the real migration, throwaway JWT secret |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` (set by the deploy pipeline) | Trusts `X-Forwarded-*` behind the Cloud Run TLS proxy, `app.auth.cookie-secure=true` |

## Module layout (Spring Modulith)

```
io.github.navysama.taskmanager
├── auth/            accounts, JWT, refresh tokens, security filter chain, cookies
│   └── internal/    config, dto, model, repository, security, service, web — not visible outside auth
├── task/            task CRUD, filtering, search
│   └── internal/    dto, model, repository, service, web — not visible outside task
└── shared/          OPEN module: ProblemDetail error model, PageResponse, CurrentUser, cross-cutting config
```

`task` depends only on `shared` — it identifies the owner by the `ownerId: Long` read from the JWT, never
through a JPA relation to `auth`'s `User` entity, so the two modules stay independently testable.
`ModularityTest` (`src/test/java/.../ModularityTest.java`) runs `ApplicationModules.verify()` and fails the
build if a boundary is crossed. Full diagram and rationale: `docs/architecture.md`.

## Error model

Every error is an RFC 9457 `ProblemDetail` (`application/problem+json`) with a stable `code` extension and,
for field validation failures, an `errors: [{ field, message }]` array. `GlobalExceptionHandler`
(`shared/error`) is the single place this is assembled, including for `401`/`403` raised inside the
security filter chain before the DispatcherServlet ever sees the request. Full status/code catalogue:
`docs/api-contract.md` §4.

## Tests and quality gates

```
./mvnw test                          # unit tests only (*Test, surefire)
./mvnw verify                        # unit + integration tests (*IT, failsafe), Spotless check, JaCoCo gate
./mvnw spotless:apply                # auto-format (also runs automatically before every compile)
./mvnw verify -Dspotless.apply.skip=true   # what CI runs: fails if code wasn't already formatted
```

- **Unit tests** (`*Test`): JUnit 5 + Mockito + AssertJ, no Spring context.
- **Integration tests** (`*IT`): `@SpringBootTest` + MockMvc with the real security filter chain, against H2
  in MySQL mode locally and a real MySQL 8.4 service in CI (same Flyway migration both times).
- **`ModularityTest`**: verifies the module boundaries described above.
- **Coverage**: JaCoCo, enforced minimum 80% line coverage on `verify` (`jacoco.minimum.coverage`).
- **Formatting**: Spotless with `palantirJavaFormat`, applied automatically in `process-sources` and
  checked again in `verify`; CI passes `-Dspotless.apply.skip=true` so an unformatted commit fails the
  build instead of silently passing.

## Migrations policy

Flyway owns the schema; Hibernate runs with `ddl-auto=validate` and never generates DDL.

- **`V1__init_schema.sql` is immutable.** Never edit an already-applied migration — Flyway checksums it and
  will refuse to start if it changes underneath a database that already ran it.
- Add new migrations as `V2__<description>.sql`, `V3__...`, etc., under
  `src/main/resources/db/migration/`.
- Write SQL portable between MySQL 8.4 and H2 in MySQL mode (the local test database), since integration
  tests run the same migrations.
