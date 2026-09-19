import { cva } from "class-variance-authority";

/** Shared focus ring: always visible for keyboard users, never for mouse clicks. */
export const focusRing =
  "focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background";

/** Base look of text-like form controls (input, textarea, select trigger). */
export const fieldControlClasses = [
  "w-full min-w-0 rounded-md border border-input bg-card text-base text-foreground shadow-xs md:text-sm",
  "transition-[border-color,box-shadow] duration-150 placeholder:text-muted-foreground/80",
  "focus-visible:border-ring focus-visible:outline-hidden focus-visible:ring-3 focus-visible:ring-ring/30",
  "disabled:cursor-not-allowed disabled:opacity-60",
  "aria-invalid:border-destructive aria-invalid:focus-visible:ring-destructive/25",
].join(" ");

export const buttonVariants = cva(
  [
    "inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium",
    "transition-colors duration-150 disabled:pointer-events-none disabled:opacity-50",
    "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
    focusRing,
  ],
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground shadow-xs hover:bg-primary-hover",
        destructive:
          "bg-destructive text-destructive-foreground shadow-xs hover:bg-destructive-hover",
        outline:
          "border border-input bg-card text-foreground shadow-xs hover:bg-muted hover:text-foreground",
        ghost: "text-foreground hover:bg-muted",
        link: "h-auto px-0 text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-11 px-4 md:h-10",
        sm: "h-10 px-3 md:h-9",
        icon: "size-11 md:size-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export const overlayClasses =
  "fixed inset-0 z-50 bg-slate-950/40 backdrop-blur-[2px] data-[state=open]:animate-fade-in data-[state=closed]:animate-fade-out";

export const modalContentClasses =
  "fixed top-1/2 left-1/2 z-50 grid max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 gap-4 overflow-y-auto rounded-xl border border-border bg-popover p-6 text-popover-foreground shadow-lg focus:outline-hidden data-[state=open]:animate-zoom-in data-[state=closed]:animate-zoom-out";
