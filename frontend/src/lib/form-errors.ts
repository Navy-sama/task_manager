import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { ApiError } from "./api-error";

/**
 * Copies the server-side field errors of a `VALIDATION_FAILED` problem onto the matching form
 * fields. Returns `true` when at least one error could be attached to a field of the form.
 */
export function applyServerFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  let applied = false;
  for (const fieldError of error.errors ?? []) {
    const field = fields.find((name) => name === fieldError.field);
    if (field) {
      setError(field, { type: "server", message: fieldError.message }, { shouldFocus: !applied });
      applied = true;
    }
  }
  return applied;
}
