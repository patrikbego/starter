import { config } from '@/src/config/env';
import type { AuthPort } from '@/src/ports/AuthPort';
import { ApiError, NetworkError, type ApiPort } from '@/src/ports/ApiPort';

interface HttpApiClientOptions {
  authPort: AuthPort;
  onUnauthorized?: () => void;
}

export function createHttpApiClient({
  authPort,
  onUnauthorized,
}: HttpApiClientOptions): ApiPort {
  async function request<T>(
    path: string,
    options: RequestInit & { authenticated?: boolean; retry?: boolean } = {},
  ): Promise<T> {
    const { authenticated = true, retry = true, ...fetchOptions } = options;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(fetchOptions.headers as Record<string, string> | undefined),
    };

    if (authenticated) {
      const token = await authPort.getIdToken();
      if (!token) {
        onUnauthorized?.();
        throw new ApiError(401, 'Not authenticated');
      }
      headers.Authorization = `Bearer ${token}`;
    }

    let response: Response;
    try {
      response = await fetch(`${config.apiBaseUrl}${path}`, {
        ...fetchOptions,
        headers,
      });
    } catch {
      throw new NetworkError();
    }

    if (response.status === 401 && authenticated && retry) {
      const refreshedToken = await authPort.getIdToken(true);
      if (!refreshedToken) {
        onUnauthorized?.();
        throw new ApiError(401, 'Session expired');
      }

      return request<T>(path, {
        ...options,
        retry: false,
        headers: {
          ...headers,
          Authorization: `Bearer ${refreshedToken}`,
        },
      });
    }

    if (response.status === 401) {
      onUnauthorized?.();
      throw new ApiError(401, 'Session expired');
    }

    if (!response.ok) {
      const body = await response.text();
      throw new ApiError(response.status, body || response.statusText);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  return {
    get<T>(path: string, options?: { authenticated?: boolean }) {
      return request<T>(path, { method: 'GET', ...options });
    },

    post<T>(path: string, body: unknown, options?: { authenticated?: boolean }) {
      return request<T>(path, {
        method: 'POST',
        body: JSON.stringify(body),
        ...options,
      });
    },
  };
}
