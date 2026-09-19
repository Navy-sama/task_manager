import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { setSessionExpiredHandler } from "@/lib/api-client";
import { authApi } from "./api";
import { useAuthStore } from "./auth-store";

/** Signs the user in; the API answers with the user and sets the session cookies. */
export function useLogin() {
  const queryClient = useQueryClient();
  const setUser = useAuthStore((state) => state.setUser);
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: ({ user }) => {
      // Never show data cached for a previous session.
      queryClient.removeQueries();
      setUser(user);
    },
  });
}

/** Creates the account and signs the user in (the API returns an `AuthResponse`). */
export function useRegister() {
  const queryClient = useQueryClient();
  const setUser = useAuthStore((state) => state.setUser);
  return useMutation({
    mutationFn: authApi.register,
    onSuccess: ({ user }) => {
      queryClient.removeQueries();
      setUser(user);
    },
  });
}

/**
 * Signs out on the server, then locally whatever the outcome: the user asked to leave, and the
 * endpoint is idempotent anyway. Navigation happens here rather than in the calling component,
 * which is unmounted as soon as the store is cleared.
 */
export function useLogout() {
  const queryClient = useQueryClient();
  const clear = useAuthStore((state) => state.clear);
  const navigate = useNavigate();
  const { t } = useTranslation();
  return useMutation({
    mutationFn: authApi.logout,
    onSettled: async () => {
      clear();
      queryClient.removeQueries();
      toast.success(t("auth.loggedOut"));
      await navigate("/login", { replace: true });
    },
  });
}

/**
 * Registers what the API client does when the session cannot be refreshed any more: sign out
 * locally and send the user back to the login screen. Visitors who were never signed in (boot
 * sequence) are just marked anonymous, without a misleading "session expired" message.
 */
export function useSessionExpiredHandler(): void {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    setSessionExpiredHandler(() => {
      const { status, clear } = useAuthStore.getState();
      clear();
      queryClient.removeQueries();
      if (status === "authenticated") {
        toast.error(t("auth.sessionExpired"));
        void navigate("/login", { replace: true });
      }
    });
    return () => setSessionExpiredHandler(null);
  }, [navigate, queryClient, t]);
}
