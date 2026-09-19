import { BrowserRouter } from "react-router";
import { AuthBootstrap } from "@/features/auth/components/AuthBootstrap";
import { createQueryClient } from "@/lib/query-client";
import { AppProviders } from "./providers";
import { AppRoutes } from "./router";

const queryClient = createQueryClient();

export function App() {
  return (
    <BrowserRouter>
      <AppProviders queryClient={queryClient}>
        <AuthBootstrap>
          <AppRoutes />
        </AuthBootstrap>
      </AppProviders>
    </BrowserRouter>
  );
}
