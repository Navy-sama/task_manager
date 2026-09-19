# API contract

Single source of truth for the backend, the web client and the mobile client.

- Base path: `/api`. JSON only (`Content-Type: application/json`).
- Dates: ISO-8601 UTC instants, e.g. `2026-09-19T08:15:30.123Z`.
- Success responses return the resource itself (no envelope). Errors use RFC 9457 Problem Details.
- Local dev: API on `http://localhost:8080`, web app on `http://localhost:5173` (Vite proxies `/api` to the API,
  so the browser only ever talks to its own origin). Swagger UI: `http://localhost:8080/swagger-ui.html`.

## 1. Authentication model

Two client modes share the same JWT access token and the same opaque refresh token:

| | Web (browser) | Mobile |
|---|---|---|
| Token transport | `HttpOnly` cookies set by the API; JavaScript never sees a token | JSON body; client stores tokens in secure storage |
| Authenticated calls | cookie `access_token` sent automatically | header `Authorization: Bearer <accessToken>` |
| Refresh / logout input | cookie `refresh_token` | body `{ "refreshToken": "..." }` |
| CSRF | required on every non-GET request (header `X-XSRF-TOKEN`) | not applicable |
| Mode selector | none (default) | header `X-Client-Type: mobile` on `/api/auth/**` calls |

Token lifetimes: access token 15 minutes, refresh token 7 days. Every refresh rotates the refresh token. Reusing
an already-used refresh token is treated as theft: all of the user's refresh tokens are revoked.

### Cookies (web mode)

| Cookie | Attributes | Set by |
|---|---|---|
| `access_token` | `HttpOnly; SameSite=Strict; Path=/api; Max-Age=900; Secure` (prod) | register, login, refresh |
| `refresh_token` | `HttpOnly; SameSite=Strict; Path=/api/auth; Max-Age=604800; Secure` (prod) | register, login, refresh |
| `XSRF-TOKEN` | readable by JavaScript; `SameSite=Strict` | any API response when missing, rotated after login/logout |

Logout and a failed refresh clear `access_token` and `refresh_token` (`Max-Age=0`).

### CSRF (web mode)

The web client reads the `XSRF-TOKEN` cookie and sends its value in the `X-XSRF-TOKEN` header on every
`POST`/`PUT`/`DELETE` (axios does this by default for same-origin requests). The client calls `GET /api/auth/me` at
start-up; that response (even a `401`) sets the cookie, so the first `POST /api/auth/login` already has a token.
Requests carrying `Authorization: Bearer` or `X-Client-Type: mobile` are exempt: browsers cannot send custom
headers cross-site without a CORS preflight, and no cross-origin browser access is allowed.

## 2. Endpoints

`🔓` = public, `🔒` = authenticated (web cookie or mobile Bearer).

### `POST /api/auth/register` 🔓

Creates an account and signs the user in.

```json
{ "email": "jane@example.com", "password": "correct-horse-42" }
```

- `email`: required, valid e-mail, ≤ 254 chars; trimmed and lower-cased server-side.
- `password`: required, 8–72 characters.

`201 Created` → `AuthResponse`. Errors: `400 VALIDATION_FAILED`, `409 EMAIL_ALREADY_USED`.

### `POST /api/auth/login` 🔓

```json
{ "email": "jane@example.com", "password": "correct-horse-42" }
```

`200 OK` → `AuthResponse`. Errors: `400 VALIDATION_FAILED`, `401 INVALID_CREDENTIALS` (same response whether the
e-mail is unknown or the password is wrong).

### `POST /api/auth/refresh` 🔓

Web: no body, uses the `refresh_token` cookie. Mobile: `{ "refreshToken": "..." }`.

`200 OK` → `AuthResponse` (new access token, new refresh token). Errors: `401 INVALID_REFRESH_TOKEN` (missing,
unknown, expired, revoked or reused; web cookies are cleared).

### `POST /api/auth/logout` 🔓

Web: no body, uses the `refresh_token` cookie. Mobile: `{ "refreshToken": "..." }`.

`204 No Content`, always (idempotent). Revokes the refresh token when it exists and clears web cookies.

### `GET /api/auth/me` 🔒

