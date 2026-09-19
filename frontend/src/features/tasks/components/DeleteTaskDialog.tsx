import { LoaderCircleIcon, Trash2Icon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { useDeleteTask } from "../hooks";
import { notifyTaskMutationError } from "../notify";
import type { TaskResponse } from "../types";

interface DeleteTaskDialogProps {
  open: boolean;
  task: TaskResponse | null;
  onOpenChange: (open: boolean) => void;
}

export function DeleteTaskDialog({ open, task, onOpenChange }: DeleteTaskDialogProps) {
  const { t } = useTranslation();
  const deleteTask = useDeleteTask();
  const pending = deleteTask.isPending;

  const confirm = () => {
    if (!task) {
      return;
    }
    deleteTask.mutate(task.id, {
      onSuccess: () => {
        toast.success(t("tasks.toast.deleted"));
        onOpenChange(false);
      },
      onError: (error) => {
        if (notifyTaskMutationError(t, error)) {
          onOpenChange(false);
        }
      },
    });
  };

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        // Stay open while the deletion runs, so its outcome is visible.
        if (!pending) onOpenChange(next);
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("tasks.deleteDialog.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("tasks.deleteDialog.description", { title: task?.title ?? "" })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>{t("common.cancel")}</AlertDialogCancel>
          <Button variant="destructive" onClick={confirm} disabled={pending}>
            {pending ? (
              <>
                <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
                {t("tasks.deleteDialog.deleting")}
              </>
            ) : (
              <>
                <Trash2Icon aria-hidden="true" />
                {t("tasks.deleteDialog.confirm")}
              </>
            )}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
