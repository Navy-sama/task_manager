import { ChevronDownIcon, PencilIcon, Trash2Icon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { focusRing } from "@/components/ui/variants";
import { cn } from "@/lib/utils";
import { useUpdateTask } from "../hooks";
import { notifyTaskMutationError } from "../notify";
import { isTaskStatus } from "../schemas";
import { STATUS_META } from "../status";
import { TASK_STATUSES, type TaskResponse, type TaskStatus } from "../types";
import { TaskStatusBadge } from "./TaskStatusBadge";

interface TaskRowProps {
  task: TaskResponse;
  dateFormatter: Intl.DateTimeFormat;
  onEdit: (task: TaskResponse) => void;
  onDelete: (task: TaskResponse) => void;
}

/** A card on small screens, a row from `md` up. */
export function TaskRow({ task, dateFormatter, onEdit, onDelete }: TaskRowProps) {
  const { t } = useTranslation();
  const updateTask = useUpdateTask();
  // While the change is in flight, show the requested status rather than the stale one.
  const displayedStatus = updateTask.isPending ? updateTask.variables.input.status : task.status;

  const changeStatus = (status: TaskStatus) => {
    if (status === task.status) {
      return;
    }
    updateTask.mutate(
      { id: task.id, input: { title: task.title, description: task.description, status } },
      {
        onSuccess: () =>
          toast.success(t("tasks.toast.statusChanged", { status: t(`tasks.status.${status}`) })),
        onError: (error) => notifyTaskMutationError(t, error),
      },
    );
  };

  return (
    <li className="rounded-xl border border-border bg-card p-4 shadow-xs transition-colors duration-150 hover:border-input sm:px-5">
      <article className="flex flex-col gap-3 md:flex-row md:items-center md:gap-6">
        <div className="grid min-w-0 flex-1 gap-1">
          <h3 className="truncate text-base font-semibold text-foreground">{task.title}</h3>
          {task.description ? (
            <p className="line-clamp-2 text-sm break-words text-muted-foreground">
              {task.description}
            </p>
          ) : (
            <p className="text-sm text-muted-foreground italic">{t("tasks.noDescription")}</p>
          )}
          <p className="text-xs text-muted-foreground">
            <time dateTime={task.updatedAt}>
              {t("tasks.updatedAt", { date: dateFormatter.format(new Date(task.updatedAt)) })}
            </time>
          </p>
        </div>

        <div className="flex items-center gap-2 md:shrink-0">
          <DropdownMenu>
            <DropdownMenuTrigger
              disabled={updateTask.isPending}
              className={cn(
                "inline-flex min-h-11 items-center rounded-full disabled:opacity-60 md:min-h-9",
                focusRing,
              )}
            >
              <TaskStatusBadge status={displayedStatus} className="px-3 py-1 text-sm">
                <ChevronDownIcon aria-hidden="true" />
              </TaskStatusBadge>
              <span className="sr-only">
                {t("tasks.actions.changeStatus", { title: task.title })}
              </span>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
              <DropdownMenuLabel>{t("tasks.form.status")}</DropdownMenuLabel>
              <DropdownMenuRadioGroup
                value={displayedStatus}
                onValueChange={(value) => {
                  if (isTaskStatus(value)) changeStatus(value);
                }}
              >
                {TASK_STATUSES.map((status) => {
                  const Icon = STATUS_META[status].icon;
                  return (
                    <DropdownMenuRadioItem key={status} value={status}>
                      <Icon className="size-4 text-muted-foreground" aria-hidden="true" />
                      {t(`tasks.status.${status}`)}
                    </DropdownMenuRadioItem>
                  );
                })}
              </DropdownMenuRadioGroup>
            </DropdownMenuContent>
          </DropdownMenu>

          <div className="ml-auto flex items-center gap-1">
            <Button
              variant="ghost"
              size="icon"
              aria-label={t("tasks.actions.edit", { title: task.title })}
              onClick={() => onEdit(task)}
            >
              <PencilIcon aria-hidden="true" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              aria-label={t("tasks.actions.delete", { title: task.title })}
              className="text-muted-foreground hover:bg-destructive-soft hover:text-destructive"
              onClick={() => onDelete(task)}
            >
              <Trash2Icon aria-hidden="true" />
            </Button>
          </div>
        </div>
      </article>
    </li>
  );
}
