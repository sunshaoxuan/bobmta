import {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
} from '../react/index.js';

const DEFAULT_LOCATION = {
  pathname: '/',
  search: '',
  hash: '',
};

let currentRouterState = {
  location: DEFAULT_LOCATION,
  navigate: () => {},
};
const routerSubscribers = new Set();

function notifyRouterSubscribers(nextState) {
  currentRouterState = nextState;
  routerSubscribers.forEach((listener) => {
    try {
      listener(nextState);
    } catch (error) {
      console.error('Router subscriber failed', error);
    }
  });
}

function subscribeRouter(listener) {
  routerSubscribers.add(listener);
  listener(currentRouterState);
  return () => {
    routerSubscribers.delete(listener);
  };
}

function readWindowLocation() {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCATION;
  }
  return {
    pathname: window.location.pathname || '/',
    search: window.location.search || '',
    hash: window.location.hash || '',
  };
}

function normalizePathname(pathname) {
  if (!pathname) {
    return '/';
  }
  if (!pathname.startsWith('/')) {
    return `/${pathname}`;
  }
  return pathname;
}

function resolveNavigateTarget(base, target, options) {
  const normalizedBase = {
    pathname: normalizePathname(base?.pathname || '/'),
    search: base?.search || '',
    hash: base?.hash || '',
  };
  let nextPathname = normalizedBase.pathname;
  let nextSearch = normalizedBase.search;
  let nextHash = normalizedBase.hash;
  if (typeof target === 'string') {
    nextPathname = normalizePathname(target || '/');
    if (!options?.preserveSearch) {
      nextSearch = '';
    }
    if (!options?.preserveHash) {
      nextHash = '';
    }
  } else if (target && typeof target === 'object') {
    if (typeof target.pathname === 'string') {
      nextPathname = normalizePathname(target.pathname || '/');
      if (!options?.preserveSearch && target.search === undefined) {
        nextSearch = '';
      }
      if (!options?.preserveHash && target.hash === undefined) {
        nextHash = '';
      }
    }
    if (typeof target.search === 'string') {
      nextSearch = target.search;
    }
    if (typeof target.hash === 'string') {
      nextHash = target.hash;
    }
  }
  if (options?.preserveSearch) {
    nextSearch = normalizedBase.search;
  }
  if (options?.preserveHash) {
    nextHash = normalizedBase.hash;
  }
  return {
    pathname: normalizePathname(nextPathname || '/'),
    search: nextSearch || '',
    hash: nextHash || '',
  };
}

function createBrowserNavigate(setLocation, locationRef) {
  return (target, options) => {
    const base = typeof window === 'undefined' ? locationRef.current : readWindowLocation();
    const nextLocation = resolveNavigateTarget(base, target, options);
    const current = locationRef.current ?? DEFAULT_LOCATION;
    if (
      nextLocation.pathname === current.pathname &&
      nextLocation.search === current.search &&
      nextLocation.hash === current.hash
    ) {
      return;
    }
    if (typeof window !== 'undefined') {
      const url = `${nextLocation.pathname}${nextLocation.search}${nextLocation.hash}`;
      if (options?.replace) {
        window.history.replaceState(null, '', url);
      } else {
        window.history.pushState(null, '', url);
      }
    }
    locationRef.current = nextLocation;
    setLocation(nextLocation);
  };
}

