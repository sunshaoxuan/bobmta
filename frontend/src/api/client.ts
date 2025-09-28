import type { Locale } from '../i18n/localization';

export type ApiError =
  | {
      type: 'status';
      status: number;
    }
  | {
      type: 'network';
    };

export type RequestOptions = {
  signal?: AbortSignal;
  headers?: Record<string, string>;
  authToken?: string | null;
};

export type RequestWithBodyOptions = RequestOptions & {
  body?: unknown;
};

export type ApiClient = {
  get: <T>(path: string, options?: RequestOptions) => Promise<T>;
  post: <T>(path: string, body: unknown, options?: RequestOptions) => Promise<T>;
};

export function isApiError(value: unknown): value is ApiError {
  if (!value || typeof value !== 'object') {
    return false;
  }
  if (!('type' in value)) {
    return false;
  }
  const type = (value as { type?: unknown }).type;
  return type === 'status' || type === 'network';
}

export function createApiClient(config: { getLocale: () => Locale }): ApiClient {
  const request = async <T>(
    path: string,
    method: string,
    options: RequestWithBodyOptions = {}
  ): Promise<T> => {
    const headers = new Headers(options.headers);
    headers.set('Accept-Language', config.getLocale());
    if (options.authToken) {
      headers.set('Authorization', `Bearer ${options.authToken}`);
    }
    const init: RequestInit = {
      method,
      headers,
      signal: options.signal,
    };
    if (options.body !== undefined) {
      headers.set('Content-Type', 'application/json');
      init.body = JSON.stringify(options.body);
    }

    let response: Response;
    try {
      response = await fetch(path, init);
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        throw error;
      }
      throw { type: 'network' } as ApiError;
    }

    if (!response.ok) {
      throw { type: 'status', status: response.status } as ApiError;
    }

    if (response.status === 204) {
      return undefined as T;
    }

    try {
      return (await response.json()) as T;
    } catch (error) {
      throw { type: 'network' } as ApiError;
    }
  };

  return {
    get: <T>(path: string, options?: RequestOptions) => request<T>(path, 'GET', options),
    post: <T>(path: string, body: unknown, options?: RequestOptions) =>
      request<T>(path, 'POST', { ...options, body }),
  };
}
