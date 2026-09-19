import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./api-error";

const MAX_QUERY_RETRIES = 2;

/** Client errors (4xx) are deterministic: retrying them only delays the error state. */
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < MAX_QUERY_RETRIES;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      // `refetchOnWindowFocus` keeps its default (on): the API is the single source of truth shared
      // with the mobile app, so coming back to the tab shows the changes made elsewhere.
      queries: { retry: shouldRetry },
      mutations: { retry: false },
    },
  });
}
