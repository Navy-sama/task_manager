import { SearchIcon } from "lucide-react";
import { useEffect, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { isTaskStatus } from "../schemas";
import { TASK_STATUSES, type TaskStatus } from "../types";

export const SEARCH_DEBOUNCE_MS = 300;

const ALL = "ALL";
const STATUS_OPTIONS = [ALL, ...TASK_STATUSES] as const;

interface TaskFiltersProps {
  status: TaskStatus | undefined;
  q: string;
  onStatusChange: (status: TaskStatus | undefined) => void;
  /** Called once typing has paused for {@link SEARCH_DEBOUNCE_MS}. */
  onSearchChange: (q: string) => void;
}

export function TaskFilters({ status, q, onStatusChange, onSearchChange }: TaskFiltersProps) {
  const { t } = useTranslation();
  const searchId = useId();
  const [input, setInput] = useState(q);
  const [syncedQ, setSyncedQ] = useState(q);

  // The URL changed from elsewhere ("clear filters", history navigation): mirror it in the input.
  if (q !== syncedQ) {
    setSyncedQ(q);
    setInput(q);
  }

  useEffect(() => {
    if (input === q) {
      return undefined;
    }
    const timeout = window.setTimeout(() => onSearchChange(input), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timeout);
  }, [input, q, onSearchChange]);

  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div className="relative md:w-80">
        <Label htmlFor={searchId} className="sr-only">
          {t("tasks.filters.search")}
        </Label>
        <SearchIcon
          className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id={searchId}
          type="search"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder={t("tasks.filters.searchPlaceholder")}
          className="pl-9"
          autoComplete="off"
        />
      </div>

      <ToggleGroup
        type="single"
        value={status ?? ALL}
        onValueChange={(value) => {
          // An empty value means the active item was clicked again: keep the current filter.
          if (value === ALL) onStatusChange(undefined);
          else if (isTaskStatus(value)) onStatusChange(value);
        }}
        aria-label={t("tasks.filters.status")}
        className="grid w-full grid-cols-4 md:inline-flex md:w-auto"
      >
        {STATUS_OPTIONS.map((option) => (
          <ToggleGroupItem key={option} value={option} className="px-2 sm:px-3">
            {t(`tasks.status.${option}`)}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </div>
  );
}
