import type { ApiClient, ApiError } from './client';
import type { ApiEnvelope, LoginResponse } from './types';

export type LoginCredentials = {
  username: string;
  password: string;
};

export async function login(
  client: ApiClient,
  credentials: LoginCredentials
): Promise<LoginResponse> {
  try {
    const response = await client.post<ApiEnvelope<LoginResponse>>(
      '/api/v1/auth/login',
      credentials
    );
    if (!response || !response.data) {
      throw { type: 'network' } as ApiError;
    }
    return response.data;
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
