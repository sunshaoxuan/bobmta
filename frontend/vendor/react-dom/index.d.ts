import type { ReactNode } from 'react';

export interface Root {
  render(element: ReactNode): void;
  unmount(): void;
}

export function createRoot(container: Element | DocumentFragment): Root;
export function render(element: ReactNode, container: Element | DocumentFragment): void;

declare const ReactDOM: {
  createRoot: typeof createRoot;
  render: typeof render;
};

export default ReactDOM;
