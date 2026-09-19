export interface UserResponse {
  id: number;
  email: string;
}

/** Web mode only receives `user`: tokens travel in HttpOnly cookies (docs/api-contract.md §3). */
export interface AuthResponse {
  user: UserResponse;
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
}

export interface Credentials {
  email: string;
  password: string;
}
