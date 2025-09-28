import type { ApiClient, ApiError } from './client';
import type { PingResponse } from './types';

export async function fetchPing(
  client: ApiClient,
  signal?: AbortSignal
): Promise<PingResponse> {
  try {
    return await client.get<PingResponse>('/api/ping', { signal });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    if ((error as ApiError)?.type === 'network' || (error as ApiError)?.type === 'status') {
      throw error;
    }
    throw { type: 'network' } as ApiError;
  }
}
