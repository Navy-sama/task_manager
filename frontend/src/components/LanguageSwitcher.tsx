import { useTranslation } from "react-i18next";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  SUPPORTED_LANGUAGES,
  changeLanguage,
  currentLanguage,
  isLanguage,
  type Language,
} from "@/i18n";

/** Each language is named in itself, so users can find theirs whatever the current UI language. */
const NATIVE_NAMES: Record<Language, string> = {
  en: "English",
  fr: "Français",
};

export function LanguageSwitcher() {
  // Subscribing to i18next re-renders the switcher when the language changes.
  const { t } = useTranslation();
  const language = currentLanguage();

  return (
    <ToggleGroup
      type="single"
      value={language}
      onValueChange={(value) => {
        if (isLanguage(value) && value !== language) {
          void changeLanguage(value);
        }
      }}
      aria-label={t("language.label")}
      className="p-0.5"
    >
      {SUPPORTED_LANGUAGES.map((code) => (
        <ToggleGroupItem
          key={code}
          value={code}
          lang={code}
          aria-label={NATIVE_NAMES[code]}
          title={NATIVE_NAMES[code]}
          className="h-8 px-2.5 text-xs uppercase"
        >
          {code}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
