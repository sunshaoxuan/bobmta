import type { Locale } from '../i18n/localization';

export type ApiError =
  | {
      type: 'status';
      status: number;
      code?: string | number | null;
      message?: string | null;
    }
  | {
      type: 'network';
      message?: string | null;
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
  put: <T>(path: string, body: unknown, options?: RequestOptions) => Promise<T>;
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
      throw { type: 'network', message: (error as Error)?.message ?? null } as ApiError;
    }

    if (!response.ok) {
      throw await buildStatusError(response);
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
    put: <T>(path: string, body: unknown, options?: RequestOptions) =>
      request<T>(path, 'PUT', { ...options, body }),
  };
}

async function buildStatusError(response: Response): Promise<ApiError> {
  const error: ApiError = { type: 'status', status: response.status };
  let bodyText: string | null = null;
  try {
    bodyText = await response.text();
  } catch (error) {
    bodyText = null;
  }
  if (!bodyText) {
    return error;
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(bodyText);
  } catch (error) {
    parsed = null;
  }

  if (parsed && typeof parsed === 'object') {
    const payload = parsed as {
      code?: unknown;
      message?: unknown;
      data?: unknown;
    };
    const candidates: Array<{ code?: unknown; message?: unknown }> = [
      payload,
    ];
    if (payload && payload.data && typeof payload.data === 'object') {
      candidates.push(payload.data as { code?: unknown; message?: unknown });
    }
    for (const candidate of candidates) {
      if (candidate) {
        if (candidate.code !== undefined && candidate.code !== null) {
          const normalizedCode = normalizeErrorCode(candidate.code);
          if (normalizedCode !== null) {
            (error as { code?: string | number | null }).code = normalizedCode;
          }
        }
        if (typeof candidate.message === 'string' && candidate.message.trim()) {
          (error as { message?: string | null }).message = candidate.message.trim();
          break;
        }
      }
    }
    if (!('message' in error) || !error.message) {
      const textCandidate = extractStringFromUnknown(parsed);
      if (textCandidate) {
        (error as { message?: string | null }).message = textCandidate;
      }
    }
    return error;
  }

  const fallback = bodyText.trim();
  if (fallback) {
    (error as { message?: string | null }).message = fallback;
  }
  return error;
}

function normalizeErrorCode(value: unknown): string | number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  return null;
}

function extractStringFromUnknown(value: unknown): string | null {
  if (!value) {
    return null;
  }
  if (typeof value === 'string') {
    return value.trim() || null;
  }
  if (typeof value === 'object' && 'message' in (value as Record<string, unknown>)) {
    const message = (value as { message?: unknown }).message;
    if (typeof message === 'string') {
      return message.trim() || null;
    }
  }
  return null;
}
