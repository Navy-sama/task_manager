# Architecture decisions

Lightweight ADRs for the choices made while building this test. Each entry is context / decision /
consequences in a few lines. Numbering (D1-D15) matches the internal design notes; some numbers were
reserved for later phases and are folded into the deviations section below.

## D1 — Versions

**Context:** needed a Boot version with a long support window and a Modulith release that tracks it.
**Decision:** Java 21, Spring Boot 4.1.1 (OSS support into mid-2027), Spring Modulith 2.1.1, springdoc 3.1.x.
**Consequences:** modern baseline (virtual threads available, current Spring Security API); no LTS risk
during the test window.

## D2 — JWT via OAuth2 Resource Server

**Context:** Spring Security ships a resource-server module purpose-built for bearer-token validation.
**Decision:** issue tokens with `JwtEncoder` (Nimbus, HS256); validate them with the standard
`oauth2ResourceServer().jwt()` filter instead of a hand-rolled filter.
**Consequences:** signature/expiry/issuer checks come from a maintained library; a custom
`BearerTokenResolver` was still needed to also accept the token from a cookie (see D3a).

## D3a — Web session: httpOnly cookies

**Context:** the spec allows either a cookie or `localStorage` for the JWT.
**Decision:** `HttpOnly` cookies for the web client, served through a same-origin proxy (Vite dev proxy,
nginx in production) so the browser never talks cross-origin.
**Consequences:** access token is invisible to JavaScript, closing the main XSS token-theft vector;
same-origin also makes `SameSite=Strict` viable and removes the need for a CORS allow-list in the default
setup.

## D3b — Mobile: dual token transport

**Context:** a native app cannot rely on cookies the way a browser does.
**Decision:** one `BearerTokenResolver` reads `Authorization: Bearer` first, then falls back to the
`access_token` cookie; `/api/auth/**` responses include the tokens in the JSON body only when the caller
sends `X-Client-Type: mobile`.
**Consequences:** a single backend serves both clients with no duplicated endpoints; the header is the only
thing that distinguishes a mobile request from a web one.

## D3c — CSRF: double-submit cookie

**Context:** `SameSite=Strict` cookies stop most cross-site requests, but defence in depth is cheap.
**Decision:** Spring Security's SPA CSRF support (`CookieCsrfTokenRepository`, readable `XSRF-TOKEN` cookie)
on every non-GET web request; Bearer and `X-Client-Type: mobile` requests are exempt because a browser
cannot attach those headers cross-site without a preflight the API rejects.
**Consequences:** one extra header on every mutation from the web client; mobile is unaffected.

## D4 — Test strategy

**Context:** integration tests need a real relational engine, but spinning up MySQL for every local run is
slow.
**Decision:** unit tests with JUnit 5 + Mockito + AssertJ; integration tests (`@SpringBootTest` + MockMvc)
run on H2 in MySQL mode with Flyway applying the real migration locally, and against a real MySQL 8.4
service in CI; JaCoCo enforces a minimum 80% line coverage on `verify`.
**Consequences:** fast local feedback loop; CI still catches MySQL-specific behaviour (e.g. the `CHECK`
constraint on `status`) that H2 might not enforce identically.

## D5 — Schema management: Flyway

**Context:** `hibernate.ddl-auto=update` is unsafe for anything beyond a prototype.
**Decision:** Flyway owns the schema (`V1__init_schema.sql`), Hibernate runs with `ddl-auto=validate`; the
SQL is written to be portable between H2 (MySQL mode) and real MySQL 8.4.
**Consequences:** schema changes are explicit, reviewable, versioned files; the entity mapping is checked
against the real schema at startup instead of silently drifting.

## D6 — Errors: RFC 9457 Problem Details

**Context:** clients need a stable, machine-readable error shape, not just a status code.
**Decision:** every error response is a `ProblemDetail` (`application/problem+json`) with a stable `code`
extension and, for validation failures, an `errors[]` array — including 401/403 raised in the security
filter chain, before the DispatcherServlet.
**Consequences:** one error contract for the whole API; clients branch on `code`, never on `detail`.

## D7 — Pagination

**Context:** the task list needs to scale and stay predictable.
**Decision:** `?page=&size=` with `size` clamped to `1..100` and a default of 20, sorted by
`updatedAt DESC, id DESC`; responses use a custom `PageResponse<T>` instead of serializing Spring's `Page`
directly.
**Consequences:** out-of-range input degrades gracefully instead of erroring or hitting the database
unbounded; the response shape is stable regardless of the Spring Data version.

## D8 — Build: Maven + wrapper

**Decision:** Maven with `mvnw`/`mvnw.cmd` committed, so no local Maven install is required.
**Consequences:** `./mvnw verify` is the single reproducible entry point for CI and local runs alike.

## D9 — Success responses: bare DTOs

**Decision:** success responses return the resource directly (`TaskResponse`, `AuthResponse`, …), no
`{ data: ... }` envelope.
**Consequences:** simpler client typing; consistent with `PageResponse<T>` already carrying its own paging
metadata.

## D10 — Light Spring Modulith layout

**Context:** the test is small, but module boundaries still matter for reviewability.
**Decision:** three modules — `auth`, `task`, `shared` (`OPEN`) — each with an `internal` package that
nothing outside the module may import; `ModularityTest` verifies the boundaries on every build.
**Consequences:** `task` never gets a JPA relationship to `auth`'s `User` entity (it only knows an
`ownerId: Long`), keeping the modules independently testable; the trade-off is a bit more indirection than
a flat package would need for a project this size.

## Deferred decisions, now implemented

- **D9 (frontend):** React Router, Zustand, TanStack Query, React Hook Form + Zod, axios, Tailwind 4 + shadcn/ui, sonner,
  Vitest + React Testing Library + MSW, Node 24 LTS — all as planned. **pnpm** was used as
  the package manager instead of npm.
- **D11 (mobile, bonus):** Flutter, dio, flutter_secure_storage, provider — see `mobile/README.md`.
- **D12 (deployment, bonus):** Cloud Run (backend + nginx-served frontend proxying `/api`) + Cloud SQL
  MySQL 8.4 + Secret Manager + Artifact Registry + Workload Identity Federation — see `docs/deployment.md`.
- **Java formatter:** Spotless with `palantirJavaFormat`, applied automatically on build and checked on
  `verify` (CI runs with the apply step skipped, so an unformatted commit fails).

## Deviations from the written spec, summarized

| Spec says                         | Shipped instead                                        | Why                                                                                                                                                         |
| --------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| JWT in cookie _or_ `localStorage` | HttpOnly cookie for web, Bearer body tokens for mobile | `localStorage` is readable by any script on the page; HttpOnly cookies remove that attack surface for the web client, and mobile needed body tokens anyway. |
| 4 endpoints                       | 4 endpoints + `refresh`, `logout`, `me`                | Short-lived access tokens are unusable without a refresh flow.                                                                                              |
