import { CircleCheckIcon, CircleDashedIcon, CircleIcon, type LucideIcon } from "lucide-react";
import type { TaskStatus } from "./types";

interface StatusMeta {
  icon: LucideIcon;
  variant: "neutral" | "warning" | "success";
}

/** Each status has its own icon as well as its colour, so it never relies on colour alone. */
export const STATUS_META: Record<TaskStatus, StatusMeta> = {
  TODO: { icon: CircleIcon, variant: "neutral" },
  IN_PROGRESS: { icon: CircleDashedIcon, variant: "warning" },
  DONE: { icon: CircleCheckIcon, variant: "success" },
};
