import { apiClient } from "@/lib/api-client";
import type { PageResponse } from "@/lib/api-types";
import type { TaskInput, TaskListParams, TaskResponse } from "./types";

export const tasksApi = {
  list: async (
    params: TaskListParams,
    signal?: AbortSignal,
  ): Promise<PageResponse<TaskResponse>> => {
    // `undefined` values (no status filter, blank search) are left out of the query string.
    const response = await apiClient.get<PageResponse<TaskResponse>>("/tasks", { params, signal });
    return response.data;
  },

  create: async (input: TaskInput): Promise<TaskResponse> => {
    const response = await apiClient.post<TaskResponse>("/tasks", input);
    return response.data;
  },

  update: async (id: number, input: TaskInput): Promise<TaskResponse> => {
    const response = await apiClient.put<TaskResponse>(`/tasks/${id}`, input);
    return response.data;
  },

  remove: async (id: number): Promise<void> => {
    await apiClient.delete(`/tasks/${id}`);
  },
};
