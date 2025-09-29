import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from '../../vendor/react/index.js';
import {
  executePlanNodeAction,
  fetchPlanDetail,
  updatePlanReminder,
  type PlanNodeActionKind,
  type PlanNodeActionRequest,
} from '../api/plans';
import type { ApiClient, ApiError } from '../api/client';
import type {
  LoginResponse,
  PlanDetail,
  PlanDetailPayload,
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
  mutation: PlanDetailMutationState;
};

export type PlanDetailController = {
  state: PlanDetailState;
  selectPlan: (planId: string | null) => Promise<void>;
  refresh: () => Promise<void>;
  clear: () => void;
  retain: (planIds: readonly string[]) => void;
  executeNodeAction: (input: PlanNodeActionInput) => Promise<void>;
  updateReminder: (input: PlanReminderUpdateInput) => Promise<void>;
};

export type PlanNodeActionInput =
  | {
      planId: string;
      nodeId: string;
      type: 'start';
      operatorId: string;
    }
  | {
      planId: string;
      nodeId: string;
      type: 'complete';
      operatorId: string;
      resultSummary?: string | null;
    }
  | {
      planId: string;
      nodeId: string;
      type: 'handover';
      operatorId: string;
      assigneeId: string;
      comment?: string | null;
    };

export type PlanReminderUpdateInput = {
  planId: string;
  reminderId: string;
  active: boolean;
  offsetMinutes?: number | null;
};

export type PlanDetailMutationContext =
  | {
      type: 'node';
      nodeId: string;
      action: PlanNodeActionKind;
    }
  | {
      type: 'reminder';
      reminderId: string;
      action: 'toggle' | 'edit';
    };

export type PlanDetailMutationState = {
  status: 'idle' | 'loading' | 'success' | 'error';
  context: PlanDetailMutationContext | null;
  error: ApiError | null;
  completedAt: string | null;
};

const INITIAL_MUTATION_STATE: PlanDetailMutationState = {
  status: 'idle',
  context: null,
  error: null,
  completedAt: null,
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
  mutation: INITIAL_MUTATION_STATE,
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

  const commitPlanDetail = useCallback(
    (planId: string, payload: PlanDetailPayload, origin: 'cache' | 'network', fetchedAt: number) => {
      setState((current) => ({
        ...current,
        activePlanId: planId,
        detail: payload.detail,
        timeline: payload.timeline,
        reminders: payload.reminders,
        status: 'success',
        error: null,
        lastUpdated: new Date(fetchedAt).toISOString(),
        origin,
      }));
    },
    []
  );

  const writeStateFromCache = useCallback(
    (planId: string, cacheEntry: PlanDetailCacheEntry) => {
      commitPlanDetail(planId, cacheEntry.payload, 'cache', cacheEntry.fetchedAt);
    },
    [commitPlanDetail]
  );

  const persistPlanDetail = useCallback(
    (planId: string, payload: PlanDetailPayload, fetchedAt: number) => {
      const cache = ensurePlanDetailCache(cacheRef);
      const entry: PlanDetailCacheEntry = { payload, fetchedAt };
      cache.set(planId, entry);
      evictPlanDetailCacheEntries(cache, PLAN_DETAIL_CACHE_LIMIT);
    },
    [cacheRef]
  );

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
        persistPlanDetail(planId, payload, fetchedAt);
        commitPlanDetail(planId, payload, 'network', fetchedAt);
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
    [client, commitPlanDetail, persistPlanDetail, session, writeStateFromCache]
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
        setState((current) => ({
          ...current,
          mutation: INITIAL_MUTATION_STATE,
        }));
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
          mutation: INITIAL_MUTATION_STATE,
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

  const executeNodeAction = useCallback(
    async (input: PlanNodeActionInput) => {
      if (!session) {
        return;
      }
      const mutationContext: PlanDetailMutationContext = {
        type: 'node',
        nodeId: input.nodeId,
        action: input.type,
      };
      setState((current) => ({
        ...current,
        mutation: { status: 'loading', context: mutationContext, error: null, completedAt: null },
      }));
      try {
        const request: PlanNodeActionRequest = (() => {
          switch (input.type) {
            case 'start':
              return { type: 'start', operatorId: input.operatorId } as const;
            case 'complete':
              return {
                type: 'complete',
                operatorId: input.operatorId,
                resultSummary: input.resultSummary ?? null,
              } as const;
            case 'handover':
              return {
                type: 'handover',
                operatorId: input.operatorId,
                assigneeId: input.assigneeId,
                comment: input.comment ?? null,
              } as const;
          }
        })();
        const payloadRaw = await executePlanNodeAction(
          client,
          session.token,
          input.planId,
          input.nodeId,
          request
        );
        const payload = normalizePlanDetailPayload(payloadRaw);
        const fetchedAt = Date.now();
        persistPlanDetail(input.planId, payload, fetchedAt);
        commitPlanDetail(input.planId, payload, 'network', fetchedAt);
        setState((current) => ({
          ...current,
          mutation: {
            status: 'success',
            context: mutationContext,
            error: null,
            completedAt: new Date(fetchedAt).toISOString(),
          },
        }));
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          setState((current) => ({
            ...current,
            mutation: INITIAL_MUTATION_STATE,
          }));
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setState((current) => ({
          ...current,
          mutation: {
            status: 'error',
            context: mutationContext,
            error: apiError,
            completedAt: new Date().toISOString(),
          },
        }));
        throw apiError;
      }
    },
    [client, commitPlanDetail, persistPlanDetail, session]
  );

  const updateReminder = useCallback(
    async (input: PlanReminderUpdateInput) => {
      if (!session) {
        return;
      }
      const mutationContext: PlanDetailMutationContext = {
        type: 'reminder',
        reminderId: input.reminderId,
        action: typeof input.offsetMinutes === 'number' ? 'edit' : 'toggle',
      };
      setState((current) => ({
        ...current,
        mutation: { status: 'loading', context: mutationContext, error: null, completedAt: null },
      }));
      try {
        const request = {
          active: input.active,
          offsetMinutes: input.offsetMinutes,
        };
        const payloadRaw = await updatePlanReminder(
          client,
          session.token,
          input.planId,
          input.reminderId,
          request
        );
        const payload = normalizePlanDetailPayload(payloadRaw);
        const fetchedAt = Date.now();
        persistPlanDetail(input.planId, payload, fetchedAt);
        commitPlanDetail(input.planId, payload, 'network', fetchedAt);
        setState((current) => ({
          ...current,
          mutation: {
            status: 'success',
            context: mutationContext,
            error: null,
            completedAt: new Date(fetchedAt).toISOString(),
          },
        }));
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          setState((current) => ({
            ...current,
            mutation: INITIAL_MUTATION_STATE,
          }));
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setState((current) => ({
          ...current,
          mutation: {
            status: 'error',
            context: mutationContext,
            error: apiError,
            completedAt: new Date().toISOString(),
          },
        }));
        throw apiError;
      }
    },
    [client, commitPlanDetail, persistPlanDetail, session]
  );

  const controllerValue = useMemo<PlanDetailController>(() => ({
    state,
    selectPlan,
    refresh,
    clear: resetState,
    retain,
    executeNodeAction,
    updateReminder,
  }), [executeNodeAction, refresh, retain, resetState, selectPlan, state, updateReminder]);

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
