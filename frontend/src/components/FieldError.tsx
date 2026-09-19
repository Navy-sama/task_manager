import { useTranslation } from "react-i18next";
import { translateValidationMessage } from "@/i18n/error-message";

interface FieldErrorProps {
  /** Referenced by the control's `aria-describedby`. */
  id: string;
  /** A translation key (client-side validation) or a server sentence. */
  message?: string | undefined;
}

/**
 * Error line under a form control. The element is always rendered so that it acts as a polite live
 * region: errors appearing on blur are announced by screen readers too.
 */
export function FieldError({ id, message }: FieldErrorProps) {
  const { t } = useTranslation();
  return (
    <p id={id} aria-live="polite" className="text-sm text-destructive">
      {message ? translateValidationMessage(t, message) : null}
    </p>
  );
}
