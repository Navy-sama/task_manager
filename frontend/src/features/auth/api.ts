import { apiClient } from "@/lib/api-client";
import type { AuthResponse, Credentials, UserResponse } from "./types";

export const authApi = {
  me: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>("/auth/me");
    return response.data;
  },

  login: async (credentials: Credentials): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>("/auth/login", credentials);
    return response.data;
  },

  register: async (credentials: Credentials): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>("/auth/register", credentials);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post("/auth/logout");
  },
};
