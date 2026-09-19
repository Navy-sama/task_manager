<div align="center">

# ⚙️ Task Manager — Backend

### Spring Boot REST API for the Task Manager recruitment test

JWT auth (register/login/refresh/logout), task CRUD with filtering, search and pagination — Java 21, Spring Modulith, MySQL

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-2.1.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-modulith)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

[Root README](../README.md) • [Quick Start](#-quick-start) • [Configuration](#-configuration) • [API Contract](../docs/api-contract.md)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Profiles](#-profiles)
- [Module Layout](#-module-layout-spring-modulith)
- [Error Model](#-error-model)
- [Migrations Policy](#-migrations-policy)
- [Tests & Quality Gates](#-tests--quality-gates)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Overview

Spring Boot 4.1.1 REST API (Java 21, Spring Modulith, MySQL). See the [root README](../README.md) for the full-stack picture and [`docs/api-contract.md`](../docs/api-contract.md) for the endpoint reference.

---

## 🛠 Tech Stack

<details open>
<summary><b>🎯 Framework</b></summary>

| Package                                          | Version | Purpose                                         |
| ------------------------------------------------ | ------- | ----------------------------------------------- |
| Java                                             | `21`    | Language / runtime                              |
| Spring Boot (`spring-boot-starter-parent`)       | `4.1.1` | Application framework                           |
| Spring Modulith (`spring-modulith-starter-core`) | `2.1.1` | Module boundaries, enforced by `ModularityTest` |

</details>

<details>
<summary><b>🔐 Security & Auth</b></summary>

| Package                                      | Purpose                         |
| -------------------------------------------- | ------------------------------- |
| `spring-boot-starter-security`               | Security filter chain           |
| `spring-boot-starter-oauth2-resource-server` | JWT decoding/validation support |

</details>

<details>
<summary><b>💾 Persistence</b></summary>

| Package                                       | Version               | Purpose                                                   |
| --------------------------------------------- | --------------------- | --------------------------------------------------------- |
| `spring-boot-starter-data-jpa`                | (Spring Boot–managed) | JPA / Hibernate, `ddl-auto=validate`                      |
| `mysql-connector-j`                           | (Spring Boot–managed) | MySQL JDBC driver                                         |
| `spring-boot-starter-flyway` + `flyway-mysql` | (Spring Boot–managed) | Schema migrations                                         |
| `mysql-socket-factory-connector-j-8`          | `1.30.0`              | Cloud SQL socket connections (no public IP), runtime-only |
| H2                                            | (Spring Boot–managed) | In-memory, MySQL-mode database for integration tests      |

</details>

<details>
<summary><b>📖 API Docs</b></summary>

| Package                               | Version | Purpose                                      |
| ------------------------------------- | ------- | -------------------------------------------- |
| `springdoc-openapi-starter-webmvc-ui` | `3.1.1` | OpenAPI / Swagger UI generated from the code |

</details>

<details>
<summary><b>🧪 Testing & Quality</b></summary>

| Package                             | Version               | Purpose                                                       |
| ----------------------------------- | --------------------- | ------------------------------------------------------------- |
| `spring-boot-starter-webmvc-test`   | (Spring Boot–managed) | MockMvc                                                       |
| `spring-boot-starter-security-test` | (Spring Boot–managed) | Security test support                                         |
| `spring-boot-starter-data-jpa-test` | (Spring Boot–managed) | JPA slice tests                                               |
| `spring-modulith-starter-test`      | `2.1.1`               | `ApplicationModules.verify()` (`ModularityTest`)              |
| JaCoCo                              | `0.8.15`              | ≥ 80% line coverage gate on `verify`                          |
| Spotless (`palantirJavaFormat`)     | `3.10.2`              | Formatting, applied on `process-sources`, checked on `verify` |
| `maven-failsafe-plugin`             | (Spring Boot–managed) | Runs `*IT` integration tests separately from unit tests       |

</details>

---

## 📦 Prerequisites

<table>
<tr>
<td width="50%">

### 🖥 Required

- ✅ Java 21 (Temurin recommended)
- ✅ Maven wrapper included (`mvnw` / `mvnw.cmd`) — no local Maven install needed

</td>
<td width="50%">

### 💾 Database

- ✅ MySQL 8.4 (via the root `docker compose up -d`, or any local install)
- ✅ H2 is used automatically for tests — no setup needed

</td>
</tr>
</table>

---

## 🚀 Quick Start

### 1️⃣ Start MySQL

```bash
# from the repository root
docker compose up -d
```

See the root README if you don't use Docker — any local MySQL 8.4 works, just point `DATABASE_URL` at it.

### 2️⃣ Configure the environment

```bash
cp backend/.env.example backend/.env
```

Generate `JWT_SECRET` with `openssl rand -base64 32` (or `node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"`) and paste it in.

### 3️⃣ Run the app

```bash
./mvnw spring-boot:run
```

> 🎉 API on `http://localhost:8080`, Swagger UI on `http://localhost:8080/swagger-ui.html`.

> 🪟 **Windows:** use `mvnw.cmd` instead of `./mvnw`.

---

## ⚙️ Configuration

Read from environment variables (or `backend/.env` locally — git-ignored, loaded via `spring.config.import=optional:file:.env[.properties]`).

| Variable               | Required | Default | Notes                                                                                     |
| ---------------------- | -------- | ------- | ----------------------------------------------------------------------------------------- |
| `DATABASE_URL`         | yes      | —       | JDBC URL, e.g. `jdbc:mysql://localhost:3306/taskmanager`                                  |
| `DATABASE_USERNAME`    | yes      | —       |                                                                                           |
| `DATABASE_PASSWORD`    | yes      | —       |                                                                                           |
| `JWT_SECRET`           | yes      | —       | Base64-encoded HMAC key, must decode to ≥ 32 bytes; the app refuses to start otherwise    |
| `CORS_ALLOWED_ORIGINS` | no       | empty   | Comma-separated origins; leave empty when the SPA is same-origin (the default deployment) |
| `PORT`                 | no       | `8080`  |                                                                                           |

Fixed (not env-configurable) but relevant to know: `app.jwt.access-token-ttl=15m`, `app.jwt.refresh-token-ttl=7d`, `app.jwt.issuer=task-manager-api`.

---

## 🏷 Profiles

| Profile                                    | Activated by                                               | Purpose                                                                                                               |
| ------------------------------------------ | ---------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `dev` (default, `spring.profiles.default`) | nothing — active unless overridden                         | Local run against docker-compose MySQL; `app.auth.cookie-secure=false` so cookies work over plain HTTP; debug logging |
| `test`                                     | `@ActiveProfiles("test")` in `IntegrationTest`             | H2 in MySQL mode (`MODE=MySQL`) with Flyway running the real migration, throwaway JWT secret                          |
| `prod`                                     | `SPRING_PROFILES_ACTIVE=prod` (set by the deploy pipeline) | Trusts `X-Forwarded-*` behind the Cloud Run TLS proxy, `app.auth.cookie-secure=true`                                  |

---

## 📁 Module Layout (Spring Modulith)

```
io.github.navysama.taskmanager
├── 🔐 auth/            # accounts, JWT, refresh tokens, security filter chain, cookies
│   └── internal/       #   config, dto, model, repository, security, service, web — not visible outside auth
├── ✅ task/            # task CRUD, filtering, search
│   └── internal/       #   dto, model, repository, service, web — not visible outside task
└── 🌐 shared/          # OPEN module: ProblemDetail error model, PageResponse, CurrentUser, cross-cutting config
```

`task` depends only on `shared` — it identifies the owner by the `ownerId: Long` read from the JWT, never through a JPA relation to `auth`'s `User` entity, so the two modules stay independently testable. `ModularityTest` (`src/test/java/.../ModularityTest.java`) runs `ApplicationModules.verify()` and fails the build if a boundary is crossed. Full diagram and rationale: [`../docs/architecture.md`](../docs/architecture.md).

---

## 🧯 Error Model

Every error is an RFC 9457 `ProblemDetail` (`application/problem+json`) with a stable `code` extension and, for field validation failures, an `errors: [{ field, message }]` array. `GlobalExceptionHandler` (`shared/error`) is the single place this is assembled, including for `401`/`403` raised inside the security filter chain before the DispatcherServlet ever sees the request. Full status/code catalogue: [`../docs/api-contract.md`](../docs/api-contract.md) §4.

---

## 🗃 Migrations Policy

Flyway owns the schema; Hibernate runs with `ddl-auto=validate` and never generates DDL.

- **`V1__init_schema.sql` is immutable.** Never edit an already-applied migration — Flyway checksums it and will refuse to start if it changes underneath a database that already ran it.
- Add new migrations as `V2__<description>.sql`, `V3__...`, etc., under `src/main/resources/db/migration/`.
- Write SQL portable between MySQL 8.4 and H2 in MySQL mode (the local test database), since integration tests run the same migrations.

---

## 🧪 Tests & Quality Gates

```bash
./mvnw test                                # unit tests only (*Test, surefire)
./mvnw verify                               # unit + integration tests (*IT, failsafe), Spotless check, JaCoCo gate
./mvnw spotless:apply                       # auto-format (also runs automatically before every compile)
./mvnw verify -Dspotless.apply.skip=true    # what CI runs: fails if code wasn't already formatted
```

| Tooling                         | Purpose                                                                                                                                                                            |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| JUnit 5 + Mockito + AssertJ     | **Unit tests** (`*Test`) — no Spring context                                                                                                                                       |
| `@SpringBootTest` + MockMvc     | **Integration tests** (`*IT`) — real security filter chain, against H2 in MySQL mode locally and a real MySQL 8.4 service in CI (same Flyway migration both times)                 |
| `ModularityTest`                | Verifies the module boundaries described above                                                                                                                                     |
| JaCoCo                          | Coverage, enforced minimum 80% line coverage on `verify` (`jacoco.minimum.coverage`)                                                                                               |
| Spotless (`palantirJavaFormat`) | Applied automatically in `process-sources`, checked again in `verify`; CI passes `-Dspotless.apply.skip=true` so an unformatted commit fails the build instead of silently passing |

---

## 🔧 Troubleshooting

<details>
<summary><b>🔑 App refuses to start — JWT secret</b></summary>

`JWT_SECRET` must be a Base64-encoded HMAC key that decodes to at least 32 bytes. Regenerate with `openssl rand -base64 32`.

</details>

<details>
<summary><b>🎨 `verify` fails with a Spotless error</b></summary>

Run `./mvnw spotless:apply` to auto-format, then commit and re-run `./mvnw verify`.

</details>

<details>
<summary><b>🗃 Flyway checksum mismatch</b></summary>

An already-applied migration (e.g. `V1__init_schema.sql`) was edited. Never edit an applied migration — add a new `V2__...` file instead.

</details>

<details>
<summary><b>🪟 Windows — `mvnw` not recognized</b></summary>

Use `mvnw.cmd` instead of `./mvnw` for every command in this README.

</details>

---

<div align="center">

Part of the [Task Manager](../README.md) monorepo.

[⬆ Back to root README](../README.md)

</div>
