<div align="center">

# ⚛️ Task Manager — Frontend

### React + Vite SPA for the Task Manager recruitment test

Auth, task list with filter/search/pagination, CRUD and toasts — TypeScript, Tailwind CSS, shadcn/ui, TanStack Query

[![React](https://img.shields.io/badge/React-19.3-61DAFB?style=for-the-badge&logo=react&logoColor=white)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.3-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vite.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![TailwindCSS](https://img.shields.io/badge/Tailwind%20CSS-4.3-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

[Root README](../README.md) • [Quick Start](#-quick-start) • [Scripts](#-scripts) • [Testing](#-testing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Scripts](#-scripts)
- [Project Structure](#-project-structure)
- [API Client & Auth Flow](#-api-client--auth-flow)
- [Internationalization](#-internationalization)
- [Testing](#-testing)
- [Environment & Proxy](#-environment--proxy)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Overview

React 19 + Vite + TypeScript SPA (Tailwind 4 + shadcn/ui, TanStack Query, React Hook Form + Zod). See the [root README](../README.md) for the full-stack picture and [`docs/api-contract.md`](../docs/api-contract.md) for the endpoint reference this client is built against.

---

## 🛠 Tech Stack

<details open>
<summary><b>🎯 Framework</b></summary>

```
React 19.3.0  •  Vite 8.3.0  •  TypeScript 6.0.3
```

</details>

<details>
<summary><b>📊 State & Data</b></summary>

| Package                 | Version    | Purpose                                       |
| ----------------------- | ---------- | --------------------------------------------- |
| `@tanstack/react-query` | `^5.103.1` | Server cache & async requests                 |
| `axios`                 | `^1.20.0`  | HTTP client — cookies + CSRF header injection |
| `zustand`               | `^5.0.15`  | Auth session state                            |

</details>

<details>
<summary><b>📝 Forms & Validation</b></summary>

| Package               | Version   | Purpose               |
| --------------------- | --------- | --------------------- |
| `react-hook-form`     | `^7.88.0` | Form state            |
| `zod`                 | `^4.6.5`  | Schema validation     |
| `@hookform/resolvers` | `^5.9.1`  | Zod ↔ RHF integration |

</details>

<details>
<summary><b>🎨 UI & Styling</b></summary>

| Package                             | Version             | Purpose                               |
| ----------------------------------- | ------------------- | ------------------------------------- |
| `tailwindcss` + `@tailwindcss/vite` | `^4.3.3`            | Utility-first styling                 |
| `radix-ui`                          | `^1.6.7`            | shadcn/ui-style accessible primitives |
| `class-variance-authority`          | `^0.7.1`            | Variant-based component styling       |
| `clsx` + `tailwind-merge`           | `^2.1.1` + `^3.7.0` | Conditional / merged class names      |
| `lucide-react`                      | `^1.47.0`           | Icons                                 |
| `sonner`                            | `^2.0.8`            | Toasts                                |
| `react-router`                      | `^8.4.0`            | Routing                               |

</details>

<details>
<summary><b>🌍 Internationalization</b></summary>

| Package         | Version    | Purpose           |
| --------------- | ---------- | ----------------- |
| `i18next`       | `^26.4.2`  | i18n engine       |
| `react-i18next` | `^17.0.14` | React integration |

Languages: **English** and **French**. See [Internationalization](#-internationalization).

</details>

<details>
<summary><b>🧪 Testing & Dev Tooling</b></summary>

| Package                        | Version                | Purpose                     |
| ------------------------------ | ---------------------- | --------------------------- |
| `vitest`                       | `^5.0.1`               | Test runner (jsdom)         |
| `@testing-library/react`       | `^16.3.3`              | Component tests             |
| `@testing-library/user-event`  | `^14.6.7`              | User-interaction simulation |
| `@testing-library/jest-dom`    | `^7.0.1`               | DOM matchers                |
| `msw`                          | `^2.15.0`              | Mocked API for tests        |
| `eslint` + `typescript-eslint` | `^10.10.0` + `^8.70.0` | Linting                     |
| `prettier`                     | `^3.9.8`               | Formatting                  |

</details>

---

## 📦 Prerequisites

<table>
<tr>
<td width="50%">

### 🖥 Required

- ✅ Node.js `≥ 24` (`engines.node` in `package.json`)
- ✅ pnpm

</td>
<td width="50%">

### 🔌 Backend

- ✅ The API must be running first — see [`backend/README.md`](../backend/README.md)

</td>
</tr>
</table>

---

## 🚀 Quick Start

### 1️⃣ Install dependencies

```bash
pnpm install
```

### 2️⃣ Configure the environment (optional)

```bash
cp .env.example .env
# only needed if the API isn't on http://localhost:8080 — edit VITE_API_PROXY_TARGET
```

### 3️⃣ Start the dev server

```bash
pnpm dev
```

> 🎉 Opens on `http://localhost:5173`. Requests to `/api` are proxied to the backend (`http://localhost:8080` by default) so the browser only ever talks to its own origin — required for the `SameSite=Strict` auth cookies and the CSRF cookie to work.

---

## 📜 Scripts

<table>
<tr>
<td width="50%">

### 🚀 Development

```bash
pnpm dev          # Vite dev server with the /api proxy
pnpm build        # tsc -b then production build
pnpm preview      # Serve the production build (port 4173, same proxy)
```

</td>
<td width="50%">

### 🧪 Quality & Tests

```bash
pnpm lint         # ESLint + Prettier check
pnpm format       # ESLint --fix + Prettier write
pnpm typecheck    # tsc -b only
pnpm test         # Vitest run
pnpm test:watch   # Vitest watch mode
```

</td>
</tr>
</table>

---

## 📁 Project Structure

```
src/
├── 🧭 app/               # router, providers (QueryClient, auth bootstrap), root App
├── 🧩 components/        # shared components; components/ui/ are shadcn/ui primitives
├── 🗂 features/
│   ├── 🔐 auth/          #   api.ts, hooks.ts, schemas.ts (Zod), auth-store.ts (Zustand), components/, pages/
│   └── ✅ tasks/         #   same shape: api, hooks, schemas, components (list, filters, form dialog, pagination), pages
├── 🌍 i18n/              # i18next setup, locales/en.json + fr.json
├── 🛠 lib/               # api-client (axios), api-error, query-client, form-errors, utils
└── 🧪 test/              # MSW server + handlers, custom render, setup
```

Each feature owns its API calls, validation schemas, hooks and components — nothing outside a feature imports its internals directly.

---

## 🌐 API Client & Auth Flow

`src/lib/api-client.ts` is the single axios instance used everywhere:

- `baseURL: "/api"`, `withCredentials: true` — cookies (`access_token`, `refresh_token`, `XSRF-TOKEN`) are sent automatically; the app never reads or stores a token itself.
- `xsrfCookieName: "XSRF-TOKEN"` / `xsrfHeaderName: "X-XSRF-TOKEN"` — axios copies the CSRF cookie into the request header on every same-origin mutation, satisfying the backend's double-submit check.
- A response interceptor handles `401`s from any endpoint except `login`/`register`/`refresh`/`logout` (those `401`s are business answers, not session expiry): it triggers `POST /api/auth/refresh`, shares one in-flight refresh between concurrent failing requests, and replays the original request exactly once. If the refresh itself fails (and it wasn't a network error), it calls the session-expired handler registered by the auth feature and the user is signed out.
- Session state (`loading` / `authenticated` / `unauthenticated`, current user) lives in a small Zustand store (`features/auth/auth-store.ts`). `AuthBootstrap` calls `GET /api/auth/me` on mount to resolve it — this is also what obtains the first `XSRF-TOKEN` cookie before any `POST` is attempted, per [`docs/api-contract.md`](../docs/api-contract.md) §5.

---

## 🌍 Internationalization

English and French, via `i18next` + `react-i18next` (`src/i18n/`). Language is detected from a stored preference (`localStorage`, key `task-manager.language`), falling back to the browser language (French or English), falling back to English. `LanguageSwitcher` lets the user override it at runtime; all user-facing strings, including API error messages (mapped from the backend's `code` — see `src/i18n/error-message.ts`), come from `locales/en.json` / `locales/fr.json`, kept in sync key-for-key.

---

## 🧪 Testing

Vitest + React Testing Library + MSW (`src/test/`):

| Tooling                               | Role                                                                                                               |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `test/server.ts` + `test/handlers.ts` | MSW server with an in-memory task list and mock auth/task endpoints, reset between tests                           |
| `test/render.tsx`                     | Wraps components with the real providers (router, query client, i18n) so tests exercise the same tree the app runs |
| `*.schemas.test.ts`                   | Schema validation coverage                                                                                         |
| `ProtectedRoute.test.tsx`             | Route guards                                                                                                       |
| `LoginForm.test.tsx`                  | Forms                                                                                                              |
| `TasksPage.test.tsx`                  | Full page flow — filter, search, create/edit/delete against the mock API                                           |

Run with `pnpm test` (CI mode) or `pnpm test:watch` while developing.

---

## ⚙️ Environment & Proxy

| Variable                | Default                 | Purpose                                          |
| ----------------------- | ----------------------- | ------------------------------------------------ |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Backend the dev/preview server proxies `/api` to |

The proxy is defined once in `vite.config.ts` and applies to both `pnpm dev` and `pnpm preview`. In production there is no Vite proxy: nginx plays the same role (see [`docs/deployment.md`](../docs/deployment.md)).

---

## 🔧 Troubleshooting

<details>
<summary><b>🌐 Requests to `/api` fail or hit the wrong backend</b></summary>

Check `VITE_API_PROXY_TARGET` in `.env` (defaults to `http://localhost:8080`), and make sure the backend is actually running — see [`backend/README.md`](../backend/README.md).

</details>

<details>
<summary><b>🍪 Login works but subsequent requests get `401`</b></summary>

The SPA must be accessed through the proxy origin (`http://localhost:5173` in dev, or the nginx origin in the Docker/prod build) — the `SameSite=Strict` auth cookies and CSRF cookie only work same-origin.

</details>

<details>
<summary><b>🎨 `pnpm lint` fails on formatting</b></summary>

Run `pnpm format` (ESLint `--fix` + Prettier `--write`) then re-run `pnpm lint`.

</details>

---

<div align="center">

Part of the [Task Manager](../README.md) monorepo.

[⬆ Back to root README](../README.md)

</div>
