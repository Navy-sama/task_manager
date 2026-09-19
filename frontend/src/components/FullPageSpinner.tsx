import { LoaderCircleIcon } from "lucide-react";
import { useTranslation } from "react-i18next";

export function FullPageSpinner() {
  const { t } = useTranslation();
  return (
    <div role="status" className="grid min-h-dvh place-items-center">
      <LoaderCircleIcon className="size-8 animate-spin text-primary" aria-hidden="true" />
      <span className="sr-only">{t("app.loading")}</span>
    </div>
  );
}
