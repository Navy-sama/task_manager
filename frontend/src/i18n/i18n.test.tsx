import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { LoginPage } from "@/features/auth/pages/LoginPage";
import { renderWithProviders } from "@/test/render";
import { detectInitialLanguage } from "./index";
import en from "./locales/en.json";
import fr from "./locales/fr.json";

/** Flattens nested keys, merging plural forms (`count_one`, `count_many`...) into their base key. */
function keysOf(value: unknown, prefix = ""): string[] {
  if (typeof value !== "object" || value === null) {
    return [prefix.replace(/_(zero|one|two|few|many|other)$/, "")];
  }
  return Object.entries(value).flatMap(([key, child]) =>
    keysOf(child, prefix ? `${prefix}.${key}` : key),
  );
}

describe("i18n", () => {
  it("has the same keys in English and French", () => {
    expect(new Set(keysOf(fr))).toEqual(new Set(keysOf(en)));
  });

  it("switches the interface to French and remembers the choice", async () => {
    const { user } = renderWithProviders(<LoginPage />, { route: "/login" });
    expect(screen.getByRole("heading", { name: "Welcome back" })).toBeInTheDocument();

    await user.click(screen.getByRole("radio", { name: "Français" }));

    expect(
      await screen.findByRole("heading", { name: "Bon retour parmi nous" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Mot de passe")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Se connecter" })).toBeInTheDocument();
    expect(document.documentElement.lang).toBe("fr");
    expect(detectInitialLanguage()).toBe("fr");
  });

  it("marks the active language in the switcher", () => {
    renderWithProviders(<LanguageSwitcher />);

    expect(screen.getByRole("radio", { name: "English" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "Français" })).not.toBeChecked();
  });
});
