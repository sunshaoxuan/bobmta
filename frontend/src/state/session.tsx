import { useCallback, useMemo, useState } from '../../vendor/react/index.js';
import { login } from '../api/auth';
import type { ApiClient, ApiError } from '../api/client';
import type { LoginResponse } from '../api/types';

export type SessionState = {
  session: LoginResponse | null;
  status: 'idle' | 'loading' | 'authenticated';
  error: ApiError | null;
};

export type SessionController = {
  state: SessionState;
  login: (credentials: { username: string; password: string }) => Promise<void>;
  logout: () => void;
};

const initialState: SessionState = {
  session: null,
  status: 'idle',
  error: null,
};

export function useSessionController(client: ApiClient): SessionController {
  const [state, setState] = useState<SessionState>(initialState);

  const handleLogin = useCallback(
    async (credentials: { username: string; password: string }) => {
      const username = credentials.username.trim();
      if (!username || credentials.password.length === 0 || state.status === 'loading') {
        return;
      }
      setState((current) => ({ ...current, status: 'loading', error: null }));
      try {
        const session = await login(client, {
          username,
          password: credentials.password,
        });
        setState({ session, status: 'authenticated', error: null });
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setState({ session: null, status: 'idle', error: apiError });
      }
    },
    [client, state.status]
  );

  const handleLogout = useCallback(() => {
    setState(initialState);
  }, []);

  return useMemo(
    () => ({
      state,
      login: handleLogin,
      logout: handleLogout,
    }),
    [state, handleLogin, handleLogout]
  );
}