export function BrowserRouter({ children }) {
  const [location, setLocation] = useState(() => readWindowLocation());
  const locationRef = useRef(location);
  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  const navigate = useMemo(() => createBrowserNavigate(setLocation, locationRef), []);
  const value = useMemo(
    () => ({
      location,
      navigate,
    }),
    [location, navigate]
  );

  useEffect(() => {
    if (typeof window === 'undefined') {
      return undefined;
    }
    const handlePopState = () => {
      const next = readWindowLocation();
      locationRef.current = next;
      setLocation(next);
    };
    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, []);

  useEffect(() => {
    notifyRouterSubscribers(value);
  }, [value]);

  useEffect(
    () => () => {
      notifyRouterSubscribers({
        location: DEFAULT_LOCATION,
        navigate: () => {},
      });
    },
    []
  );

  return children ?? null;
}

export function MemoryRouter({ children, initialEntries, initialIndex }) {
  const normalizedEntries = (Array.isArray(initialEntries) && initialEntries.length > 0
    ? initialEntries
    : ['/']
  ).map((entry) =>
    resolveNavigateTarget(
      DEFAULT_LOCATION,
      typeof entry === 'string' ? entry : entry?.pathname,
      {}
    )
  );
  const startIndex = Math.min(
    Math.max(typeof initialIndex === 'number' ? initialIndex : normalizedEntries.length - 1, 0),
    normalizedEntries.length - 1
  );
  const [historyState, setHistoryState] = useState(() => ({
    entries: normalizedEntries,
    index: startIndex,
  }));
  const stateRef = useRef(historyState);
  useEffect(() => {
    stateRef.current = historyState;
  }, [historyState]);

  const navigate = useCallback((target, options) => {
    setHistoryState((prev) => {
      const base = prev.entries[prev.index] ?? DEFAULT_LOCATION;
      const nextLocation = resolveNavigateTarget(base, target, options);
      const current = prev.entries[prev.index] ?? DEFAULT_LOCATION;
      if (
        nextLocation.pathname === current.pathname &&
        nextLocation.search === current.search &&
        nextLocation.hash === current.hash
      ) {
        return prev;
      }
      if (options?.replace) {
        const updated = prev.entries.slice();
        updated[prev.index] = nextLocation;
        return { entries: updated, index: prev.index };
      }
      const nextEntries = prev.entries.slice(0, prev.index + 1);
      nextEntries.push(nextLocation);
      return { entries: nextEntries, index: nextEntries.length - 1 };
    });
  }, []);

  const location = historyState.entries[historyState.index] ?? DEFAULT_LOCATION;
  const value = useMemo(
    () => ({
      location,
      navigate,
    }),
    [location, navigate]
  );

  useEffect(() => {
    notifyRouterSubscribers(value);
  }, [value]);

  useEffect(
    () => () => {
      notifyRouterSubscribers({
        location: DEFAULT_LOCATION,
        navigate: () => {},
      });
    },
    []
  );

  return children ?? null;
}

function useRouterState() {
  const [state, setState] = useState(currentRouterState);
  useEffect(() => subscribeRouter(setState), []);
  return state;
}

export function useNavigate() {
  const state = useRouterState();
  return state.navigate;
}

export function useLocation() {
  const state = useRouterState();
  return state.location;
}

function normalizeSegments(path) {
  const trimmed = (path || '').replace(/^\/+|\/+$/g, '');
  if (!trimmed) {
    return [];
  }
  return trimmed.split('/');
}

function matchPath(pathPattern, pathname) {
  if (!pathPattern) {
    return false;
  }
  if (pathPattern === '*') {
    return true;
  }
  const normalizedPattern = normalizePathname(pathPattern);
  const patternSegments = normalizeSegments(normalizedPattern);
  const pathnameSegments = normalizeSegments(normalizePathname(pathname));
  if (patternSegments.length === 0) {
    return pathnameSegments.length === 0;
  }
  if (patternSegments.length !== pathnameSegments.length) {
    return false;
  }
  for (let index = 0; index < patternSegments.length; index += 1) {
    const patternSegment = patternSegments[index];
    const pathnameSegment = pathnameSegments[index];
    if (patternSegment.startsWith(':')) {
      if (!pathnameSegment) {
        return false;
      }
      continue;
    }
    if (patternSegment !== pathnameSegment) {
      return false;
    }
  }
  return true;
}

export function Routes({ children }) {
  const location = useLocation();
  const childArray = Array.isArray(children) ? children : children ? [children] : [];
  for (let index = 0; index < childArray.length; index += 1) {
    const child = childArray[index];
    if (!child || child.type !== Route) {
      continue;
    }
    const { path, element } = child.props ?? {};
    if (matchPath(path, location.pathname)) {
      return element ?? null;
    }
  }
  return null;
}

export function Route() {
  return null;
}

export function useRoutes(routes) {
  const location = useLocation();
  for (let index = 0; index < routes.length; index += 1) {
    const route = routes[index];
    if (matchPath(route.path, location.pathname)) {
      return route.element ?? null;
    }
  }
  return null;
}

export function createRoutesFromChildren(children) {
  const childArray = Array.isArray(children) ? children : children ? [children] : [];
  return childArray
    .filter((child) => child && child.type === Route)
    .map((child) => ({ path: child.props?.path ?? '*', element: child.props?.element ?? null }));
}

export default {
  BrowserRouter,
  MemoryRouter,
  Routes,
  Route,
  useNavigate,
  useLocation,
  useRoutes,
  createRoutesFromChildren,
};
