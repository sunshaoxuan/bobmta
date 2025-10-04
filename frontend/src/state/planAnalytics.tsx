import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from '../../vendor/react/index.js';
import { fetchPlanAnalytics, type PlanAnalyticsQuery } from '../api/plans.js';
import type { ApiClient, ApiError } from '../api/client.js';
import type {
  LoginResponse,
  PlanAnalyticsOverview,
} from '../api/types.js';
import { createMockPlanAnalyticsOverview } from '../mocks/planAnalytics.js';

export type PlanAnalyticsFilters = {
  ownerId: string | null;
  customerId: string | null;
  tenantId: string | null;
  from: string | null;
  to: string | null;
};

export type PlanAnalyticsState = {
  data: PlanAnalyticsOverview | null;
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  origin: 'mock' | 'network' | null;
  lastUpdated: string | null;
  filters: PlanAnalyticsFilters;
  liveMode: boolean;
};

export type PlanAnalyticsController = {
  state: PlanAnalyticsState;
  refresh: () => Promise<void>;
  applyFilters: (filters: Partial<PlanAnalyticsFilters>) => Promise<void>;
  resetFilters: () => Promise<void>;
  setLiveMode: (enabled: boolean) => void;
};

const INITIAL_FILTERS: PlanAnalyticsFilters = {
  ownerId: null,
  customerId: null,
  tenantId: null,
  from: null,
  to: null,
};

function createInitialState(): PlanAnalyticsState {
  const now = new Date().toISOString();
  return {
    data: createMockPlanAnalyticsOverview(),
    status: 'success',
    error: null,
    origin: 'mock',
    lastUpdated: now,
    filters: { ...INITIAL_FILTERS },
    liveMode: false,
  };
}

function normalizeFilters(
  filters: PlanAnalyticsFilters
): Omit<PlanAnalyticsQuery, 'signal'> {
  const query: Omit<PlanAnalyticsQuery, 'signal'> = {};
  if (filters.ownerId && filters.ownerId.trim()) {
    query.ownerId = filters.ownerId.trim();
  }
  if (filters.customerId && filters.customerId.trim()) {
    query.customerId = filters.customerId.trim();
  }
  if (filters.tenantId && filters.tenantId.trim()) {
    query.tenantId = filters.tenantId.trim();
  }
  if (filters.from && filters.from.trim()) {
    query.from = filters.from;
  }
  if (filters.to && filters.to.trim()) {
    query.to = filters.to;
  }
  return query;
}

export function usePlanAnalyticsController(
  client: ApiClient,
  session: LoginResponse | null
): PlanAnalyticsController {
  const [state, setState] = useState<PlanAnalyticsState>(() => createInitialState());
  const stateRef = useRef<PlanAnalyticsState | null>(state);
  const requestIdRef = useRef<number | null>(0);
  const fallbackRef = useRef<PlanAnalyticsOverview>(createMockPlanAnalyticsOverview());

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const applyMockSnapshot = useCallback(() => {
    setState((prev) => ({
      ...prev,
      data: fallbackRef.current,
      status: 'success',
      error: null,
      origin: 'mock',
      lastUpdated: new Date().toISOString(),
    }));
  }, []);

  const loadWithFilters = useCallback(
    async (filters: PlanAnalyticsFilters, options?: { signal?: AbortSignal }) => {
      const currentState = stateRef.current;
      if (!currentState || !currentState.liveMode || !session?.token) {
        applyMockSnapshot();
        return;
      }

      const requestId = (requestIdRef.current ?? 0) + 1;
      requestIdRef.current = requestId;

      setState((prev) => ({
        ...prev,
        status: 'loading',
        error: null,
      }));

      try {
        const query: PlanAnalyticsQuery = {
          ...normalizeFilters(filters),
          signal: options?.signal,
        };
        const data = await fetchPlanAnalytics(client, session.token, query);
        if (requestId !== requestIdRef.current) {
          return;
        }
        setState((prev) => ({
          ...prev,
          data,
          status: 'success',
          error: null,
          origin: 'network',
          lastUpdated: new Date().toISOString(),
        }));
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        if (requestId !== requestIdRef.current) {
          return;
        }
        setState((prev) => ({
          ...prev,
          status: 'success',
          error: error as ApiError,
          data: fallbackRef.current,
          origin: 'mock',
          lastUpdated: new Date().toISOString(),
        }));
      }
    },
    [client, session, applyMockSnapshot]
  );

  useEffect(() => {
    if (!session?.token) {
      applyMockSnapshot();
      return;
    }
    const latest = stateRef.current;
    if (!latest || !latest.liveMode) {
      return;
    }
    const controller = new AbortController();
    loadWithFilters(latest.filters, { signal: controller.signal }).catch(() => {
      // errors handled in loadWithFilters
    });
    return () => {
      controller.abort();
    };
  }, [session, loadWithFilters, applyMockSnapshot]);

  const refresh = useCallback(async () => {
    const latest = stateRef.current;
    if (!latest) {
      return;
    }
    await loadWithFilters(latest.filters);
  }, [loadWithFilters]);

  const applyFilters = useCallback(
    async (filters: Partial<PlanAnalyticsFilters>) => {
      const latest = stateRef.current;
      const nextFilters = { ...(latest ? latest.filters : INITIAL_FILTERS), ...filters };
      setState((prev) => ({
        ...prev,
        filters: nextFilters,
      }));
      await loadWithFilters(nextFilters);
    },
    [loadWithFilters]
  );

  const resetFilters = useCallback(async () => {
    setState((prev) => ({
      ...prev,
      filters: { ...INITIAL_FILTERS },
    }));
    await loadWithFilters({ ...INITIAL_FILTERS });
  }, [loadWithFilters]);

  const setLiveMode = useCallback(
    (enabled: boolean) => {
      setState((prev) => {
        if (prev.liveMode === enabled) {
          return prev;
        }
        return {
          ...prev,
          liveMode: enabled,
        };
      });
      if (enabled) {
        const latest = stateRef.current;
        if (latest) {
          void loadWithFilters(latest.filters);
        }
      } else {
        applyMockSnapshot();
      }
    },
    [loadWithFilters, applyMockSnapshot]
  );

  const controller = useMemo<PlanAnalyticsController>(() => ({
    state,
    refresh,
    applyFilters,
    resetFilters,
    setLiveMode,
  }), [state, refresh, applyFilters, resetFilters, setLiveMode]);

  return controller;
}
