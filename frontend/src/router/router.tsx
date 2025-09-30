import { useCallback, useEffect, useMemo, useRef, useState } from '../../vendor/react/index.js';

export type RouterLocation = {
  pathname: string;
  search: string;
  hash: string;
};

export type NavigateTarget =
  | string
  | {
      pathname?: string;
      search?: string;
      hash?: string;
    };

export type NavigateOptions = {
  replace?: boolean;
  preserveSearch?: boolean;
  preserveHash?: boolean;
};

export type HistoryRouter = {
  location: RouterLocation;
  navigate: (target?: NavigateTarget, options?: NavigateOptions) => void;
};

const DEFAULT_LOCATION: RouterLocation = {
  pathname: '/',
  search: '',
  hash: '',
};

function readWindowLocation(): RouterLocation {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCATION;
  }
  return {
    pathname: window.location.pathname || '/',
    search: window.location.search || '',
    hash: window.location.hash || '',
  };
}

export function useHistoryRouter(): HistoryRouter {
  const [location, setLocation] = useState<RouterLocation>(() => readWindowLocation());
  const locationRef = useRef<RouterLocation>(location);
  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }
    const handlePopState = () => {
      setLocation(readWindowLocation());
    };
    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, []);

  const navigate = useCallback<HistoryRouter['navigate']>((target, options) => {
    const currentLocation = locationRef.current ?? DEFAULT_LOCATION;
    const base =
      typeof window === 'undefined' ? currentLocation : readWindowLocation();
    let nextPathname = base.pathname || '/';
    let nextSearch = base.search || '';
    let nextHash = base.hash || '';
    if (typeof target === 'string') {
      nextPathname = target || '/';
      if (!options?.preserveSearch) {
        nextSearch = '';
      }
      if (!options?.preserveHash) {
        nextHash = '';
      }
    } else if (target) {
      if (typeof target.pathname === 'string') {
        nextPathname = target.pathname || '/';
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
      nextSearch = base.search || '';
    }
    if (options?.preserveHash) {
      nextHash = base.hash || '';
    }
    if (!nextPathname.startsWith('/')) {
      nextPathname = `/${nextPathname}`;
    }
    const current = locationRef.current ?? DEFAULT_LOCATION;
    if (
      nextPathname === current.pathname &&
      nextSearch === current.search &&
      nextHash === current.hash
    ) {
      return;
    }
    if (typeof window !== 'undefined') {
      const url = `${nextPathname}${nextSearch}${nextHash}`;
      if (options?.replace) {
        window.history.replaceState(null, '', url);
      } else {
        window.history.pushState(null, '', url);
      }
    }
    const nextLocation: RouterLocation = {
      pathname: nextPathname,
      search: nextSearch,
      hash: nextHash,
    };
    locationRef.current = nextLocation;
    setLocation(nextLocation);
  }, []);

  return useMemo(
    () => ({
      location,
      navigate,
    }),
    [location, navigate]
  );
}
