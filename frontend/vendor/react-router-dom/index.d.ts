import type { FunctionComponent, ReactNode } from '../react/index.js';

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

export type RouterLocation = {
  pathname: string;
  search: string;
  hash: string;
};

export type NavigateFunction = (target?: NavigateTarget, options?: NavigateOptions) => void;

export type RouterState = {
  location: RouterLocation;
  navigate: NavigateFunction;
};

export type BrowserRouterProps = {
  children?: ReactNode;
};

export declare const BrowserRouter: FunctionComponent<BrowserRouterProps>;

export type MemoryRouterProps = {
  children?: ReactNode;
  initialEntries?: Array<string | { pathname?: string; search?: string; hash?: string }>;
  initialIndex?: number;
};

export declare const MemoryRouter: FunctionComponent<MemoryRouterProps>;

export declare function useNavigate(): NavigateFunction;

export declare function useLocation(): RouterLocation;

export type RouteObject = {
  path: string;
  element: ReactNode;
};

export type RouteProps = {
  path: string;
  element: ReactNode;
};

export declare const Routes: FunctionComponent<{ children?: ReactNode }>;

export declare const Route: FunctionComponent<RouteProps>;

export declare function useRoutes(routes: RouteObject[]): ReactNode;

export declare function createRoutesFromChildren(children: ReactNode): RouteObject[];
