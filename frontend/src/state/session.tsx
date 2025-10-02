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
  children?: SessionNavigationItem[];
};

export type SessionNavigationMenuConfigItem = {
  key: string;
  path: string;
  labelKey: UiMessageKey;
  roles: string[];
  children?: SessionNavigationMenuConfigItem[];
};

export type SessionNavigationState = {
  items: SessionNavigationItem[];
  config: SessionNavigationMenuConfigItem[];
  pathMap: Record<string, string>;
  paths: string[];
  loading: boolean;
  error: ApiError | null;
  source: 'mock' | 'remote';
  unauthorized: boolean;
  forbidden: boolean;
};

export type SessionUserMenuAction = {
  key: string;
  labelKey: UiMessageKey;
  labelValues?: Record<string, string | number>;
  disabled?: boolean;
};

export type SessionUserMenuDivider = {
  key: string;
  type: 'divider';
};

export type SessionUserMenuItem = SessionUserMenuAction | SessionUserMenuDivider;

export type SessionUserMenuState = {
  items: SessionUserMenuItem[];
  source: 'guest' | 'session';
};

export type SessionPermissionFlags = {
  canAccessOperations: boolean;
  canManagePlans: boolean;
};

export type SessionPermissionsState = {
  roles: string[];
  normalizedRoles: string[];
  flags: SessionPermissionFlags;
  source: 'guest' | 'session';
};

export type SessionState = {
  session: LoginResponse | null;
  status: 'idle' | 'loading' | 'authenticated';
  error: ApiError | null;
  navigation: SessionNavigationState;
  userMenu: SessionUserMenuState;
  permissions: SessionPermissionsState;
};

export type SessionController = {
  state: SessionState;
  login: (credentials: { username: string; password: string }) => Promise<void>;
  logout: () => void;
  userMenu: SessionUserMenuState;
  permissions: SessionPermissionsState;
  navigationMenu: SessionNavigationMenuConfigItem[];
  navigationItems: SessionNavigationItem[];
  navigationPathMap: Record<string, string>;
  navigationPaths: string[];
  canAccessPath: (path: string) => boolean;
};

const normalizePath = (path: string): string => {
  if (!path) {
    return '/';
  }
  return path.startsWith('/') ? path : `/${path}`;
};

const normalizeRoles = (roles: string[]): string[] => {
  const normalized = roles
    .map((role) => role.trim().toUpperCase())
    .filter((role) => role.length > 0);
  return Array.from(new Set(normalized));
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
      children: children.length > 0 ? children : undefined,
    };
  };

  return items
    .map((item) => mapItem(item))
    .filter((item): item is SessionNavigationItem => Boolean(item));
};

const normalizeNavigationConfig = (
  items: NavigationMenuPayload[]
): SessionNavigationMenuConfigItem[] =>
  items.map((item) => {
    const normalizedChildren = item.children ? normalizeNavigationConfig(item.children) : undefined;
    return {
      key: item.key,
      path: normalizePath(item.path),
      labelKey: item.labelKey,
      roles: normalizeRoles(item.roles ?? []),
      children: normalizedChildren && normalizedChildren.length > 0 ? normalizedChildren : undefined,
    };
  });

const defaultNavigationConfig: SessionNavigationMenuConfigItem[] = normalizeNavigationConfig(
  MOCK_NAVIGATION_MENU
);

const createEmptyNavigationAccess = (): { pathMap: Record<string, string>; paths: string[] } => ({
  pathMap: {},
  paths: [],
});

const buildNavigationAccessArtifacts = (
  items: SessionNavigationItem[]
): { pathMap: Record<string, string>; paths: string[] } => {
  if (items.length === 0) {
    return createEmptyNavigationAccess();
  }
  const pathMap: Record<string, string> = {};
  const pathSet = new Set<string>();
  const visit = (list: SessionNavigationItem[]) => {
    list.forEach((item) => {
      const normalizedPath = normalizePath(item.path);
      pathMap[item.key] = normalizedPath;
      pathSet.add(normalizedPath);
      if (item.children && item.children.length > 0) {
        visit(item.children);
      }
    });
  };
  visit(items);
  return {
    pathMap,
    paths: Array.from(pathSet),
  };
};

const ROLE_BASED_USER_MENU_ITEMS: Record<string, SessionUserMenuAction[]> = {
  ADMIN: [{ key: 'user-menu-operations', labelKey: 'navMenuOperations' }],
  PLANNER: [
    { key: 'user-menu-plans', labelKey: 'planSectionTitle' },
    { key: 'user-menu-calendar', labelKey: 'planDetailTimelineTitle' },
  ],
  OPERATOR: [{ key: 'user-menu-reminders', labelKey: 'planDetailRemindersTitle' }],
};

