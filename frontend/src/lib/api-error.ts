import { isAxiosError } from "axios";

/** One entry of a `VALIDATION_FAILED` problem's `errors` array. */
export interface ApiFieldError {
  field: string;
  message: string;
}

/** RFC 9457 Problem Details body, extended with the API's stable `code` (docs/api-contract.md §4). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: ApiFieldError[];
}

/** Client-side codes, used when the server could not produce a Problem Details body. */
export const NETWORK_ERROR = "NETWORK_ERROR";
export const UNKNOWN_ERROR = "UNKNOWN_ERROR";

export interface ApiErrorInit {
  status: number;
  code: string;
  detail: string;
  errors?: ApiFieldError[];
}

/**
 * The only error type the rest of the app deals with: every failure coming out of `apiClient`
 * (Problem Details, non-JSON error page, network failure) is normalised into it, so callers branch
 * on the stable `code` instead of inspecting axios internals.
 */
export class ApiError extends Error {
  /** HTTP status, or `0` when no response was received. */
  readonly status: number;
  readonly code: string;
  /** Human-readable English sentence from the server (or a client-side fallback). */
  readonly detail: string;
  readonly errors?: ApiFieldError[];

  constructor({ status, code, detail, errors }: ApiErrorInit) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.detail = detail;
    if (errors) {
      this.errors = errors;
    }
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isFieldError(value: unknown): value is ApiFieldError {
  return isRecord(value) && typeof value.field === "string" && typeof value.message === "string";
}

function readProblemDetail(data: unknown): ProblemDetail | null {
  if (!isRecord(data)) {
    return null;
  }
  const problem: ProblemDetail = {};
  if (typeof data.type === "string") problem.type = data.type;
  if (typeof data.title === "string") problem.title = data.title;
  if (typeof data.status === "number") problem.status = data.status;
  if (typeof data.detail === "string") problem.detail = data.detail;
  if (typeof data.instance === "string") problem.instance = data.instance;
  if (typeof data.code === "string") problem.code = data.code;
  if (Array.isArray(data.errors)) problem.errors = data.errors.filter(isFieldError);
  return problem;
}

/** Converts anything thrown by an HTTP call into an {@link ApiError}. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }

  if (isAxiosError(error)) {
    const response = error.response;
    if (!response) {
      return new ApiError({
        status: 0,
        code: NETWORK_ERROR,
        detail: "The server could not be reached.",
      });
    }

    const problem = readProblemDetail(response.data);
    return new ApiError({
      status: problem?.status ?? response.status,
      code: problem?.code ?? UNKNOWN_ERROR,
      detail: problem?.detail ?? problem?.title ?? error.message,
      ...(problem?.errors?.length ? { errors: problem.errors } : {}),
    });
  }

  return new ApiError({
    status: 0,
    code: UNKNOWN_ERROR,
    detail: error instanceof Error ? error.message : "Unexpected error.",
  });
}
