import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from '../../vendor/react/index.js';
import { fetchPlanDetail } from '../api/plans';
import type { ApiClient, ApiError } from '../api/client';
import type {
  LoginResponse,
  PlanDetail,
  PlanReminderSummary,
  PlanTimelineEntry,
} from '../api/types';
import {
  PLAN_DETAIL_CACHE_LIMIT,
  PLAN_DETAIL_CACHE_TTL_MS,
  type PlanDetailCacheEntry,
  evictPlanDetailCacheEntries,
  isPlanDetailCacheEntryFresh,
  prunePlanDetailCache,
} from './planDetailCache';
import { normalizePlanDetailPayload } from './planDetailNormalizer';

export type PlanDetailState = {
  activePlanId: string | null;
  detail: PlanDetail | null;
  timeline: PlanTimelineEntry[];
  reminders: PlanReminderSummary[];
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  lastUpdated: string | null;
  origin: 'cache' | 'network' | null;
};

export type PlanDetailController = {
  state: PlanDetailState;
  selectPlan: (planId: string | null) => Promise<void>;
  refresh: () => Promise<void>;
  clear: () => void;
  retain: (planIds: readonly string[]) => void;
};

const INITIAL_STATE: PlanDetailState = {
  activePlanId: null,
  detail: null,
  timeline: [],
  reminders: [],
  status: 'idle',
  error: null,
  lastUpdated: null,
  origin: null,
};

export function usePlanDetailController(
  client: ApiClient,
  session: LoginResponse | null
): PlanDetailController {
  const [state, setState] = useState<PlanDetailState>(INITIAL_STATE);
  const cacheRef = useRef<Map<string, PlanDetailCacheEntry> | null>(new Map());
  const abortRef = useRef<AbortController | null>(null);

  const resetState = useCallback(() => {
    abortCurrentRequest(abortRef);
    ensurePlanDetailCache(cacheRef).clear();
    setState(INITIAL_STATE);
  }, []);

  useEffect(() => {
    if (!session) {
      resetState();
    }
  }, [session, resetState]);

  const writeStateFromCache = useCallback((planId: string, cacheEntry: PlanDetailCacheEntry) => {
    setState({
      activePlanId: planId,
      detail: cacheEntry.payload.detail,
      timeline: cacheEntry.payload.timeline,
      reminders: cacheEntry.payload.reminders,
      status: 'success',
      error: null,
      lastUpdated: new Date(cacheEntry.fetchedAt).toISOString(),
      origin: 'cache',
    });
  }, []);

  const loadPlan = useCallback(
    async (planId: string, options: { force?: boolean } = {}) => {
      if (!session) {
        return;
      }
      const cache = ensurePlanDetailCache(cacheRef);
      const cached = cache.get(planId) ?? null;
      const now = Date.now();
      const cacheValid =
        isPlanDetailCacheEntryFresh(cached, now, PLAN_DETAIL_CACHE_TTL_MS) && !options.force;
      if (cacheValid && cached) {
        writeStateFromCache(planId, cached);
        return;
      }

      abortCurrentRequest(abortRef);
      const controller = new AbortController();
      abortRef.current = controller;

      setState((current) => ({
        ...current,
        activePlanId: planId,
        status: 'loading',
        error: null,
        origin: null,
      }));

      try {
        const payloadRaw = await fetchPlanDetail(client, session.token, planId, {
          signal: controller.signal,
        });
        const payload = normalizePlanDetailPayload(payloadRaw);
        const fetchedAt = Date.now();
        const entry: PlanDetailCacheEntry = { payload, fetchedAt };
        cache.set(planId, entry);
        evictPlanDetailCacheEntries(cache, PLAN_DETAIL_CACHE_LIMIT);
        setState({
          activePlanId: planId,
          detail: payload.detail,
          timeline: payload.timeline,
          reminders: payload.reminders,
          status: 'success',
          error: null,
          lastUpdated: new Date(fetchedAt).toISOString(),
          origin: 'network',
        });
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
          activePlanId: planId,
          status: 'idle',
          error: apiError,
          origin: null,
        }));
      }
    },
    [client, session, writeStateFromCache]
  );

  const selectPlan = useCallback(
    async (planId: string | null) => {
      if (!planId) {
        setState(INITIAL_STATE);
        return;
      }
      if (!session) {
        setState({
          ...INITIAL_STATE,
          activePlanId: planId,
        });
        return;
      }
      const cache = ensurePlanDetailCache(cacheRef);
      const cached = cache.get(planId);
      if (cached) {
        writeStateFromCache(planId, cached);
      } else {
        setState((current) => ({
          ...current,
          activePlanId: planId,
          detail: null,
          timeline: [],
          reminders: [],
          status: 'loading',
          error: null,
          origin: null,
        }));
      }
      await loadPlan(planId, { force: false });
    },
    [loadPlan, session, writeStateFromCache]
  );

  const refresh = useCallback(async () => {
    if (!state.activePlanId || !session) {
      return;
    }
    await loadPlan(state.activePlanId, { force: true });
  }, [loadPlan, session, state.activePlanId]);

  const retain = useCallback((planIds: readonly string[]) => {
    const cache = ensurePlanDetailCache(cacheRef);
    const allowed = new Set(planIds);
    for (const key of Array.from(cache.keys())) {
      if (!allowed.has(key)) {
        cache.delete(key);
      }
    }
    prunePlanDetailCache(cache, planIds, PLAN_DETAIL_CACHE_LIMIT);
  }, []);

  const controllerValue = useMemo<PlanDetailController>(() => ({
    state,
    selectPlan,
    refresh,
    clear: resetState,
    retain,
  }), [refresh, retain, resetState, selectPlan, state]);

  return controllerValue;
}

function ensurePlanDetailCache(
  ref: { current: Map<string, PlanDetailCacheEntry> | null }
): Map<string, PlanDetailCacheEntry> {
  if (!ref.current) {
    ref.current = new Map();
  }
  return ref.current;
}

function abortCurrentRequest(ref: { current: AbortController | null }) {
  if (ref.current) {
    ref.current.abort();
    ref.current = null;
  }
}
