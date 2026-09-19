import { QueryClient } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router";
import { AppProviders } from "@/app/providers";
import { AppRoutes } from "@/app/router";
import { AuthBootstrap } from "@/features/auth/components/AuthBootstrap";
import { LocationProbe } from "./LocationProbe";

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
}

interface RenderOptions {
  route?: string;
  queryClient?: QueryClient;
}

export function renderWithProviders(
  ui: ReactElement,
  { route = "/", queryClient = createTestQueryClient() }: RenderOptions = {},
) {
  const user = userEvent.setup();
  const result = render(
    <MemoryRouter initialEntries={[route]}>
      <AppProviders queryClient={queryClient}>
        {ui}
        <LocationProbe />
      </AppProviders>
    </MemoryRouter>,
  );
  return { user, queryClient, ...result };
}

/** The whole app (session bootstrap + routes) at `route`. */
export function renderApp(route = "/", options: Omit<RenderOptions, "route"> = {}) {
  return renderWithProviders(
    <AuthBootstrap>
      <AppRoutes />
    </AuthBootstrap>,
    { ...options, route },
  );
}
