import { TEXT_ELEMENT, Fragment, StrictMode } from '../react/index.js';
import { prepareHooks, finalizeHooks, setRerender } from '../react/hooks.js';

const hookState = new WeakMap();
let rootContainer = null;
let rootElement = null;
let isRendering = false;
let needsRender = false;

const scheduleCommit = () => {
  if (needsRender) {
    return;
  }
  needsRender = true;
  queueMicrotask(() => {
    if (!needsRender) {
      return;
    }
    needsRender = false;
    commit();
  });
};

setRerender(scheduleCommit);

function applyProp(node, key, value) {
  if (key === 'children' || key === 'ref') {
    return;
  }
  if (key === 'className') {
    node.className = value ?? '';
    return;
  }
  if (key === 'style' && value && typeof value === 'object') {
    for (const [styleKey, styleValue] of Object.entries(value)) {
      if (styleValue === null || styleValue === undefined) {
        node.style.removeProperty(styleKey);
      } else if (typeof styleValue === 'number') {
        node.style[styleKey] = `${styleValue}px`;
      } else {
        node.style[styleKey] = styleValue;
      }
    }
    return;
  }
  if (key.startsWith('on') && typeof value === 'function') {
    const eventName = key.slice(2).toLowerCase();
    node.addEventListener(eventName, value);
    return;
  }
  if (value === false || value === null || value === undefined) {
    node.removeAttribute(key);
    if (key in node) {
      try {
        node[key] = '';
      } catch (error) {
        /* ignore */
      }
    }
    return;
  }
  if (key === 'value' || key === 'checked' || key === 'disabled') {
    try {
      node[key] = value;
    } catch (error) {
      /* ignore */
    }
  }
  node.setAttribute(key, value);
}

function applyProps(node, props) {
  if (!props) {
    return;
  }
  for (const [key, value] of Object.entries(props)) {
    applyProp(node, key, value);
  }
}

function normalizeChildren(children) {
  if (!children) {
    return [];
  }
  if (!Array.isArray(children)) {
    return [children];
  }
  return children;
}

function createNode(element, effects) {
  if (element === null || element === undefined || element === false) {
    return null;
  }
  if (Array.isArray(element)) {
    const fragment = document.createDocumentFragment();
    element.forEach((child) => {
      const node = createNode(child, effects);
      if (node) {
        fragment.appendChild(node);
      }
    });
    return fragment;
  }
  if (element.type === TEXT_ELEMENT) {
    return document.createTextNode(element.props?.nodeValue ?? '');
  }
  if (element.type === Fragment || element.type === StrictMode) {
    const fragment = document.createDocumentFragment();
    normalizeChildren(element.props?.children).forEach((child) => {
      const node = createNode(child, effects);
      if (node) {
        fragment.appendChild(node);
      }
    });
    return fragment;
  }
  if (typeof element.type === 'function') {
    const component = element.type;
    const hooks = hookState.get(component) ?? [];
    prepareHooks(hooks);
    const result = component({ ...(element.props ?? {}), children: element.props?.children });
    hookState.set(component, hooks);
    const node = createNode(result, effects);
    const hookEffects = finalizeHooks();
    effects.push(...hookEffects);
    return node;
  }
  const domNode = document.createElement(element.type);
  applyProps(domNode, element.props);
  normalizeChildren(element.props?.children).forEach((child) => {
    const node = createNode(child, effects);
    if (node) {
      domNode.appendChild(node);
    }
  });
  return domNode;
}

function commit() {
  if (!rootContainer) {
    return;
  }
  if (isRendering) {
    scheduleCommit();
    return;
  }
  isRendering = true;
  const effects = [];
  const fragment = document.createDocumentFragment();
  if (rootElement) {
    const node = createNode(rootElement, effects);
    if (node) {
      fragment.appendChild(node);
    }
  }
  rootContainer.replaceChildren(fragment);
  isRendering = false;
  effects.forEach((effect) => {
    try {
      effect();
    } catch (error) {
      console.error('Effect execution failed', error);
    }
  });
}

export function render(element, container) {
  rootContainer = container;
  rootElement = element;
  commit();
}

export function createRoot(container) {
  rootContainer = container;
  return {
    render(element) {
      rootElement = element;
      commit();
    },
    unmount() {
      rootElement = null;
      commit();
    },
  };
}

const ReactDOM = {
  createRoot,
  render,
};

export default ReactDOM;