const buildUserMenu = (session: LoginResponse | null): SessionUserMenuState => {
  if (!session) {
    return { items: [], source: 'guest' };
  }
  const normalizedRoles = normalizeRoles(session.roles ?? []);
  const derived: SessionUserMenuAction[] = [];
  const seen = new Set<string>();
  normalizedRoles.forEach((role) => {
    const items = ROLE_BASED_USER_MENU_ITEMS[role];
    if (!items) {
      return;
    }
    items.forEach((item) => {
      if (seen.has(item.key)) {
        return;
      }
      seen.add(item.key);
      derived.push({ ...item });
    });
  });
  const menuItems: SessionUserMenuItem[] = [
    {
      key: 'user-menu-profile',
      labelKey: 'authWelcome',
      labelValues: { name: session.displayName },
      disabled: true,
    },
  ];
  if (derived.length > 0) {
    menuItems.push({ key: 'user-menu-divider', type: 'divider' });
    menuItems.push(...derived);
  }
  menuItems.push({ key: 'logout', labelKey: 'authLogout' });
  return { items: menuItems, source: 'session' };
};

const buildPermissions = (session: LoginResponse | null): SessionPermissionsState => {
  const roles = session?.roles ?? [];
  const normalizedRoles = normalizeRoles(roles);
  const canAccessOperations = normalizedRoles.some((role) =>
    ['ADMIN', 'PLANNER', 'OPERATOR'].includes(role)
  );
  const canManagePlans = normalizedRoles.some((role) => ['ADMIN', 'PLANNER'].includes(role));
  return {
    roles,
    normalizedRoles,
    flags: {
      canAccessOperations,
      canManagePlans,
    },
    source: session ? 'session' : 'guest',
  };
};

const deriveSessionArtifacts = (session: LoginResponse | null) => ({
  userMenu: buildUserMenu(session),
  permissions: buildPermissions(session),
});

const initialState: SessionState = {
  session: null,
  status: 'idle',
  error: null,
  navigation: {
    items: [],
    config: defaultNavigationConfig,
    ...createEmptyNavigationAccess(),
    loading: false,
    error: null,
    source: 'mock',
    unauthorized: false,
    forbidden: false,
  },
  ...deriveSessionArtifacts(null),
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
          ...deriveSessionArtifacts(session),
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
          ...deriveSessionArtifacts(null),
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
    const fallbackConfig = defaultNavigationConfig;
    const fallbackAccess = buildNavigationAccessArtifacts(fallbackItems);

    if (!hasSession) {
      navigationRequestRef.current?.abort();
      navigationRequestRef.current = null;
      setState((current) => ({
        ...current,
        navigation: {
          items: [],
          config: fallbackConfig,
          ...createEmptyNavigationAccess(),
          loading: false,
          error: null,
          source: 'mock',
          unauthorized: false,
          forbidden: false,
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
        unauthorized: false,
        forbidden: false,
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
        const normalizedPayload = normalizeNavigationConfig(payload);
        const items = filtered.length > 0 ? filtered : fallbackItems;
        const config = normalizedPayload.length > 0 ? normalizedPayload : fallbackConfig;
        const filteredAccess = buildNavigationAccessArtifacts(filtered);
        const access = filtered.length > 0 ? filteredAccess : fallbackAccess;
        setState((current) => ({
          ...current,
          navigation: {
            items,
            config,
            pathMap: access.pathMap,
            paths: access.paths,
            loading: false,
            error: null,
            source: filtered.length > 0 ? 'remote' : 'mock',
            unauthorized: false,
            forbidden: false,
          },
        }));
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }
        const apiError: ApiError = isApiError(error)
          ? (error as ApiError)
          : ({ type: 'network', message: (error as Error)?.message ?? null } as ApiError);
        const status = apiError.type === 'status' ? apiError.status : null;
        const unauthorized = status === 401;
        const forbidden = status === 403;
        if (unauthorized) {
          setState((current) => ({
            ...current,
            session: null,
            status: 'idle',
            error: apiError,
            navigation: {
              items: [],
              config: fallbackConfig,
              ...createEmptyNavigationAccess(),
              loading: false,
              error: apiError,
              source: 'mock',
              unauthorized: true,
              forbidden: false,
            },
            ...deriveSessionArtifacts(null),
          }));
          return;
        }
        setState((current) => ({
          ...current,
          navigation: {
            items: fallbackItems,
            config: fallbackConfig,
            pathMap: fallbackAccess.pathMap,
            paths: fallbackAccess.paths,
            loading: false,
            error: apiError,
            source: 'mock',
            unauthorized: false,
            forbidden,
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

  const canAccessPath = useCallback(
    (path: string) => {
      if (!state.session) {
        return false;
      }
      if (state.navigation.paths.length === 0) {
        return false;
      }
      const normalizedTarget = normalizePath(path);
      return state.navigation.paths.some((allowedPath) => {
        if (normalizedTarget === allowedPath) {
          return true;
        }
        if (allowedPath === '/') {
          return normalizedTarget === '/';
        }
        return normalizedTarget.startsWith(`${allowedPath}/`);
      });
    },
    [state.session, state.navigation.paths]
  );

  return useMemo(
    () => ({
      state,
      login: handleLogin,
      logout: handleLogout,
      userMenu: state.userMenu,
      permissions: state.permissions,
      navigationMenu: state.navigation.config,
      navigationItems: state.navigation.items,
      navigationPathMap: state.navigation.pathMap,
      navigationPaths: state.navigation.paths,
      canAccessPath,
    }),
    [state, handleLogin, handleLogout, canAccessPath]
  );
}
