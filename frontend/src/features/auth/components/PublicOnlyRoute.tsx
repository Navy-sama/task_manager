import { Navigate, Outlet, useLocation } from "react-router";
import { FullPageSpinner } from "@/components/FullPageSpinner";
import { useAuthStore } from "../auth-store";

/**
 * Where to go once signed in: back to the page that required authentication (set by
 * `ProtectedRoute`), restricted to same-app paths, otherwise home.
 */
function redirectTarget(state: unknown): string {
  if (typeof state !== "object" || state === null || !("from" in state)) {
    return "/";
  }
  const from = state.from;
  if (typeof from !== "object" || from === null || !("pathname" in from)) {
    return "/";
  }
  const { pathname } = from;
  const search = "search" in from && typeof from.search === "string" ? from.search : "";
  if (typeof pathname !== "string" || !pathname.startsWith("/") || pathname.startsWith("//")) {
    return "/";
  }
  return pathname + search;
}

/** Layout route for the login and register screens: signed-in users are sent to the app. */
export function PublicOnlyRoute() {
  const status = useAuthStore((state) => state.status);
  const location = useLocation();

  if (status === "loading") {
    return <FullPageSpinner />;
  }
  if (status === "authenticated") {
    return <Navigate to={redirectTarget(location.state)} replace />;
  }
  return <Outlet />;
}
