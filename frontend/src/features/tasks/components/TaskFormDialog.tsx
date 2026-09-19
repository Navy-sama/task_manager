import { zodResolver } from "@hookform/resolvers/zod";
import { LoaderCircleIcon } from "lucide-react";
import { useId, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { FieldError } from "@/components/FieldError";
import { FormAlert } from "@/components/FormAlert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { getErrorMessage } from "@/i18n/error-message";
import { toApiError } from "@/lib/api-error";
import { applyServerFieldErrors } from "@/lib/form-errors";
import { useCreateTask, useUpdateTask } from "../hooks";
import {
  isTaskStatus,
  taskFormSchema,
  toTaskInput,
  type TaskFormOutput,
  type TaskFormValues,
} from "../schemas";
import { TASK_STATUSES, type TaskResponse } from "../types";

interface TaskFormDialogProps {
  open: boolean;
  /** The task to edit, or `null` to create one. */
  task: TaskResponse | null;
  onOpenChange: (open: boolean) => void;
}

export function TaskFormDialog({ open, task, onOpenChange }: TaskFormDialogProps) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {task ? t("tasks.form.editTitle") : t("tasks.form.createTitle")}
          </DialogTitle>
          <DialogDescription>
            {task ? t("tasks.form.editDescription") : t("tasks.form.createDescription")}
          </DialogDescription>
        </DialogHeader>
        {/* Mounted with the dialog content, so every opening starts from fresh values. */}
        <TaskForm task={task} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

const FIELDS = ["title", "description", "status"] as const;

function TaskForm({ task, onDone }: { task: TaskResponse | null; onDone: () => void }) {
  const { t } = useTranslation();
  const id = useId();
  const createTask = useCreateTask();
  const updateTask = useUpdateTask();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<TaskFormValues, unknown, TaskFormOutput>({
    resolver: zodResolver(taskFormSchema),
    defaultValues: {
      title: task?.title ?? "",
      description: task?.description ?? "",
      status: task?.status ?? "TODO",
    },
    mode: "onTouched",
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    const input = toTaskInput(values);
    try {
      if (task) {
        await updateTask.mutateAsync({ id: task.id, input });
        toast.success(t("tasks.toast.updated"));
      } else {
        await createTask.mutateAsync(input);
        toast.success(t("tasks.toast.created"));
      }
      onDone();
    } catch (error) {
      const apiError = toApiError(error);
      if (apiError.code === "TASK_NOT_FOUND") {
        toast.error(t("tasks.toast.notFound"));
        onDone();
        return;
      }
      if (
        apiError.code === "VALIDATION_FAILED" &&
        applyServerFieldErrors(apiError, setError, FIELDS)
      ) {
        return;
      }
      const message = getErrorMessage(t, apiError);
      setFormError(message);
      toast.error(message);
    }
  });

  const pending = isSubmitting || createTask.isPending || updateTask.isPending;
  const titleErrorId = `${id}-title-error`;
  const descriptionErrorId = `${id}-description-error`;
  const statusErrorId = `${id}-status-error`;

  return (
    <form noValidate onSubmit={onSubmit} className="grid gap-5" aria-busy={pending}>
      {formError ? <FormAlert>{formError}</FormAlert> : null}

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-title`}>{t("tasks.form.title")}</Label>
        <Input
          id={`${id}-title`}
          placeholder={t("tasks.form.titlePlaceholder")}
          aria-invalid={errors.title ? true : undefined}
          aria-describedby={titleErrorId}
          aria-required="true"
          {...register("title")}
        />
        <FieldError id={titleErrorId} message={errors.title?.message} />
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-description`}>
          {t("tasks.form.description")}{" "}
          <span className="font-normal text-muted-foreground">({t("tasks.form.optional")})</span>
        </Label>
        <Textarea
          id={`${id}-description`}
          rows={4}
          aria-invalid={errors.description ? true : undefined}
          aria-describedby={descriptionErrorId}
          {...register("description")}
        />
        <FieldError id={descriptionErrorId} message={errors.description?.message} />
      </div>

      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-status`}>{t("tasks.form.status")}</Label>
        <Controller
          control={control}
          name="status"
          render={({ field }) => (
            <Select
              value={field.value}
              onValueChange={(value) => {
                if (isTaskStatus(value)) field.onChange(value);
              }}
            >
              <SelectTrigger
                id={`${id}-status`}
                ref={field.ref}
                onBlur={field.onBlur}
                aria-invalid={errors.status ? true : undefined}
                aria-describedby={statusErrorId}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TASK_STATUSES.map((status) => (
                  <SelectItem key={status} value={status}>
                    {t(`tasks.status.${status}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        <FieldError id={statusErrorId} message={errors.status?.message} />
      </div>

      <DialogFooter>
        <DialogClose asChild>
          <Button variant="outline" disabled={pending}>
            {t("common.cancel")}
          </Button>
        </DialogClose>
        <Button type="submit" disabled={pending}>
          {pending ? (
            <>
              <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
              {t("tasks.form.saving")}
            </>
          ) : task ? (
            t("tasks.form.save")
          ) : (
            t("tasks.form.create")
          )}
        </Button>
      </DialogFooter>
    </form>
  );
}
