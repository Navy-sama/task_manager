import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

export default defineConfig(({ mode }) => {
  // A value exported in the shell wins over the `.env*` files.
  const env = loadEnv(mode, import.meta.dirname, "VITE_");
  const apiProxyTarget =
    process.env.VITE_API_PROXY_TARGET ?? env.VITE_API_PROXY_TARGET ?? "http://localhost:8080";

  // The browser only ever talks to its own origin: `/api` is forwarded to the backend so the
  // auth cookies (SameSite=Strict, Path=/api) and the CSRF cookie stay first-party.
  const proxy = {
    "/api": { target: apiProxyTarget, changeOrigin: false },
  };

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: { "@": path.resolve(import.meta.dirname, "src") },
    },
    server: { port: 5173, strictPort: true, proxy },
    preview: { port: 4173, strictPort: true, proxy },
    test: {
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      include: ["src/**/*.test.{ts,tsx}"],
      css: false,
      restoreMocks: true,
    },
  };
});
