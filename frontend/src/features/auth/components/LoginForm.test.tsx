import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { problem, testUser } from "@/test/handlers";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/server";
import { useAuthStore } from "../auth-store";
import { LoginForm } from "./LoginForm";

describe("LoginForm", () => {
  beforeEach(() => {
    useAuthStore.getState().clear();
  });

  it("shows field errors and sends nothing when the form is invalid", async () => {
    let called = false;
    server.use(
      http.post("/api/auth/login", () => {
        called = true;
        return HttpResponse.json({ user: testUser });
      }),
    );
    const { user } = renderWithProviders(<LoginForm />);

    await user.type(screen.getByLabelText("Email"), "not-an-email");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
    expect(called).toBe(false);
  });

  it("signs the user in with the submitted credentials", async () => {
    let body: unknown;
    server.use(
      http.post("/api/auth/login", async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ user: testUser });
      }),
    );
    const { user } = renderWithProviders(<LoginForm />);

    await user.type(screen.getByLabelText("Email"), "  jane@example.com ");
    await user.type(screen.getByLabelText("Password"), "correct-horse-42");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => expect(useAuthStore.getState().status).toBe("authenticated"));
    expect(useAuthStore.getState().user).toEqual(testUser);
    expect(body).toEqual({ email: "jane@example.com", password: "correct-horse-42" });
  });

  it("shows a form error when the credentials are rejected", async () => {
    server.use(
      http.post("/api/auth/login", () =>
        problem(401, "INVALID_CREDENTIALS", "Invalid email or password."),
      ),
    );
    const { user } = renderWithProviders(<LoginForm />);

    await user.type(screen.getByLabelText("Email"), "jane@example.com");
    await user.type(screen.getByLabelText("Password"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Incorrect email or password.");
    expect(useAuthStore.getState().status).toBe("unauthenticated");
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });

  it("maps server-side field errors onto the fields", async () => {
    server.use(
      http.post("/api/auth/login", () =>
        problem(400, "VALIDATION_FAILED", "Request validation failed.", [
          { field: "email", message: "must be a well-formed email address" },
        ]),
      ),
    );
    const { user } = renderWithProviders(<LoginForm />);

    await user.type(screen.getByLabelText("Email"), "jane@example.com");
    await user.type(screen.getByLabelText("Password"), "whatever-1");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("must be a well-formed email address")).toBeInTheDocument();
  });
});
