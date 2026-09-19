import { screen } from "@testing-library/react";
import { http } from "msw";
import { describe, expect, it } from "vitest";
import { problem } from "@/test/handlers";
import { renderApp } from "@/test/render";
import { server } from "@/test/server";

describe("route guards", () => {
  it("redirects anonymous visitors from the tasks page to /login", async () => {
    server.use(http.get("/api/auth/me", () => problem(401, "UNAUTHENTICATED", "No session.")));

    renderApp("/?status=DONE");

    expect(await screen.findByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/login$/);
  });

  it("sends signed-in users away from /login to the tasks page", async () => {
    renderApp("/login");

    expect(await screen.findByRole("heading", { name: "My tasks" })).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/$/);
  });
});
