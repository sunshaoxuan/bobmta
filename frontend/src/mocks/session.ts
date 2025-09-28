import type { LoginResponse } from '../api/types';

export function createMockSession(overrides: Partial<LoginResponse> = {}): LoginResponse {
  const expiresAt = overrides.expiresAt ?? new Date(Date.now() + 60 * 60 * 1000).toISOString();
  return {
    token: 'mock-token',
    displayName: '前端モック利用者',
    roles: ['ROLE_OPERATOR'],
    expiresAt,
    ...overrides,
  };
}
