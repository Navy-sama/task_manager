import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "./locales/en.json";
import fr from "./locales/fr.json";

export const SUPPORTED_LANGUAGES = ["en", "fr"] as const;
export type Language = (typeof SUPPORTED_LANGUAGES)[number];
export const DEFAULT_LANGUAGE: Language = "en";

const STORAGE_KEY = "task-manager.language";

export function isLanguage(value: unknown): value is Language {
  return SUPPORTED_LANGUAGES.some((language) => language === value);
}

function readStoredLanguage(): Language | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return isLanguage(stored) ? stored : null;
  } catch {
    // Storage can be unavailable (privacy mode, disabled cookies): fall back to detection.
    return null;
  }
}

function storeLanguage(language: Language): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // Not persisting the choice is acceptable: it still applies to the current session.
  }
}

/** Saved choice first, then the browser language (French or English), then English. */
export function detectInitialLanguage(): Language {
  const stored = readStoredLanguage();
  if (stored) {
    return stored;
  }
  return navigator.language.toLowerCase().startsWith("fr") ? "fr" : DEFAULT_LANGUAGE;
}

/** Normalises i18next's current language (which may carry a region) to a supported one. */
export function currentLanguage(): Language {
  const language = i18n.resolvedLanguage ?? i18n.language;
  return isLanguage(language) ? language : DEFAULT_LANGUAGE;
}

export async function changeLanguage(language: Language): Promise<void> {
  storeLanguage(language);
  await i18n.changeLanguage(language);
}

i18n.on("languageChanged", (language) => {
  document.documentElement.lang = language;
});

void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    fr: { translation: fr },
  },
  lng: detectInitialLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: SUPPORTED_LANGUAGES,
  interpolation: { escapeValue: false },
  returnNull: false,
  initAsync: false,
});

export { i18n };
