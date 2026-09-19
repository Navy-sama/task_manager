import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { tasksApi } from "./api";
import type { TaskInput, TaskListParams } from "./types";

export const taskKeys = {
  all: ["tasks"] as const,
  lists: () => [...taskKeys.all, "list"] as const,
  list: (params: TaskListParams) => [...taskKeys.lists(), params] as const,
};

export function useTasks(params: TaskListParams) {
  return useQuery({
    queryKey: taskKeys.list(params),
    queryFn: ({ signal }) => tasksApi.list(params, signal),
    // Keeps the current page on screen while the next page / filter result loads.
    placeholderData: keepPreviousData,
  });
}

/**
 * Every mutation refetches the task lists once settled, including on failure: a `TASK_NOT_FOUND`
 * means the list on screen is stale (the task was deleted elsewhere, e.g. from the mobile app).
 * Returning the promise keeps the mutation pending until the fresh list is there.
 */
function useInvalidateTasks() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: taskKeys.all });
}

export function useCreateTask() {
  const invalidateTasks = useInvalidateTasks();
  return useMutation({
    mutationFn: (input: TaskInput) => tasksApi.create(input),
    onSettled: invalidateTasks,
  });
}

export function useUpdateTask() {
  const invalidateTasks = useInvalidateTasks();
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: TaskInput }) => tasksApi.update(id, input),
    onSettled: invalidateTasks,
  });
}

export function useDeleteTask() {
  const invalidateTasks = useInvalidateTasks();
  return useMutation({
    mutationFn: (id: number) => tasksApi.remove(id),
    onSettled: invalidateTasks,
  });
}