`200 OK` → `UserResponse`. Errors: `401 UNAUTHENTICATED`.

### `GET /api/tasks` 🔒

Lists the caller's tasks, newest update first (`updatedAt DESC`, then `id DESC`).

| Query param | Type | Default | Notes |
|---|---|---|---|
| `status` | `TODO` \| `IN_PROGRESS` \| `DONE` | none | exact match |
| `q` | string | none | case-insensitive "contains" on title and description; blank = ignored |
| `page` | int | `0` | values below 0 are clamped to 0 |
| `size` | int | `20` | clamped to `1..100` |

`200 OK` → `PageResponse<TaskResponse>`. Errors: `400 INVALID_PARAMETER` (e.g. unknown `status`).

### `POST /api/tasks` 🔒

```json
{ "title": "Write the README", "description": "Screenshots included", "status": "TODO" }
```

- `title`: required, 1–200 chars after trim.
- `description`: optional, ≤ 2000 chars (`null` or omitted = none).
- `status`: optional, defaults to `TODO`.

`201 Created` → `TaskResponse` (+ `Location: /api/tasks/{id}`). Errors: `400 VALIDATION_FAILED`.

### `PUT /api/tasks/{id}` 🔒

Full replacement of the editable fields.

```json
{ "title": "Write the README", "description": null, "status": "IN_PROGRESS" }
```

- `title`: required (same rules as create). `status`: required. `description`: optional (omitted or `null` clears it).

`200 OK` → `TaskResponse`. Errors: `400 VALIDATION_FAILED`, `404 TASK_NOT_FOUND` (also when the task belongs to
someone else: existence is never disclosed).

### `DELETE /api/tasks/{id}` 🔒

`204 No Content`. Errors: `404 TASK_NOT_FOUND` (same rule as above).

## 3. Schemas

```ts
type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

interface UserResponse { id: number; email: string }

// Web mode: only `user` is present. Mobile mode: all fields are present.
interface AuthResponse {
  user: UserResponse;
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number; // access token lifetime in seconds
}

interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  createdAt: string; // ISO-8601 UTC
  updatedAt: string; // ISO-8601 UTC
}

interface PageResponse<T> {
  content: T[];
  page: number;          // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
}
```

## 4. Errors (RFC 9457)

`Content-Type: application/problem+json`

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed.",
  "instance": "/api/tasks",
  "code": "VALIDATION_FAILED",
  "errors": [{ "field": "title", "message": "must not be blank" }]
}
```

`code` is stable and meant to be branched on; `detail` is a human-readable English sentence; `errors` is present
only for field validation failures.

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | body field constraints violated |
| 400 | `MALFORMED_REQUEST` | unreadable JSON, wrong type, unknown enum value in body |
| 400 | `INVALID_PARAMETER` | invalid query/path parameter |
| 401 | `UNAUTHENTICATED` | missing, invalid or expired access token |
| 401 | `INVALID_CREDENTIALS` | login failed |
| 401 | `INVALID_REFRESH_TOKEN` | refresh failed |
| 403 | `CSRF_TOKEN_INVALID` | web mutation without a valid `X-XSRF-TOKEN` |
| 403 | `FORBIDDEN` | any other access denial |
| 404 | `TASK_NOT_FOUND` | unknown task or task owned by another user |
| 404 | `NOT_FOUND` | unknown route |
| 405 | `METHOD_NOT_ALLOWED` | wrong HTTP method |
| 409 | `EMAIL_ALREADY_USED` | register with an existing e-mail |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | body is not JSON |
| 500 | `INTERNAL_ERROR` | unexpected error (no internal detail is ever leaked) |

## 5. Client guidance

- **Web boot:** `GET /api/auth/me` → `200` authenticated; `401` → `POST /api/auth/refresh` once → `200`
  authenticated, otherwise anonymous.
- **Expired access token:** on a `401` from any non-auth endpoint, call `POST /api/auth/refresh` once (share one
  in-flight refresh between concurrent requests), then replay the original request once. If the refresh fails,
  sign out locally and go to the login screen.
- **Sync between web and mobile:** the API is the single source of truth; clients refetch on focus / resume /
  pull-to-refresh.
