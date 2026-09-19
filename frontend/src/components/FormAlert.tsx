import { CircleAlertIcon } from "lucide-react";
import type { ReactNode } from "react";

/** Form-level error (as opposed to a field error), announced as soon as it appears. */
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive-soft px-3 py-2.5 text-sm text-destructive"
    >
      <CircleAlertIcon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <p>{children}</p>
    </div>
  );
}
