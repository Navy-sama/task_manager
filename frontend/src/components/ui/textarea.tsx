import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";
import { fieldControlClasses } from "./variants";

export function Textarea({ className, ...props }: ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(fieldControlClasses, "min-h-24 resize-y px-3 py-2", className)}
      {...props}
    />
  );
}
