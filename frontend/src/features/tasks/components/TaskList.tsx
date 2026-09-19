import { useTranslation } from "react-i18next";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { TaskResponse } from "../types";
import { TaskRow } from "./TaskRow";

interface TaskListProps {
  tasks: TaskResponse[];
  dateFormatter: Intl.DateTimeFormat;
  /** True while the previous result stays on screen during a page/filter change. */
  isStale: boolean;
  onEdit: (task: TaskResponse) => void;
  onDelete: (task: TaskResponse) => void;
}

export function TaskList({ tasks, dateFormatter, isStale, onEdit, onDelete }: TaskListProps) {
  const { t } = useTranslation();
  return (
    <ul
      aria-label={t("tasks.heading")}
      aria-busy={isStale}
      className={cn("grid gap-3 transition-opacity duration-150", isStale && "opacity-60")}
    >
      {tasks.map((task) => (
        <TaskRow
          key={task.id}
          task={task}
          dateFormatter={dateFormatter}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </ul>
  );
}

const SKELETON_ROWS = 4;

export function TaskListSkeleton() {
  const { t } = useTranslation();
  return (
    <div role="status" className="grid gap-3">
      <span className="sr-only">{t("tasks.loading")}</span>
      {Array.from({ length: SKELETON_ROWS }, (_, index) => (
        <div
          key={index}
          className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 md:flex-row md:items-center sm:px-5"
        >
          <div className="grid flex-1 gap-2">
            <Skeleton className="h-5 w-2/5" />
            <Skeleton className="h-4 w-4/5" />
            <Skeleton className="h-3 w-32" />
          </div>
          <Skeleton className="h-8 w-28 rounded-full" />
        </div>
      ))}
    </div>
  );
}
