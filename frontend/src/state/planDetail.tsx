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
} from '../api/plans.js';
import type { ApiClient, ApiError } from '../api/client.js';
import type {
  LoginResponse,
  PlanDetail,
  PlanDetailPayload,
  PlanReminderSummary,
  PlanTimelineEntry,
  PlanStatus,
} from '../api/types.js';
import {
  PLAN_DETAIL_CACHE_LIMIT,
  PLAN_DETAIL_CACHE_TTL_MS,
  type PlanDetailCacheEntry,
  evictPlanDetailCacheEntries,
  isPlanDetailCacheEntryFresh,
  prunePlanDetailCache,
} from './planDetailCache.js';
import { normalizePlanDetailPayload } from './planDetailNormalizer.js';
import {
  applyTimelineCategorySelection,
  derivePlanDetailFilters,
  INITIAL_PLAN_DETAIL_FILTERS,
  type PlanDetailFilterSnapshot,
  type PlanDetailFilters,
} from './planDetailFilters.js';
import { PLAN_STATUS_MODE, type PlanViewMode } from '../constants/planMode.js';
import { findCurrentPlanNodeId } from '../utils/planNodes.js';

export type PlanDetailContext = {
  planStatus: PlanStatus | null;
  mode: PlanViewMode;
  currentNodeId: string | null;
};

export function derivePlanDetailContext(detail: PlanDetail | null): PlanDetailContext {
  if (!detail) {
    return { planStatus: null, mode: 'design', currentNodeId: null };
  }
  const planStatus = detail.status;
  const mode = PLAN_STATUS_MODE[planStatus];
  const currentNodeId = findCurrentPlanNodeId(detail.nodes ?? []);
  return { planStatus, mode, currentNodeId };
}

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
  filters: PlanDetailFilters;
  mode: PlanViewMode;
  currentNodeId: string | null;
  context: PlanDetailContext;
};

export type PlanDetailController = {
  state: PlanDetailState;
  selectPlan: (planId: string | null) => Promise<void>;
  refresh: () => Promise<void>;
  clear: () => void;
  retain: (planIds: readonly string[]) => void;
  executeNodeAction: (input: PlanNodeActionInput) => Promise<void>;
  updateReminder: (input: PlanReminderUpdateInput) => Promise<void>;
  setTimelineCategoryFilter: (category: string | null) => void;
};

export function selectPlanDetailMode(state: PlanDetailState): PlanViewMode {
  return state.mode;
}

export function selectPlanDetailCurrentNodeId(state: PlanDetailState): string | null {
  return state.currentNodeId;
}

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
      input: PlanNodeActionInput;
    }
  | {
      type: 'reminder';
      reminderId: string;
      action: 'toggle' | 'edit';
      input: PlanReminderUpdateInput;
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

const INITIAL_CONTEXT = derivePlanDetailContext(null);

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
  filters: INITIAL_PLAN_DETAIL_FILTERS,
  mode: INITIAL_CONTEXT.mode,
  currentNodeId: INITIAL_CONTEXT.currentNodeId,
  context: INITIAL_CONTEXT,
};

export function usePlanDetailController(
  client: ApiClient,
  session: LoginResponse | null
): PlanDetailController {
  const [state, setState] = useState<PlanDetailState>(INITIAL_STATE);
  const cacheRef = useRef<Map<string, PlanDetailCacheEntry> | null>(new Map());
  const filterSnapshotRef = useRef<Map<string, PlanDetailFilterSnapshot> | null>(new Map());
  const abortRef = useRef<AbortController | null>(null);

  const resetState = useCallback(() => {
    abortCurrentRequest(abortRef);
    ensurePlanDetailCache(cacheRef).clear();
    ensurePlanDetailFilterSnapshots(filterSnapshotRef).clear();
    setState(INITIAL_STATE);
  }, []);

  useEffect(() => {
    if (!session) {
      resetState();
    }
  }, [session, resetState]);

  const commitPlanDetail = useCallback(
    (planId: string, payload: PlanDetailPayload, origin: 'cache' | 'network', fetchedAt: number) => {
      const snapshotStore = ensurePlanDetailFilterSnapshots(filterSnapshotRef);
      const previousSnapshot = snapshotStore.get(planId) ?? null;
      const { filters, snapshot } = derivePlanDetailFilters(payload, previousSnapshot);
      snapshotStore.set(planId, snapshot);

      const context = derivePlanDetailContext(payload.detail);
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
        filters,
        mode: context.mode,
        currentNodeId: context.currentNodeId,
        context,
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
          ...INITIAL_STATE,
          activePlanId: planId,
          status: 'loading',
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
    prunePlanDetailCache(cache, planIds, PLAN_DETAIL_CACHE_LIMIT);

    const cacheKeys = new Set(cache.keys());
    const snapshotStore = ensurePlanDetailFilterSnapshots(filterSnapshotRef);
    for (const planId of Array.from(snapshotStore.keys())) {
      if (!cacheKeys.has(planId)) {
        snapshotStore.delete(planId);
      }
    }
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
        input,
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
        input,
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

  const setTimelineCategoryFilter = useCallback((category: string | null) => {
    setState((current) => {
      if (!current.activePlanId) {
        return current;
      }
      const { filters, snapshot } = applyTimelineCategorySelection(current.filters, category);
      const snapshotStore = ensurePlanDetailFilterSnapshots(filterSnapshotRef);
      snapshotStore.set(current.activePlanId, snapshot);
      if (filters.timeline.activeCategory === current.filters.timeline.activeCategory) {
        return current;
      }
      return {
        ...current,
        filters,
      };
    });
  }, []);

  const controllerValue = useMemo<PlanDetailController>(() => ({
    state,
    selectPlan,
    refresh,
    clear: resetState,
    retain,
    executeNodeAction,
    updateReminder,
    setTimelineCategoryFilter,
  }), [
    executeNodeAction,
    refresh,
    retain,
    resetState,
    selectPlan,
    setTimelineCategoryFilter,
    state,
    updateReminder,
  ]);

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

function ensurePlanDetailFilterSnapshots(
  ref: { current: Map<string, PlanDetailFilterSnapshot> | null }
): Map<string, PlanDetailFilterSnapshot> {
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
