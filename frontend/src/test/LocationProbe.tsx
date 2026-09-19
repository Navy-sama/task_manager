import { useLocation } from "react-router";

/** Exposes the current location so tests can assert on redirects and search params. */
export function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname + location.search}</output>;
}
