import type { TFunction } from "i18next";
import { toast } from "sonner";
import { getErrorMessage } from "@/i18n/error-message";
import { toApiError } from "@/lib/api-error";

/**
 * Error toast for a task mutation. `TASK_NOT_FOUND` gets a dedicated message: the task was removed
 * elsewhere (another tab, the mobile app) and the list is being refetched by the mutation hooks.
 * Returns `true` for that case so callers can close the dialog showing the missing task.
 */
export function notifyTaskMutationError(t: TFunction, error: unknown): boolean {
  const apiError = toApiError(error);
  if (apiError.code === "TASK_NOT_FOUND") {
    toast.error(t("tasks.toast.notFound"));
    return true;
  }
  toast.error(getErrorMessage(t, apiError));
  return false;
}
