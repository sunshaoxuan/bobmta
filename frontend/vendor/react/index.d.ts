export type Key = string | number;

export type ReactText = string | number;
export type ReactChild = ReactElement | ReactText;
export type ReactNode = ReactChild | Iterable<ReactNode> | boolean | null | undefined;

export interface ReactElement<P = any, T = any> {
  type: T;
  props: P & { children?: ReactNode };
  key?: Key | null;
}

export interface FunctionComponent<P = {}> {
  (props: P & { children?: ReactNode }): ReactElement | null;
}

export type FC<P = {}> = FunctionComponent<P>;

export interface RefObject<T> {
  current: T | null;
}

export function createElement<P>(
  type: string | FunctionComponent<P>,
  props?: P | null,
  ...children: ReactNode[]
): ReactElement<P>;

export const Fragment: symbol;
export const StrictMode: FunctionComponent<{ children?: ReactNode }>;

export function useState<S>(initialState: S | (() => S)): [S, (value: S | ((prev: S) => S)) => void];
export function useEffect(effect: () => void | (() => void), deps?: ReadonlyArray<unknown>): void;
export function useMemo<T>(factory: () => T, deps?: ReadonlyArray<unknown>): T;
export function useCallback<T extends (...args: any[]) => any>(callback: T, deps?: ReadonlyArray<unknown>): T;
export function useRef<T>(initialValue: T): RefObject<T>;

export type Dispatch<A> = (value: A) => void;

export type PropsWithChildren<P> = P & { children?: ReactNode };

export interface CSSProperties {
  [key: string]: string | number | undefined;
}

export type ReactEventHandler<T = Element> = (event: Event & { target: T; currentTarget: T }) => void;

export interface HTMLAttributes<T> {
  className?: string;
  style?: CSSProperties;
  onClick?: ReactEventHandler<T>;
  onChange?: ReactEventHandler<T>;
  onInput?: ReactEventHandler<T>;
  onSubmit?: ReactEventHandler<T>;
  onKeyDown?: ReactEventHandler<T>;
  [key: string]: any;
}

export interface DOMAttributes<T> extends HTMLAttributes<T> {}

export interface DetailedHTMLProps<E extends HTMLAttributes<T>, T> extends E {
  children?: ReactNode;
}

export type InputHTMLAttributes<T> = DetailedHTMLProps<HTMLAttributes<T>, T> & {
  value?: string;
  defaultValue?: string;
  type?: string;
  placeholder?: string;
  disabled?: boolean;
  onPressEnter?: (event: KeyboardEvent) => void;
};

export type ButtonHTMLAttributes<T> = DetailedHTMLProps<HTMLAttributes<T>, T> & {
  disabled?: boolean;
};

export type SelectHTMLAttributes<T> = DetailedHTMLProps<HTMLAttributes<T>, T> & {
  value?: string;
};

export type TableHTMLAttributes<T> = DetailedHTMLProps<HTMLAttributes<T>, T>;

export interface MutableRefObject<T> {
  current: T;
}

export type RefCallback<T> = (instance: T | null) => void;

export type Ref<T> = RefObject<T> | RefCallback<T> | null;

export interface ForwardRefExoticComponent<P> {
  (props: P, ref?: Ref<any>): ReactNode;
}

export type ForwardedRef<T> = Ref<T>;

export function forwardRef<T, P = {}>(render: (props: P, ref: Ref<T>) => ReactNode): FunctionComponent<P & { ref?: Ref<T> }>;

declare global {
  namespace JSX {
    type Element = ReactElement;
    interface ElementChildrenAttribute {
      children: {};
    }
    interface IntrinsicElements {
      [elemName: string]: any;
    }
  }
}

declare const React: {
  createElement: typeof createElement;
  Fragment: typeof Fragment;
  StrictMode: typeof StrictMode;
  useState: typeof useState;
  useEffect: typeof useEffect;
  useMemo: typeof useMemo;
  useCallback: typeof useCallback;
  useRef: typeof useRef;
};

export default React;
