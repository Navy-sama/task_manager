import { z } from "zod";

/*
 * Messages are translation keys, translated at render time (see `FieldError`), so a language switch
 * also updates the errors already displayed.
 */

const EMAIL_MAX_LENGTH = 254;
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

const emailSchema = z
  .string()
  .trim()
  .min(1, "validation.emailRequired")
  .max(EMAIL_MAX_LENGTH, "validation.emailMax")
  .pipe(z.email("validation.emailInvalid"));

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "validation.passwordRequired"),
});

export const registerSchema = z
  .object({
    email: emailSchema,
    password: z
      .string()
      .min(1, "validation.passwordRequired")
      .min(PASSWORD_MIN_LENGTH, "validation.passwordMin")
      .max(PASSWORD_MAX_LENGTH, "validation.passwordMax"),
    confirmPassword: z.string().min(1, "validation.confirmPasswordRequired"),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ["confirmPassword"],
    error: "validation.passwordMismatch",
  });

export type LoginFormValues = z.input<typeof loginSchema>;
export type LoginInput = z.output<typeof loginSchema>;
export type RegisterFormValues = z.input<typeof registerSchema>;
export type RegisterInput = z.output<typeof registerSchema>;
