let currentHooks = null;
let hookIndex = 0;
let pendingEffects = [];
let rerender = () => {};

const cloneDeps = (deps) => (deps ? deps.map((dep) => dep) : undefined);

function ensureHooks() {
  if (!currentHooks) {
    throw new Error('Hooks can only be called inside a component.');
  }
}

function readHook() {
  ensureHooks();
  const position = hookIndex;
  hookIndex += 1;
  if (!currentHooks[position]) {
    currentHooks[position] = {};
  }
  return currentHooks[position];
}

export function prepareHooks(hooksArray) {
  currentHooks = hooksArray;
  hookIndex = 0;
}

export function resetHooks() {
  currentHooks = null;
  hookIndex = 0;
}

export function queueEffects(effects) {
  pendingEffects.push(...effects);
}

export function collectEffects() {
  const effects = pendingEffects;
  pendingEffects = [];
  return effects;
}

export function setRerender(callback) {
  rerender = callback;
}

export function useState(initialState) {
  const hook = readHook();
  if (!('state' in hook)) {
    hook.state = typeof initialState === 'function' ? initialState() : initialState;
  }
  const setState = (value) => {
    const next = typeof value === 'function' ? value(hook.state) : value;
    if (!Object.is(next, hook.state)) {
      hook.state = next;
      rerender();
    }
  };
  return [hook.state, setState];
}

export function useEffect(effect, deps) {
  const hook = readHook();
  const previous = hook.deps;
  const changed =
    !previous ||
    !deps ||
    deps.length !== previous.length ||
    deps.some((dep, index) => !Object.is(dep, previous[index]));
  if (changed) {
    pendingEffects.push(() => {
      if (typeof hook.cleanup === 'function') {
        try {
          hook.cleanup();
        } catch (error) {
          console.error('Effect cleanup failed', error);
        }
      }
      hook.cleanup = effect() || undefined;
      hook.deps = cloneDeps(deps);
    });
  }
}

export function useMemo(factory, deps) {
  const hook = readHook();
  const previous = hook.deps;
  const changed =
    !previous ||
    !deps ||
    deps.length !== previous.length ||
    deps.some((dep, index) => !Object.is(dep, previous[index]));
  if (changed) {
    hook.value = factory();
    hook.deps = cloneDeps(deps);
  }
  return hook.value;
}

export function useCallback(callback, deps) {
  return useMemo(() => callback, deps);
}

export function useRef(initialValue) {
  const hook = readHook();
  if (!hook.ref) {
    hook.ref = { current: initialValue };
  }
  return hook.ref;
}

export function finalizeHooks() {
  const effects = collectEffects();
  resetHooks();
  return effects;
}
