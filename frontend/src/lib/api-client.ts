import axios, { isAxiosError, isCancel, type InternalAxiosRequestConfig } from "axios";
import { NETWORK_ERROR, toApiError } from "./api-error";

/**
 * Single HTTP client for the whole app. Authentication relies on HttpOnly cookies set by the API
 * (JavaScript never sees a token) and CSRF uses the double-submit cookie pattern: axios copies the
 * `XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header. Axios only does that for same-origin URLs,
 * which is always the case here because `/api` is served from the web app's own origin (Vite proxy
 * in development, reverse proxy in production), so `withXSRFToken` keeps its safe default.
 */
export const apiClient = axios.create({
  baseURL: "/api",
  withCredentials: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: { Accept: "application/json, application/problem+json" },
});

type SessionExpiredHandler = () => void;

let onSessionExpired: SessionExpiredHandler | null = null;

/**
 * Registered by the auth feature, so that `lib/` never imports `features/`. Called when a request
 * failed with `401` and the session could not be refreshed.
 */
export function setSessionExpiredHandler(handler: SessionExpiredHandler | null): void {
  onSessionExpired = handler;
}

/**
 * Auth flow endpoints: a `401` from them is a business answer (wrong password, dead refresh token),
 * never a reason to refresh. `/auth/me` is deliberately absent: the boot sequence relies on it being
 * refreshed like any other protected endpoint (docs/api-contract.md §5).
 */
const NON_REFRESHABLE_ENDPOINTS = [
  "/auth/login",
  "/auth/register",
  "/auth/refresh",
  "/auth/logout",
];

function isRefreshable(url: string | undefined): boolean {
  return !NON_REFRESHABLE_ENDPOINTS.some((endpoint) => url?.startsWith(endpoint));
}

interface ReplayableRequestConfig extends InternalAxiosRequestConfig {
  /** Set on the replay of a request that failed with `401`, so it is never replayed twice. */
  isReplay?: boolean;
}

let refreshInFlight: Promise<void> | null = null;

/** Single-flight refresh: concurrent `401`s share one `POST /auth/refresh`. */
function refreshSession(): Promise<void> {
  refreshInFlight ??= apiClient
    .post("/auth/refresh")
    .then(() => undefined)
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

apiClient.interceptors.response.use(undefined, async (error: unknown) => {
  if (isCancel(error)) {
    throw error;
  }

  const apiError = toApiError(error);
  const config: ReplayableRequestConfig | undefined = isAxiosError(error)
    ? error.config
    : undefined;

  if (apiError.status !== 401 || !config || config.isReplay || !isRefreshable(config.url)) {
    throw apiError;
  }

  try {
    await refreshSession();
  } catch (refreshError: unknown) {
    const refreshApiError = toApiError(refreshError);
    // An unreachable server says nothing about the session: keep it and surface the outage.
    if (refreshApiError.code === NETWORK_ERROR) {
      throw refreshApiError;
    }
    onSessionExpired?.();
    throw apiError;
  }

  const replay: ReplayableRequestConfig = { ...config, isReplay: true };
  return apiClient.request(replay);
});
