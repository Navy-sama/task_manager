import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";

interface PaginationProps {
  /** One-based. */
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  const { t } = useTranslation();
  return (
    <nav aria-label={t("pagination.label")} className="flex items-center justify-between gap-3">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
        aria-label={t("pagination.previousPage")}
      >
        <ChevronLeftIcon aria-hidden="true" />
        <span className="hidden sm:inline">{t("pagination.previous")}</span>
      </Button>
      <p className="text-sm text-muted-foreground" aria-live="polite">
        {t("pagination.pageOf", { page, totalPages })}
      </p>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
        aria-label={t("pagination.nextPage")}
      >
        <span className="hidden sm:inline">{t("pagination.next")}</span>
        <ChevronRightIcon aria-hidden="true" />
      </Button>
    </nav>
  );
}
