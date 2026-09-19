import { z } from "zod";
import { TASK_STATUSES, type TaskInput, type TaskStatus } from "./types";

export const TITLE_MAX_LENGTH = 200;
export const DESCRIPTION_MAX_LENGTH = 2000;

export const taskStatusSchema = z.enum(TASK_STATUSES);

export function isTaskStatus(value: unknown): value is TaskStatus {
  return taskStatusSchema.safeParse(value).success;
}

/** Create/edit form. Messages are translation keys, translated at render time. */
export const taskFormSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, "validation.titleRequired")
    .max(TITLE_MAX_LENGTH, "validation.titleMax"),
  description: z.string().trim().max(DESCRIPTION_MAX_LENGTH, "validation.descriptionMax"),
  status: taskStatusSchema,
});

export type TaskFormValues = z.input<typeof taskFormSchema>;
export type TaskFormOutput = z.output<typeof taskFormSchema>;

/** An empty description is sent as `null` ("none"), as the API contract expects. */
export function toTaskInput(values: TaskFormOutput): TaskInput {
  return {
    title: values.title,
    description: values.description === "" ? null : values.description,
    status: values.status,
  };
}

/** One-based page number read from the URL; anything else falls back to the first page. */
export const pageParamSchema = z.coerce.number().int().min(1).catch(1);
