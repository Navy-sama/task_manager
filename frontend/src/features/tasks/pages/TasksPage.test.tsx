import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { makeTask, problem, taskDb, testUser } from "@/test/handlers";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/server";
import { TasksPage } from "./TasksPage";

/** Records the query string of every `GET /api/tasks` while delegating to the default handler. */
function recordListRequests(): URLSearchParams[] {
  const requests: URLSearchParams[] = [];
  server.events.on("request:start", ({ request }) => {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/api/tasks") {
      requests.push(url.searchParams);
    }
  });
  return requests;
}

function nth(elements: HTMLElement[], index: number): HTMLElement {
  const element = elements[index];
  if (!element) {
    throw new Error(`No element at index ${index}`);
  }
  return element;
}

function renderTasksPage(route = "/") {
  return renderWithProviders(<TasksPage />, { route });
}

describe("TasksPage", () => {
  beforeEach(() => {
    useAuthStore.getState().setUser(testUser);
    server.events.removeAllListeners();
  });

  it("lists the tasks returned by the API", async () => {
    taskDb.reset([
      makeTask({ id: 1, title: "Write the README", description: "With screenshots" }),
      makeTask({ id: 2, title: "Ship the app", status: "DONE" }),
    ]);

    renderTasksPage();

    const list = await screen.findByRole("list", { name: "My tasks" });
    const items = within(list).getAllByRole("listitem");
    expect(items).toHaveLength(2);
    expect(within(nth(items, 0)).getByText("Write the README")).toBeInTheDocument();
    expect(within(nth(items, 0)).getByText("With screenshots")).toBeInTheDocument();
    expect(within(nth(items, 1)).getByText("Done")).toBeInTheDocument();
    expect(screen.getByText("2 tasks")).toBeInTheDocument();
  });

  it("shows an empty state inviting to create the first task", async () => {
    renderTasksPage();

    expect(await screen.findByText("No tasks yet")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "New task" })).toHaveLength(2);
  });

  it("shows a dedicated empty state with a reset action when filters match nothing", async () => {
    taskDb.reset([makeTask({ title: "Only task" })]);
    const { user } = renderTasksPage("/?status=DONE");

    expect(await screen.findByText("No matching tasks")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Clear filters" }));

    expect(await screen.findByText("Only task")).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/$/);
  });

  it("shows an error state with a retry button", async () => {
    let fail = true;
    server.use(
      http.get("/api/tasks", () =>
        fail
          ? problem(500, "INTERNAL_ERROR", "Unexpected error.")
          : HttpResponse.json({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }),
      ),
    );
    const { user } = renderTasksPage();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Your tasks could not be loaded");
    fail = false;
    await user.click(within(alert).getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("No tasks yet")).toBeInTheDocument();
  });

  it("sends the status filter to the API and keeps it in the URL", async () => {
    taskDb.reset([
      makeTask({ title: "Todo task" }),
      makeTask({ title: "In progress task", status: "IN_PROGRESS" }),
    ]);
    const requests = recordListRequests();
    const { user } = renderTasksPage();
    await screen.findByText("Todo task");

    await user.click(screen.getByRole("radio", { name: "In progress" }));

    await waitFor(() => expect(screen.queryByText("Todo task")).not.toBeInTheDocument());
    expect(screen.getByText("In progress task")).toBeInTheDocument();
    expect(requests.at(-1)?.get("status")).toBe("IN_PROGRESS");
    expect(requests.at(-1)?.get("page")).toBe("0");
    expect(screen.getByTestId("location")).toHaveTextContent("/?status=IN_PROGRESS");
  });

  it("debounces the search before querying the API", async () => {
    taskDb.reset([makeTask({ title: "Buy milk" }), makeTask({ title: "Write report" })]);
    const requests = recordListRequests();
    const { user } = renderTasksPage();
    await screen.findByText("Buy milk");

    await user.type(screen.getByLabelText("Search tasks"), "report");

    await waitFor(() => expect(screen.queryByText("Buy milk")).not.toBeInTheDocument());
    const searches = requests.map((params) => params.get("q")).filter(Boolean);
    expect(searches).toEqual(["report"]);
    expect(screen.getByTestId("location")).toHaveTextContent("/?q=report");
  });

  it("creates a task from the dialog and refreshes the list", async () => {
    let body: unknown;
    server.events.on("request:start", ({ request }) => {
      if (request.method === "POST") {
        void request
          .clone()
          .json()
          .then((json: unknown) => {
            body = json;
          });
      }
    });
    const { user } = renderTasksPage();
    await screen.findByText("No tasks yet");

    await user.click(nth(screen.getAllByRole("button", { name: "New task" }), 0));
    const dialog = await screen.findByRole("dialog", { name: "New task" });
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));
    expect(await within(dialog).findByText("Title is required.")).toBeInTheDocument();

    await user.type(within(dialog).getByLabelText("Title"), "Write the README");
    await user.type(within(dialog).getByLabelText(/Description/), "Screenshots included");
    await user.click(within(dialog).getByRole("button", { name: "Create task" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(body).toEqual({
      title: "Write the README",
      description: "Screenshots included",
      status: "TODO",
    });
    expect(await screen.findByText("Write the README")).toBeInTheDocument();
    expect(await screen.findByText("Task created.")).toBeInTheDocument();
  });

  it("deletes a task only after confirmation", async () => {
    taskDb.reset([makeTask({ id: 5, title: "Obsolete task" })]);
    let deletedPath: string | null = null;
    server.events.on("request:start", ({ request }) => {
      if (request.method === "DELETE") deletedPath = new URL(request.url).pathname;
    });
    const { user } = renderTasksPage();
    await screen.findByText("Obsolete task");

    await user.click(screen.getByRole("button", { name: "Delete “Obsolete task”" }));
    const dialog = await screen.findByRole("alertdialog", { name: "Delete this task?" });
    expect(deletedPath).toBeNull();

    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
    expect(deletedPath).toBe("/api/tasks/5");
    expect(await screen.findByText("No tasks yet")).toBeInTheDocument();
  });

  it("explains that a task vanished when the API answers TASK_NOT_FOUND", async () => {
    taskDb.reset([makeTask({ id: 9, title: "Ghost task" })]);
    const { user } = renderTasksPage();
    await screen.findByText("Ghost task");
    taskDb.reset([]);

    await user.click(screen.getByRole("button", { name: "Delete “Ghost task”" }));
    const dialog = await screen.findByRole("alertdialog");
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    expect(
      await screen.findByText("This task no longer exists. The list has been refreshed."),
    ).toBeInTheDocument();
    expect(await screen.findByText("No tasks yet")).toBeInTheDocument();
  });
});
