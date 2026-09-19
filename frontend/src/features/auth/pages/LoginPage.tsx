import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { AuthLayout } from "@/components/layout/AuthLayout";
import { Button } from "@/components/ui/button";
import { LoginForm } from "../components/LoginForm";

export function LoginPage() {
  const { t } = useTranslation();
  return (
    <AuthLayout
      title={t("auth.login.title")}
      description={t("auth.login.subtitle")}
      footer={
        <p className="flex flex-wrap items-center justify-center gap-x-1">
          {t("auth.login.noAccount")}
          <Button asChild variant="link">
            <Link to="/register">{t("auth.login.registerLink")}</Link>
          </Button>
        </p>
      }
    >
      <LoginForm />
    </AuthLayout>
  );
}
