import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";
import { fieldControlClasses } from "./variants";

export function Input({ className, type = "text", ...props }: ComponentProps<"input">) {
  return (
    <input
      data-slot="input"
      type={type}
      className={cn(fieldControlClasses, "h-11 px-3 md:h-10", className)}
      {...props}
    />
  );
}
