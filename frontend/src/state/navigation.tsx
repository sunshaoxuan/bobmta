import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from '../../vendor/react/index.js';
import { fetchNavigationMenu, type NavigationMenuPayload } from '../api/navigation';
import { isApiError, type ApiClient, type ApiError } from '../api/client';
import type { LoginResponse } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

type NavigationMenuItem = {
  key: string;
  path: string;
  labelKey: UiMessageKey;
  children?: NavigationMenuItem[];
};

type NavigationState = {
  items: NavigationMenuItem[];
  loading: boolean;
  error: ApiError | null;
  unauthorized: boolean;
  forbidden: boolean;
};

type NavigationController = {
  state: NavigationState;
  refresh: () => Promise<void>;
};

const initialState: NavigationState = {
  items: [],
  loading: false,
  error: null,
  unauthorized: false,
  forbidden: false,
};

const MOCK_MENU: NavigationMenuPayload[] = [
  { key: 'overview', path: '/', labelKey: 'navMenuOverview' },
  {
    key: 'operations',
    path: '/plans',
    labelKey: 'navMenuOperations',
    roles: ['OPERATOR', 'PLANNER', 'ADMIN'],
  },
];

function normalizePath(path: string): string {
  if (!path) {
    return '/';
  }
  return path.startsWith('/') ? path : `/${path}`;
}

function filterMenuByRoles(
  items: NavigationMenuPayload[],
  roles: string[]
): NavigationMenuItem[] {
  const normalizedRoles = new Set(roles.map((role) => role.toUpperCase()));
  const mapItem = (item: NavigationMenuPayload): NavigationMenuItem | null => {
    const required = item.roles?.map((role) => role.toUpperCase()) ?? [];
    const allowed = required.length === 0 || required.some((role) => normalizedRoles.has(role));
    const children = item.children ? filterMenuByRoles(item.children, roles) : [];
    if (!allowed && children.length === 0) {
      return null;
    }
    return {
      key: item.key,
      path: normalizePath(item.path),
      labelKey: item.labelKey,
      children: children.length > 0 ? children : undefined,
    };
  };
  const filtered = items
    .map((item) => mapItem(item))
    .filter((item): item is NavigationMenuItem => Boolean(item));
  return filtered.length > 0 ? filtered : [];
}

type NavigationControllerOptions = {
  onUnauthorized?: () => void;
};

export function useNavigationController(
  client: ApiClient,
  session: LoginResponse | null,
  options: NavigationControllerOptions = {}
): NavigationController {
  const [state, setState] = useState<NavigationState>(initialState);
  const requestRef = useRef<AbortController | null>(null);
  const sessionToken = session?.token ?? null;
  const roleSignature = session?.roles?.join('|') ?? '';

  const applyMenu = useCallback(
    (items: NavigationMenuPayload[], roles: string[]) => filterMenuByRoles(items, roles),
    []
  );

  const loadMenu = useCallback(async () => {
    const roles = session?.roles ?? [];
    const fallback = applyMenu(MOCK_MENU, roles);
    if (!session) {
      setState({
        items: fallback,
        loading: false,
        error: null,
        unauthorized: false,
        forbidden: false,
      });
      return;
    }
    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setState((current) => ({
      ...current,
      loading: true,
      error: null,
      unauthorized: false,
      forbidden: false,
    }));
    try {
      const payload = await fetchNavigationMenu(client, {
        authToken: sessionToken,
        signal: controller.signal,
      });
      if (controller.signal.aborted) {
        return;
      }
      const filtered = applyMenu(payload, roles);
      const items = filtered.length > 0 ? filtered : fallback;
      setState({
        items,
        loading: false,
        error: null,
        unauthorized: false,
        forbidden: false,
      });
    } catch (error) {
      if (controller.signal.aborted) {
        return;
      }
      const apiError: ApiError = isApiError(error)
        ? (error as ApiError)
        : ({ type: 'network', message: (error as Error)?.message ?? null } as ApiError);
      if (apiError.type === 'status' && apiError.status === 401) {
        options.onUnauthorized?.();
        setState({
          items: fallback,
          loading: false,
          error: apiError,
          unauthorized: true,
          forbidden: false,
        });
        return;
      }
      if (apiError.type === 'status' && apiError.status === 403) {
        setState({
          items: fallback,
          loading: false,
          error: apiError,
          unauthorized: false,
          forbidden: true,
        });
        return;
      }
      setState({
        items: fallback,
        loading: false,
        error: apiError,
        unauthorized: false,
        forbidden: false,
      });
    } finally {
      if (requestRef.current === controller) {
        requestRef.current = null;
      }
    }
  }, [applyMenu, client, options.onUnauthorized, roleSignature, session, sessionToken]);

  useEffect(() => {
    void loadMenu();
  }, [loadMenu, roleSignature]);

  useEffect(() => {
    return () => {
      requestRef.current?.abort();
    };
  }, []);

  const refresh = useCallback(async () => {
    await loadMenu();
  }, [loadMenu]);

  return useMemo(
    () => ({
      state,
      refresh,
    }),
    [state, refresh]
  );
}

export type {
  NavigationController,
  NavigationMenuItem,
  NavigationState,
  NavigationControllerOptions,
};
