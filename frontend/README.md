# Task Manager — frontend

React 19 + Vite + TypeScript SPA (Tailwind 4 + shadcn/ui, TanStack Query, React Hook Form + Zod). See the
[root README](../README.md) for the full-stack picture and [`docs/api-contract.md`](../docs/api-contract.md)
for the endpoint reference this client is built against.

## Run locally

The API must be running first (see `backend/README.md`), then:

```
pnpm install
pnpm dev
```

Opens on `http://localhost:5173`. Requests to `/api` are proxied to the backend (`http://localhost:8080` by
default) so the browser only ever talks to its own origin — required for the `SameSite=Strict` auth cookies
and the CSRF cookie to work. Copy `.env.example` to `.env` and change `VITE_API_PROXY_TARGET` only if the
API runs somewhere else.

## Scripts

| Command           | Purpose                                                    |
| ----------------- | ---------------------------------------------------------- |
| `pnpm dev`        | Vite dev server with the `/api` proxy                      |
| `pnpm build`      | Type-check (`tsc -b`) then production build                |
| `pnpm preview`    | Serve the production build locally (port 4173, same proxy) |
| `pnpm lint`       | ESLint + Prettier check                                    |
| `pnpm format`     | ESLint `--fix` + Prettier write                            |
| `pnpm typecheck`  | `tsc -b` only                                              |
| `pnpm test`       | Vitest run (jsdom, React Testing Library, MSW)             |
| `pnpm test:watch` | Vitest in watch mode                                       |

Requires Node ≥ 24 (`engines.node` in `package.json`) and pnpm.

## Structure

```
src/
├── app/              router, providers (QueryClient, auth bootstrap), root App
├── components/        shared components; components/ui/ are shadcn/ui primitives
├── features/
│   ├── auth/          api.ts, hooks.ts, schemas.ts (Zod), auth-store.ts (Zustand), components/, pages/
│   └── tasks/          same shape: api, hooks, schemas, components (list, filters, form dialog, pagination), pages
├── i18n/              i18next setup, locales/en.json + fr.json
├── lib/               api-client (axios), api-error, query-client, form-errors, utils
└── test/               MSW server + handlers, custom render, setup
```

Each feature owns its API calls, validation schemas, hooks and components — nothing outside a feature
imports its internals directly.

## API client and auth flow

`src/lib/api-client.ts` is the single axios instance used everywhere:

- `baseURL: "/api"`, `withCredentials: true` — cookies (`access_token`, `refresh_token`, `XSRF-TOKEN`) are
  sent automatically; the app never reads or stores a token itself.
- `xsrfCookieName: "XSRF-TOKEN"` / `xsrfHeaderName: "X-XSRF-TOKEN"` — axios copies the CSRF cookie into the
  request header on every same-origin mutation, satisfying the backend's double-submit check.
- A response interceptor handles `401`s from any endpoint except `login`/`register`/`refresh`/`logout`
  (those `401`s are business answers, not session expiry): it triggers `POST /api/auth/refresh`, shares one
  in-flight refresh between concurrent failing requests, and replays the original request exactly once. If
  the refresh itself fails (and it wasn't a network error), it calls the session-expired handler registered
  by the auth feature and the user is signed out.
- Session state (`loading` / `authenticated` / `unauthenticated`, current user) lives in a small Zustand
  store (`features/auth/auth-store.ts`). `AuthBootstrap` calls `GET /api/auth/me` on mount to resolve it —
  this is also what obtains the first `XSRF-TOKEN` cookie before any `POST` is attempted, per
  `docs/api-contract.md` §5.

## i18n

English and French, via `i18next` + `react-i18next` (`src/i18n/`). Language is detected from a stored
preference (`localStorage`, key `task-manager.language`), falling back to the browser language (French or
English), falling back to English. `LanguageSwitcher` lets the user override it at runtime; all user-facing
strings, including API error messages (mapped from the backend's `code` — see
`src/i18n/error-message.ts`), come from `locales/en.json` / `locales/fr.json`, kept in sync key-for-key.

## Testing

Vitest + React Testing Library + MSW (`src/test/`):

- `test/server.ts` + `test/handlers.ts` set up an MSW server with an in-memory task list and mock
  auth/task endpoints, reset between tests.
- `test/render.tsx` wraps components with the real providers (router, query client, i18n) so tests exercise
  the same tree the app runs.
- Coverage spans schema validation (`*.schemas.test.ts`), the auth store, route guards
  (`ProtectedRoute.test.tsx`), forms (`LoginForm.test.tsx`) and a full page flow
  (`TasksPage.test.tsx` — filter, search, create/edit/delete against the mock API).

Run with `pnpm test` (CI mode) or `pnpm test:watch` while developing.

## Environment and proxy

| Variable                | Default                 | Purpose                                          |
| ----------------------- | ----------------------- | ------------------------------------------------ |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Backend the dev/preview server proxies `/api` to |

The proxy is defined once in `vite.config.ts` and applies to both `pnpm dev` and `pnpm preview`. In
production there is no Vite proxy: nginx plays the same role (see `docs/deployment.md`).
