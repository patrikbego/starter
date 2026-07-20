export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class NetworkError extends Error {
  constructor(message = 'No network connection') {
    super(message);
    this.name = 'NetworkError';
  }
}

export interface ApiPort {
  get<T>(path: string, options?: { authenticated?: boolean }): Promise<T>;
  post<T>(path: string, body: unknown, options?: { authenticated?: boolean }): Promise<T>;
}
