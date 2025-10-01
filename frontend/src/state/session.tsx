import { useCallback, useEffect, useMemo, useRef, useState } from '../../vendor/react/index.js';
import { login } from '../api/auth';
import { fetchNavigationMenu, type NavigationMenuPayload } from '../api/navigation';
import { isApiError, type ApiClient, type ApiError } from '../api/client';
import type { LoginResponse } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

export type SessionNavigationItem = {
  key: string;
  path: string;
  labelKey: UiMessageKey;
};

export type SessionNavigationState = {
  items: SessionNavigationItem[];
  loading: boolean;
  error: ApiError | null;
  source: 'mock' | 'remote';
};

export type SessionState = {
  session: LoginResponse | null;
  status: 'idle' | 'loading' | 'authenticated';
  error: ApiError | null;
  navigation: SessionNavigationState;
};

export type SessionController = {
  state: SessionState;
  login: (credentials: { username: string; password: string }) => Promise<void>;
  logout: () => void;
};

const normalizePath = (path: string): string => {
  if (!path) {
    return '/';
  }
  return path.startsWith('/') ? path : `/${path}`;
};

const MOCK_NAVIGATION_MENU: NavigationMenuPayload[] = [
  { key: 'overview', path: '/', labelKey: 'navMenuOverview' },
  {
    key: 'operations',
    path: '/plans',
    labelKey: 'navMenuOperations',
    roles: ['OPERATOR', 'PLANNER', 'ADMIN'],
  },
];

const filterNavigationByRoles = (
  items: NavigationMenuPayload[],
  roles: string[]
): SessionNavigationItem[] => {
  const normalizedRoles = new Set(roles.map((role) => role.toUpperCase()));
  const mapItem = (item: NavigationMenuPayload): SessionNavigationItem | null => {
    const requiredRoles = item.roles?.map((role) => role.toUpperCase()) ?? [];
    const allowed =
      requiredRoles.length === 0 || requiredRoles.some((role) => normalizedRoles.has(role));
    const children = item.children ? filterNavigationByRoles(item.children, roles) : [];
    if (!allowed && children.length === 0) {
      return null;
    }
    return {
      key: item.key,
      path: normalizePath(item.path),
      labelKey: item.labelKey,
    };
  };

  return items
    .map((item) => mapItem(item))
    .filter((item): item is SessionNavigationItem => Boolean(item));
};

const guestNavigation: SessionNavigationItem[] = filterNavigationByRoles(MOCK_NAVIGATION_MENU, []);

const initialState: SessionState = {
  session: null,
  status: 'idle',
  error: null,
  navigation: {
    items: guestNavigation,
    loading: false,
    error: null,
    source: 'mock',
  },
};

export function useSessionController(client: ApiClient): SessionController {
  const [state, setState] = useState<SessionState>(initialState);
  const navigationRequestRef = useRef<AbortController | null>(null);
  const sessionToken = state.session?.token ?? null;
  const roleSignature = state.session?.roles?.join('|') ?? '';
  const hasSession = Boolean(state.session);

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
        setState((current) => ({
          ...current,
          session,
          status: 'authenticated',
          error: null,
        }));
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setState((current) => ({
          ...current,
          session: null,
          status: 'idle',
          error: apiError,
        }));
      }
    },
    [client, state.status]
  );

  const handleLogout = useCallback(() => {
    navigationRequestRef.current?.abort();
    navigationRequestRef.current = null;
    setState(initialState);
  }, []);

  useEffect(() => {
    const roles = state.session?.roles ?? [];
    const fallbackItems = filterNavigationByRoles(MOCK_NAVIGATION_MENU, roles);

    if (!hasSession) {
      navigationRequestRef.current?.abort();
      navigationRequestRef.current = null;
      setState((current) => ({
        ...current,
        navigation: {
          items: fallbackItems,
          loading: false,
          error: null,
          source: 'mock',
        },
      }));
      return;
    }

    const controller = new AbortController();
    navigationRequestRef.current?.abort();
    navigationRequestRef.current = controller;

    setState((current) => ({
      ...current,
      navigation: {
        ...current.navigation,
        loading: true,
        error: null,
      },
    }));

    const loadNavigation = async () => {
      try {
        const payload = await fetchNavigationMenu(client, {
          authToken: sessionToken,
          signal: controller.signal,
        });
        if (controller.signal.aborted) {
          return;
        }
        const filtered = filterNavigationByRoles(payload, roles);
        const items = filtered.length > 0 ? filtered : fallbackItems;
        setState((current) => ({
          ...current,
          navigation: {
            items,
            loading: false,
            error: null,
            source: filtered.length > 0 ? 'remote' : 'mock',
          },
        }));
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }
        const apiError: ApiError = isApiError(error)
          ? (error as ApiError)
          : ({ type: 'network', message: (error as Error)?.message ?? null } as ApiError);
        setState((current) => ({
          ...current,
          navigation: {
            items: fallbackItems,
            loading: false,
            error: apiError,
            source: 'mock',
          },
        }));
      } finally {
        if (navigationRequestRef.current === controller) {
          navigationRequestRef.current = null;
        }
      }
    };

    void loadNavigation();

    return () => {
      controller.abort();
    };
  }, [client, hasSession, roleSignature, sessionToken]);

  useEffect(() => {
    return () => {
      navigationRequestRef.current?.abort();
    };
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
