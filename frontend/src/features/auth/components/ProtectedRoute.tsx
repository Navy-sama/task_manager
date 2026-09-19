import { Navigate, Outlet, useLocation } from "react-router";
import { FullPageSpinner } from "@/components/FullPageSpinner";
import { useAuthStore } from "../auth-store";

/** Layout route for signed-in screens: anonymous visitors are sent to `/login`. */
export function ProtectedRoute() {
  const status = useAuthStore((state) => state.status);
  const location = useLocation();

  if (status === "loading") {
    return <FullPageSpinner />;
  }
  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
