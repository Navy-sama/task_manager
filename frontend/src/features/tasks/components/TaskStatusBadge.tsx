import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { STATUS_META } from "../status";
import type { TaskStatus } from "../types";

interface TaskStatusBadgeProps {
  status: TaskStatus;
  className?: string;
  /** Extra content after the label (e.g. a chevron when the badge opens a menu). */
  children?: ReactNode;
}

export function TaskStatusBadge({ status, className, children }: TaskStatusBadgeProps) {
  const { t } = useTranslation();
  const { icon: Icon, variant } = STATUS_META[status];
  return (
    <Badge variant={variant} className={cn(className)}>
      <Icon aria-hidden="true" />
      {t(`tasks.status.${status}`)}
      {children}
    </Badge>
  );
}
