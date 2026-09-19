import { describe, expect, it } from "vitest";
import { useAuthStore } from "./auth-store";

describe("useAuthStore", () => {
  it("starts in the loading state", () => {
    expect(useAuthStore.getState()).toMatchObject({ status: "loading", user: null });
  });

  it("becomes authenticated with a user, then unauthenticated once cleared", () => {
    const user = { id: 7, email: "jane@example.com" };

    useAuthStore.getState().setUser(user);
    expect(useAuthStore.getState()).toMatchObject({ status: "authenticated", user });

    useAuthStore.getState().clear();
    expect(useAuthStore.getState()).toMatchObject({ status: "unauthenticated", user: null });
  });
});
