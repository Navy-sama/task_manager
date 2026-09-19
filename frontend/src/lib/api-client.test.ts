import { delay, http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { problem } from "@/test/handlers";
import { server } from "@/test/server";
import { apiClient, setSessionExpiredHandler } from "./api-client";
import { ApiError, NETWORK_ERROR, UNKNOWN_ERROR } from "./api-error";

async function captureError(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise;
  } catch (error) {
    if (error instanceof ApiError) return error;
    throw new Error("Expected an ApiError", { cause: error });
  }
  throw new Error("Expected the request to fail");
}

/** `/api/tasks` answers 401 until a refresh succeeded, like an expired access token would. */
function expiredAccessToken({ refreshDelayMs = 0, refreshSucceeds = true } = {}) {
  const calls = { refresh: 0, tasks: 0 };
  let refreshed = false;
  server.use(
    http.post("/api/auth/refresh", async () => {
      calls.refresh += 1;
      await delay(refreshDelayMs);
      if (!refreshSucceeds) {
        return problem(401, "INVALID_REFRESH_TOKEN", "Invalid refresh token.");
      }
      refreshed = true;
      return HttpResponse.json({ user: { id: 1, email: "jane@example.com" } });
    }),
    http.get("/api/tasks", () => {
      calls.tasks += 1;
      return refreshed
        ? HttpResponse.json({ ok: true })
        : problem(401, "UNAUTHENTICATED", "Authentication required.");
    }),
  );
  return calls;
}

describe("apiClient error normalisation", () => {
  it("turns a Problem Details body into an ApiError", async () => {
    server.use(
      http.post("/api/tasks", () =>
        problem(400, "VALIDATION_FAILED", "Request validation failed.", [
          { field: "title", message: "must not be blank" },
        ]),
      ),
    );

    const error = await captureError(apiClient.post("/tasks", { title: "" }));

    expect(error).toMatchObject({
      status: 400,
      code: "VALIDATION_FAILED",
      detail: "Request validation failed.",
      errors: [{ field: "title", message: "must not be blank" }],
    });
  });

  it("maps a non Problem Details error response to UNKNOWN_ERROR with its status", async () => {
    server.use(http.get("/api/tasks", () => new HttpResponse("Bad gateway", { status: 502 })));

    const error = await captureError(apiClient.get("/tasks"));

    expect(error.status).toBe(502);
    expect(error.code).toBe(UNKNOWN_ERROR);
  });

  it("maps a network failure to NETWORK_ERROR", async () => {
    server.use(http.get("/api/tasks", () => HttpResponse.error()));

    const error = await captureError(apiClient.get("/tasks"));

    expect(error.status).toBe(0);
    expect(error.code).toBe(NETWORK_ERROR);
  });

  it("sends the XSRF-TOKEN cookie back in the X-XSRF-TOKEN header", async () => {
    document.cookie = "XSRF-TOKEN=csrf-123; path=/";
    let header: string | null = null;
    server.use(
      http.post("/api/tasks", ({ request }) => {
        header = request.headers.get("X-XSRF-TOKEN");
        return HttpResponse.json({}, { status: 201 });
      }),
    );

    await apiClient.post("/tasks", { title: "A" });

    expect(header).toBe("csrf-123");
    document.cookie = "XSRF-TOKEN=; path=/; max-age=0";
  });
});

describe("apiClient session refresh", () => {
  it("refreshes the session once on 401, then replays the request", async () => {
    const calls = expiredAccessToken();

    const response = await apiClient.get("/tasks");

    expect(response.data).toEqual({ ok: true });
    expect(calls).toEqual({ refresh: 1, tasks: 2 });
  });

  it("shares a single refresh between concurrent 401s", async () => {
    const calls = expiredAccessToken({ refreshDelayMs: 50 });

    const responses = await Promise.all([apiClient.get("/tasks"), apiClient.get("/tasks")]);

    expect(responses.map((response) => response.status)).toEqual([200, 200]);
    expect(calls.refresh).toBe(1);
  });

  it("calls the session-expired handler when the refresh fails", async () => {
    const onSessionExpired = vi.fn();
    setSessionExpiredHandler(onSessionExpired);
    expiredAccessToken({ refreshSucceeds: false });

    const error = await captureError(apiClient.get("/tasks"));

    expect(error.code).toBe("UNAUTHENTICATED");
    expect(onSessionExpired).toHaveBeenCalledOnce();
  });

  it("never refreshes on a 401 from the login endpoint", async () => {
    const refresh = vi.fn();
    server.use(
      http.post("/api/auth/refresh", () => {
        refresh();
        return HttpResponse.json({});
      }),
      http.post("/api/auth/login", () =>
        problem(401, "INVALID_CREDENTIALS", "Invalid email or password."),
      ),
    );

    const error = await captureError(apiClient.post("/auth/login", {}));

    expect(error.code).toBe("INVALID_CREDENTIALS");
    expect(refresh).not.toHaveBeenCalled();
  });
});
