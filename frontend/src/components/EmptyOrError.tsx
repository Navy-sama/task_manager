import { InboxIcon, RefreshCwIcon, TriangleAlertIcon } from "lucide-react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { getErrorMessage } from "@/i18n/error-message";
import { cn } from "@/lib/utils";

interface EmptyOrErrorProps {
  /** When set, the error state wins over the empty state. */
  error?: unknown;
  onRetry?: () => void;
  errorTitle?: string;
  isEmpty?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
}

/**
 * The one way list screens tell "nothing here" apart from "something failed", so both states read
 * the same everywhere. Renders nothing when there is neither an error nor an empty result.
 */
export function EmptyOrError({
  error,
  onRetry,
  errorTitle,
  isEmpty = false,
  emptyTitle,
  emptyDescription,
  emptyAction,
}: EmptyOrErrorProps) {
  const { t } = useTranslation();

  if (error) {
    return (
      <StateBlock
        role="alert"
        tone="danger"
        icon={<TriangleAlertIcon aria-hidden="true" />}
        title={errorTitle ?? t("errors.generic")}
        description={getErrorMessage(t, error)}
        action={
          onRetry ? (
            <Button variant="outline" onClick={onRetry}>
              <RefreshCwIcon aria-hidden="true" />
              {t("common.retry")}
            </Button>
          ) : null
        }
      />
    );
  }

  if (isEmpty) {
    return (
      <StateBlock
        role="status"
        tone="neutral"
        icon={<InboxIcon aria-hidden="true" />}
        title={emptyTitle ?? ""}
        description={emptyDescription}
        action={emptyAction}
      />
    );
  }

  return null;
}

interface StateBlockProps {
  role: "alert" | "status";
  tone: "neutral" | "danger";
  icon: ReactNode;
  title: string;
  description?: string | undefined;
  action?: ReactNode;
}

function StateBlock({ role, tone, icon, title, description, action }: StateBlockProps) {
  return (
    <div
      role={role}
      className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-input bg-card px-6 py-12 text-center"
    >
      <span
        className={cn(
          "flex size-11 items-center justify-center rounded-full [&_svg]:size-5",
          tone === "danger"
            ? "bg-destructive-soft text-destructive"
            : "bg-accent text-accent-foreground",
        )}
      >
        {icon}
      </span>
      <div className="grid max-w-sm gap-1">
        <p className="text-base font-semibold text-foreground">{title}</p>
        {description ? <p className="text-sm text-muted-foreground">{description}</p> : null}
      </div>
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  );
}
