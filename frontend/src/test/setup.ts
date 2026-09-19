import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { i18n } from "@/i18n";
import { setSessionExpiredHandler } from "@/lib/api-client";
import { taskDb } from "./handlers";
import { server } from "./server";

/*
 * jsdom lacks a few browser APIs that Radix primitives rely on (pointer capture, element scrolling,
 * size observation). Minimal stand-ins are enough since tests never assert on layout.
 */
class ResizeObserverStub {
  observe(): void {
    /* no layout in jsdom */
  }
  unobserve(): void {
    /* no layout in jsdom */
  }
  disconnect(): void {
    /* no layout in jsdom */
  }
}

function polyfill(target: object, name: string, value: unknown): void {
  if (!(name in target)) {
    Object.defineProperty(target, name, { value, configurable: true, writable: true });
  }
}

polyfill(globalThis, "ResizeObserver", ResizeObserverStub);
polyfill(Element.prototype, "hasPointerCapture", () => false);
polyfill(Element.prototype, "releasePointerCapture", () => undefined);
polyfill(Element.prototype, "scrollIntoView", () => undefined);
polyfill(window, "matchMedia", (query: string) => ({
  matches: false,
  media: query,
  onchange: null,
  addEventListener: () => undefined,
  removeEventListener: () => undefined,
  addListener: () => undefined,
  removeListener: () => undefined,
  dispatchEvent: () => false,
}));

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));

afterEach(async () => {
  cleanup();
  server.resetHandlers();
  taskDb.reset();
  useAuthStore.setState({ status: "loading", user: null });
  setSessionExpiredHandler(null);
  window.localStorage.clear();
  await i18n.changeLanguage("en");
});

afterAll(() => server.close());
