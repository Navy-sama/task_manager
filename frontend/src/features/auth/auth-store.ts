import { create } from "zustand";
import type { UserResponse } from "./types";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthState {
  status: AuthStatus;
  user: UserResponse | null;
  setUser: (user: UserResponse) => void;
  clear: () => void;
}

/**
 * Who is signed in, as far as the UI is concerned. The session itself lives in HttpOnly cookies;
 * this store starts in `loading` until `AuthBootstrap` has asked the API (`GET /auth/me`).
 */
export const useAuthStore = create<AuthState>()((set) => ({
  status: "loading",
  user: null,
  setUser: (user) => set({ status: "authenticated", user }),
  clear: () => set({ status: "unauthenticated", user: null }),
}));
