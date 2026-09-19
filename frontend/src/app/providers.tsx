import { QueryClientProvider, type QueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { Toaster } from "@/components/ui/sonner";

interface AppProvidersProps {
  queryClient: QueryClient;
  children: ReactNode;
}

/** Everything but the router, so tests can pair these providers with a `MemoryRouter`. */
export function AppProviders({ queryClient, children }: AppProvidersProps) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster />
    </QueryClientProvider>
  );
}
