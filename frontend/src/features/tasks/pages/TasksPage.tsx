import { FunnelXIcon, PlusIcon } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams, type NavigateOptions } from "react-router";
import { EmptyOrError } from "@/components/EmptyOrError";
import { Button } from "@/components/ui/button";
import { DeleteTaskDialog } from "../components/DeleteTaskDialog";
import { Pagination } from "../components/Pagination";
import { TaskFilters } from "../components/TaskFilters";
import { TaskFormDialog } from "../components/TaskFormDialog";
import { TaskList, TaskListSkeleton } from "../components/TaskList";
import {
  hasActiveFilters,
  readTaskFilters,
  toListParams,
  writeTaskFilters,
  type TaskFilters as Filters,
} from "../filters";
import { useTasks } from "../hooks";
import type { TaskResponse, TaskStatus } from "../types";

interface DialogState {
  open: boolean;
  /** Kept after closing so the content does not change during the closing animation. */
  task: TaskResponse | null;
}

const CLOSED: DialogState = { open: false, task: null };

export function TasksPage() {
  const { t, i18n } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = readTaskFilters(searchParams);
  const tasksQuery = useTasks(toListParams(filters));
  const { data, error, isPending, isPlaceholderData, refetch } = tasksQuery;

  const [formDialog, setFormDialog] = useState<DialogState>(CLOSED);
  const [deleteDialog, setDeleteDialog] = useState<DialogState>(CLOSED);

  const updateFilters = useCallback(
    (patch: Partial<Filters>, options?: NavigateOptions) => {
      setSearchParams((current) => writeTaskFilters(current, patch), options);
    },
    [setSearchParams],
  );

  const onStatusChange = useCallback(
    (status: TaskStatus | undefined) => updateFilters({ status, page: 1 }),
    [updateFilters],
  );
  // Typing must not flood the history: the search replaces the current entry.
  const onSearchChange = useCallback(
    (q: string) => updateFilters({ q, page: 1 }, { replace: true }),
    [updateFilters],
  );
  const clearFilters = () => updateFilters({ status: undefined, q: "", page: 1 });

  // A page beyond the end (last task of the last page deleted, stale link): go to the last page.
  const totalPages = data?.totalPages;
  useEffect(() => {
    if (
      !isPlaceholderData &&
      totalPages !== undefined &&
      totalPages > 0 &&
      filters.page > totalPages
    ) {
      updateFilters({ page: totalPages }, { replace: true });
    }
  }, [filters.page, isPlaceholderData, totalPages, updateFilters]);

  const dateFormatter = useMemo(
    () => new Intl.DateTimeFormat(i18n.language, { dateStyle: "medium", timeStyle: "short" }),
    [i18n.language],
  );

  const openCreate = () => setFormDialog({ open: true, task: null });
  const openEdit = (task: TaskResponse) => setFormDialog({ open: true, task });
  const openDelete = (task: TaskResponse) => setDeleteDialog({ open: true, task });
  const filtered = hasActiveFilters(filters);

  let content;
  if (isPending) {
    content = <TaskListSkeleton />;
  } else if (!data) {
    content = (
      <EmptyOrError
        error={error}
        errorTitle={t("tasks.error.title")}
        onRetry={() => void refetch()}
      />
    );
  } else if (data.content.length === 0) {
    content = filtered ? (
      <EmptyOrError
        isEmpty
        emptyTitle={t("tasks.empty.filteredTitle")}
        emptyDescription={t("tasks.empty.filteredDescription")}
        emptyAction={
          <Button variant="outline" onClick={clearFilters}>
            <FunnelXIcon aria-hidden="true" />
            {t("tasks.filters.clear")}
          </Button>
        }
      />
    ) : (
      <EmptyOrError
        isEmpty
        emptyTitle={t("tasks.empty.title")}
        emptyDescription={t("tasks.empty.description")}
        emptyAction={
          <Button onClick={openCreate}>
            <PlusIcon aria-hidden="true" />
            {t("tasks.new")}
          </Button>
        }
      />
    );
  } else {
    content = (
      <TaskList
        tasks={data.content}
        dateFormatter={dateFormatter}
        isStale={isPlaceholderData}
        onEdit={openEdit}
        onDelete={openDelete}
      />
    );
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="grid gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">{t("tasks.heading")}</h1>
          <p className="text-sm text-muted-foreground">{t("tasks.subheading")}</p>
        </div>
        <Button onClick={openCreate} className="w-full sm:w-auto">
          <PlusIcon aria-hidden="true" />
          {t("tasks.new")}
        </Button>
      </div>

      <TaskFilters
        status={filters.status}
        q={filters.q}
        onStatusChange={onStatusChange}
        onSearchChange={onSearchChange}
      />

      <section className="grid gap-3">
        <p className="text-sm text-muted-foreground" aria-live="polite">
          {data && data.totalElements > 0 ? t("tasks.count", { count: data.totalElements }) : null}
        </p>
        {content}
      </section>

      {data && data.totalPages > 1 ? (
        <Pagination
          page={Math.min(filters.page, data.totalPages)}
          totalPages={data.totalPages}
          onPageChange={(page) => updateFilters({ page })}
        />
      ) : null}

      <TaskFormDialog
        open={formDialog.open}
        task={formDialog.task}
        onOpenChange={(open) => setFormDialog((state) => ({ ...state, open }))}
      />
      <DeleteTaskDialog
        open={deleteDialog.open}
        task={deleteDialog.task}
        onOpenChange={(open) => setDeleteDialog((state) => ({ ...state, open }))}
      />
    </div>
  );
}
