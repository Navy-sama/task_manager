import { http, HttpResponse } from "msw";
import type { UserResponse } from "@/features/auth/types";
import type { TaskInput, TaskResponse } from "@/features/tasks/types";
import type { ApiFieldError } from "@/lib/api-error";
import type { PageResponse } from "@/lib/api-types";

export const testUser: UserResponse = { id: 1, email: "jane@example.com" };

let nextId = 1;

export function makeTask(overrides: Partial<TaskResponse> = {}): TaskResponse {
  const id = overrides.id ?? nextId++;
  return {
    id,
    title: `Task ${id}`,
    description: null,
    status: "TODO",
    createdAt: "2026-09-18T08:00:00.000Z",
    updatedAt: "2026-09-18T08:00:00.000Z",
    ...overrides,
  };
}

/** In-memory tasks backing the default handlers; reset after every test. */
export const taskDb = {
  tasks: [] as TaskResponse[],
  reset(tasks: TaskResponse[] = []) {
    this.tasks = tasks;
    nextId = 100;
  },
};

export function problem(status: number, code: string, detail: string, errors?: ApiFieldError[]) {
  return HttpResponse.json(
    { type: "about:blank", title: "Error", status, detail, code, ...(errors ? { errors } : {}) },
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}

function taskNotFound() {
  return problem(404, "TASK_NOT_FOUND", "Task not found.");
}

export const handlers = [
  http.get("/api/auth/me", () => HttpResponse.json(testUser)),
  http.post("/api/auth/refresh", () =>
    problem(401, "INVALID_REFRESH_TOKEN", "Invalid refresh token."),
  ),
  http.post("/api/auth/logout", () => new HttpResponse(null, { status: 204 })),
  http.post("/api/auth/login", () => HttpResponse.json({ user: testUser })),

  http.get("/api/tasks", ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get("status");
    const q = url.searchParams.get("q")?.toLowerCase();
    const page = Number(url.searchParams.get("page") ?? 0);
    const size = Number(url.searchParams.get("size") ?? 20);
    const matching = taskDb.tasks.filter(
      (task) =>
        (!status || task.status === status) &&
        (!q ||
          task.title.toLowerCase().includes(q) ||
          (task.description?.toLowerCase().includes(q) ?? false)),
    );
    const body: PageResponse<TaskResponse> = {
      content: matching.slice(page * size, (page + 1) * size),
      page,
      size,
      totalElements: matching.length,
      totalPages: Math.ceil(matching.length / size),
    };
    return HttpResponse.json(body);
  }),

  http.post("/api/tasks", async ({ request }) => {
    const input = (await request.json()) as TaskInput;
    const task = makeTask({ ...input, updatedAt: "2026-09-19T08:00:00.000Z" });
    taskDb.tasks = [task, ...taskDb.tasks];
    return HttpResponse.json(task, { status: 201 });
  }),

  http.put("/api/tasks/:id", async ({ request, params }) => {
    const existing = taskDb.tasks.find((task) => String(task.id) === params.id);
    if (!existing) {
      return taskNotFound();
    }
    const input = (await request.json()) as TaskInput;
    const updated: TaskResponse = { ...existing, ...input };
    taskDb.tasks = taskDb.tasks.map((task) => (task.id === existing.id ? updated : task));
    return HttpResponse.json(updated);
  }),

  http.delete("/api/tasks/:id", ({ params }) => {
    const exists = taskDb.tasks.some((task) => String(task.id) === params.id);
    if (!exists) {
      return taskNotFound();
    }
    taskDb.tasks = taskDb.tasks.filter((task) => String(task.id) !== params.id);
    return new HttpResponse(null, { status: 204 });
  }),
];
