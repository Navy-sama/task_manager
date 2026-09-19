/** Paginated list envelope returned by every list endpoint (docs/api-contract.md §3). */
export interface PageResponse<T> {
  content: T[];
  /** Zero-based page index. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
