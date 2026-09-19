export const TASK_STATUSES = ["TODO", "IN_PROGRESS", "DONE"] as const;

export type TaskStatus = (typeof TASK_STATUSES)[number];

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  /** ISO-8601 UTC instant. */
  createdAt: string;
  /** ISO-8601 UTC instant. */
  updatedAt: string;
}

/** Body of `POST /tasks` and `PUT /tasks/{id}` (full replacement of the editable fields). */
export interface TaskInput {
  title: string;
  description: string | null;
  status: TaskStatus;
}

/** Query parameters of `GET /tasks`, as sent to the API (zero-based page). */
export interface TaskListParams {
  status?: TaskStatus | undefined;
  q?: string | undefined;
  page: number;
  size: number;
}
