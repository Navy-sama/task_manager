import { isTaskStatus, pageParamSchema } from "./schemas";
import type { TaskListParams, TaskStatus } from "./types";

export const TASK_PAGE_SIZE = 10;

/**
 * List state kept in the URL (`?status=&q=&page=`) so that a reload or a shared link shows the
 * same view. `page` is one-based here (what users see) and zero-based in the API.
 */
export interface TaskFilters {
  status: TaskStatus | undefined;
  q: string;
  page: number;
}

export function readTaskFilters(params: URLSearchParams): TaskFilters {
  const status = params.get("status");
  return {
    status: isTaskStatus(status) ? status : undefined,
    q: params.get("q") ?? "",
    page: pageParamSchema.parse(params.get("page") ?? undefined),
  };
}

/** Applies `patch` and returns canonical search params (default values are left out). */
export function writeTaskFilters(
  current: URLSearchParams,
  patch: Partial<TaskFilters>,
): URLSearchParams {
  const next = { ...readTaskFilters(current), ...patch };
  const params = new URLSearchParams();
  if (next.status) params.set("status", next.status);
  if (next.q) params.set("q", next.q);
  if (next.page > 1) params.set("page", String(next.page));
  return params;
}

export function hasActiveFilters(filters: TaskFilters): boolean {
  return filters.status !== undefined || filters.q.trim() !== "";
}

export function toListParams(filters: TaskFilters): TaskListParams {
  const q = filters.q.trim();
  return {
    status: filters.status,
    q: q === "" ? undefined : q,
    page: filters.page - 1,
    size: TASK_PAGE_SIZE,
  };
}
