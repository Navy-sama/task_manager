import { ListChecksIcon, LogOutIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, Outlet } from "react-router";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Button } from "@/components/ui/button";
import { focusRing } from "@/components/ui/variants";
import { useAuthStore } from "@/features/auth/auth-store";
import { useLogout } from "@/features/auth/hooks";
import { cn } from "@/lib/utils";

/** Shell of the signed-in screens: header (brand, user, language, sign out) and main content. */
export function AppLayout() {
  const { t } = useTranslation();
  const email = useAuthStore((state) => state.user?.email);
  const logout = useLogout();

  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#main"
        className={cn(
          "sr-only z-50 rounded-md bg-card px-3 py-2 text-sm font-medium focus:not-sr-only focus:fixed focus:top-3 focus:left-3",
          focusRing,
        )}
      >
        {t("app.skipToContent")}
      </a>
      <header className="sticky top-0 z-40 border-b border-border bg-card/90 backdrop-blur supports-[backdrop-filter]:bg-card/75">
        <div className="mx-auto flex h-16 w-full max-w-5xl items-center gap-3 px-4 sm:px-6">
          <Link
            to="/"
            className={cn("flex items-center gap-2 rounded-md font-semibold", focusRing)}
          >
            <span className="grid size-8 place-items-center rounded-lg bg-primary text-primary-foreground">
              <ListChecksIcon className="size-4" aria-hidden="true" />
            </span>
            <span className="hidden sm:inline">{t("app.name")}</span>
          </Link>
          <div className="ml-auto flex min-w-0 items-center gap-2 sm:gap-3">
            {email ? (
              <span className="hidden max-w-56 truncate text-sm text-muted-foreground md:inline">
                <span className="sr-only">{t("auth.signedInAs", { email })}</span>
                <span aria-hidden="true" title={email}>
                  {email}
                </span>
              </span>
            ) : null}
            <LanguageSwitcher />
            <Button
              variant="ghost"
              size="sm"
              onClick={() => logout.mutate()}
              disabled={logout.isPending}
            >
              <LogOutIcon aria-hidden="true" />
              <span className="sr-only sm:not-sr-only">{t("auth.logout")}</span>
            </Button>
          </div>
        </div>
      </header>
      <main
        id="main"
        tabIndex={-1}
        className="mx-auto w-full max-w-5xl flex-1 px-4 py-6 sm:px-6 sm:py-8"
      >
        <Outlet />
      </main>
    </div>
  );
}
