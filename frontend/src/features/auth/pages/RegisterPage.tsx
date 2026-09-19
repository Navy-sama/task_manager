import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { AuthLayout } from "@/components/layout/AuthLayout";
import { Button } from "@/components/ui/button";
import { RegisterForm } from "../components/RegisterForm";

export function RegisterPage() {
  const { t } = useTranslation();
  return (
    <AuthLayout
      title={t("auth.register.title")}
      description={t("auth.register.subtitle")}
      footer={
        <p className="flex flex-wrap items-center justify-center gap-x-1">
          {t("auth.register.hasAccount")}
          <Button asChild variant="link">
            <Link to="/login">{t("auth.register.loginLink")}</Link>
          </Button>
        </p>
      }
    >
      <RegisterForm />
    </AuthLayout>
  );
}
