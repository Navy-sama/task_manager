import type { TFunction } from "i18next";
import { toApiError } from "@/lib/api-error";

/**
 * User-facing message for any error thrown by an API call: the translation of the stable error
 * `code` when there is one, otherwise the server's `detail`, otherwise a generic message.
 */
export function getErrorMessage(t: TFunction, error: unknown): string {
  const apiError = toApiError(error);
  const fallback = apiError.detail.trim() || t("errors.generic");
  return t(`errors.codes.${apiError.code}`, { defaultValue: fallback });
}

/**
 * Validation messages are translation keys (see the zod schemas); server-side field messages are
 * plain English sentences and are shown as they are.
 */
export function translateValidationMessage(t: TFunction, message: string): string {
  return t(message, { defaultValue: message });
}
