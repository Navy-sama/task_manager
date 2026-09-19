import { useEffect, useRef, type ReactNode } from "react";
import { authApi } from "../api";
import { useAuthStore } from "../auth-store";
import { useSessionExpiredHandler } from "../hooks";

/**
 * Resolves the session once at start-up: `GET /auth/me` answers `200` for a signed-in user. An
 * expired access token is transparently refreshed by the API client; any other failure means the
 * visitor is anonymous. The call also makes the API set the `XSRF-TOKEN` cookie that the first
 * mutation (login, register) needs.
 */
export function AuthBootstrap({ children }: { children: ReactNode }) {
  useSessionExpiredHandler();
  const setUser = useAuthStore((state) => state.setUser);
  const clear = useAuthStore((state) => state.clear);
  // Guards against the double effect run of React's StrictMode: the session is resolved once.
  const started = useRef(false);

  useEffect(() => {
    if (started.current) {
      return;
    }
    started.current = true;
    authApi.me().then(setUser, clear);
  }, [setUser, clear]);

  return children;
}
