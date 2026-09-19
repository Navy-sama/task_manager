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
import { useRegister } from "../hooks";
import { registerSchema, type RegisterFormValues, type RegisterInput } from "../schemas";

const FIELDS = ["email", "password"] as const;

export function RegisterForm() {
  const { t } = useTranslation();
  const id = useId();
  const registerAccount = useRegister();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues, unknown, RegisterInput>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: "", password: "", confirmPassword: "" },
    mode: "onTouched",
  });

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setFormError(null);
    try {
      await registerAccount.mutateAsync({ email, password });
      toast.success(t("auth.register.success"));
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

  const pending = isSubmitting || registerAccount.isPending;
  const emailErrorId = `${id}-email-error`;
  const passwordHintId = `${id}-password-hint`;
  const passwordErrorId = `${id}-password-error`;
  const confirmErrorId = `${id}-confirm-error`;

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
          autoComplete="new-password"
          aria-invalid={errors.password ? true : undefined}
          aria-describedby={`${passwordHintId} ${passwordErrorId}`}
          {...register("password")}
        />
        <p id={passwordHintId} className="text-xs text-muted-foreground">
          {t("auth.register.passwordHint")}
        </p>
        <FieldError id={passwordErrorId} message={errors.password?.message} />
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-confirm`}>{t("auth.confirmPassword")}</Label>
        <PasswordInput
          id={`${id}-confirm`}
          autoComplete="new-password"
          aria-invalid={errors.confirmPassword ? true : undefined}
          aria-describedby={confirmErrorId}
          {...register("confirmPassword")}
        />
        <FieldError id={confirmErrorId} message={errors.confirmPassword?.message} />
      </div>

      <Button type="submit" className="w-full" disabled={pending}>
        {pending ? (
          <>
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
            {t("auth.register.submitting")}
          </>
        ) : (
          t("auth.register.submit")
        )}
      </Button>
    </form>
  );
}
