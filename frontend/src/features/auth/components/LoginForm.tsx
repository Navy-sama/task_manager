import { zodResolver } from "@hookform/resolvers/zod";
import { LoaderCircleIcon } from "lucide-react";
import { useId, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { FieldError } from "@/components/FieldError";
import { FormAlert } from "@/components/FormAlert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Label } from "@/components/ui/label";
import { getErrorMessage } from "@/i18n/error-message";
import { toApiError } from "@/lib/api-error";
import { applyServerFieldErrors } from "@/lib/form-errors";
import { useLogin } from "../hooks";
import { loginSchema, type LoginFormValues, type LoginInput } from "../schemas";

const FIELDS = ["email", "password"] as const;

export function LoginForm() {
  const { t } = useTranslation();
  const id = useId();
  const login = useLogin();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues, unknown, LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
    mode: "onTouched",
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await login.mutateAsync(values);
    } catch (error) {
      const apiError = toApiError(error);
      if (
        apiError.code === "VALIDATION_FAILED" &&
        applyServerFieldErrors(apiError, setError, FIELDS)
      ) {
        return;
      }
      const message = getErrorMessage(t, apiError);
      setFormError(message);
      toast.error(message);
    }
  });

  const pending = isSubmitting || login.isPending;
  const emailErrorId = `${id}-email-error`;
  const passwordErrorId = `${id}-password-error`;

  return (
    <form noValidate onSubmit={onSubmit} className="grid gap-5" aria-busy={pending}>
      {formError ? <FormAlert>{formError}</FormAlert> : null}

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-email`}>{t("auth.email")}</Label>
        <Input
          id={`${id}-email`}
          type="email"
          autoComplete="email"
          inputMode="email"
          placeholder={t("auth.emailPlaceholder")}
          aria-invalid={errors.email ? true : undefined}
          aria-describedby={emailErrorId}
          {...register("email")}
        />
        <FieldError id={emailErrorId} message={errors.email?.message} />
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-password`}>{t("auth.password")}</Label>
        <PasswordInput
          id={`${id}-password`}
          autoComplete="current-password"
          aria-invalid={errors.password ? true : undefined}
          aria-describedby={passwordErrorId}
          {...register("password")}
        />
        <FieldError id={passwordErrorId} message={errors.password?.message} />
      </div>

      <Button type="submit" className="w-full" disabled={pending}>
        {pending ? (
          <>
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
            {t("auth.login.submitting")}
          </>
        ) : (
          t("auth.login.submit")
        )}
      </Button>
    </form>
  );
}
