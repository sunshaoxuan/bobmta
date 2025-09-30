import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type RefObject,
} from '../../vendor/react/index.js';
import { fetchPlans } from '../api/plans';
import type { ApiClient, ApiError } from '../api/client';
import type {
  LoginResponse,
  PageResponse,
  PlanStatus,
  PlanSummary,
} from '../api/types';
import {
  clampPage,
  clampPageSize,
  createPlanListCacheKey,
  evictPlanListCacheEntries,
  type PlanListCacheEntry,
  normalizeFilters,
} from './planListCache';

export type PlanListFilters = {
  owner: string;
  keyword: string;
  status: PlanStatus | '';
  from: string;
  to: string;
};

export type PlanListState = {
  records: PlanSummary[];
  recordIndex: Record<string, PlanSummary>;
  filters: PlanListFilters;
  pagination: {
    page: number;
    pageSize: number;
    total: number;
  };
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  lastUpdated: string | null;
  origin: 'network' | 'cache' | null;
};

export type PlanListController = {
  state: PlanListState;
  refresh: () => Promise<void>;
  applyFilters: (filters: Partial<PlanListFilters>) => Promise<void>;
  resetFilters: () => Promise<void>;
  changePage: (page: number) => Promise<void>;
  changePageSize: (pageSize: number) => Promise<void>;
  restore: (state: {
    filters: PlanListFilters;
    page: number;
    pageSize: number;
  }) => Promise<void>;
  getCachedPlan: (id: string) => PlanSummary | null;
};

export const DEFAULT_PLAN_LIST_PAGE_SIZE = 10;
const PLAN_LIST_CACHE_LIMIT = 12;

function createEmptyFilters(): PlanListFilters {
  return {
    owner: '',
    keyword: '',
    status: '',
    from: '',
    to: '',
  };
}

function createInitialState(): PlanListState {
  return {
    records: [],
    recordIndex: {},
    filters: createEmptyFilters(),
    pagination: {
      page: 0,
      pageSize: DEFAULT_PLAN_LIST_PAGE_SIZE,
      total: 0,
    },
    status: 'idle',
    error: null,
    lastUpdated: null,
    origin: null,
  };
}

export function usePlanListController(
  client: ApiClient,
  session: LoginResponse | null
): PlanListController {
  const [state, setState] = useState<PlanListState>(() => createInitialState());
  const stateRef = useRef<PlanListState>(state);
  const cacheRef = useRef<Map<string, PlanListCacheEntry>>(new Map());
  const recordIndexRef = useRef<Map<string, PlanSummary>>(new Map());

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const loadPlans = useCallback(
    async (
      options?: {
        signal?: AbortSignal;
        filters?: Partial<PlanListFilters>;
        page?: number;
        pageSize?: number;
        force?: boolean;
      }
    ) => {
      const cache = ensureMapRef(cacheRef);
      const recordIndex = ensureMapRef(recordIndexRef);
      if (!session) {
        cache.clear();
        recordIndex.clear();
        setState(createInitialState());
        return;
      }
      let nextFilters: PlanListFilters = createEmptyFilters();
      let nextPage = 0;
      let pageSize = DEFAULT_PLAN_LIST_PAGE_SIZE;
      let cacheKey = '';
      let cachedEntry: PlanListCacheEntry | null = null;
      let useCache = false;
      setState((current) => {
        const mergedFilters = { ...current.filters, ...options?.filters } as PlanListFilters;
        nextFilters = normalizeFilters(mergedFilters);
        nextPage = clampPage(options?.page ?? current.pagination.page ?? 0);
        pageSize = clampPageSize(
          options?.pageSize ?? current.pagination.pageSize ?? DEFAULT_PLAN_LIST_PAGE_SIZE
        );
        cacheKey = createPlanListCacheKey(nextFilters, nextPage, pageSize);
        cachedEntry = cache.get(cacheKey) ?? null;
        useCache = Boolean(cachedEntry) && !options?.force;
        if (useCache && cachedEntry) {
          updateRecordIndex(recordIndex, cachedEntry.records);
          return {
            ...current,
            records: cachedEntry.records,
            filters: nextFilters,
            pagination: cachedEntry.pagination,
            status: 'success',
            error: null,
            lastUpdated: cachedEntry.fetchedAt,
            origin: 'cache',
            recordIndex: mapRecordIndex(recordIndex),
          };
        }
        return {
          ...current,
          status: 'loading',
          error: null,
          filters: nextFilters,
          pagination: {
            ...current.pagination,
            page: nextPage,
            pageSize,
          },
        };
      });
      if (useCache && cachedEntry) {
        return;
      }
      try {
        const response = await fetchPlans(client, session.token, {
          page: nextPage,
          size: pageSize,
          owner: normalizeQueryValue(nextFilters.owner),
          keyword: normalizeQueryValue(nextFilters.keyword),
          status: normalizeQueryValue(nextFilters.status),
          from: normalizeQueryValue(nextFilters.from),
          to: normalizeQueryValue(nextFilters.to),
          signal: options?.signal,
        });
        const pagination = resolvePagination(pageSize, response);
        const fetchedAt = new Date().toISOString();
        const records = response.list ?? [];
        const cacheEntry: PlanListCacheEntry = {
          records,
          pagination,
          fetchedAt,
        };
        cache.set(cacheKey, cacheEntry);
        evictPlanListCacheEntries(cache, PLAN_LIST_CACHE_LIMIT);
        updateRecordIndex(recordIndex, records);
        setState({
          records,
          pagination,
          status: 'success',
          error: null,
          filters: nextFilters,
          lastUpdated: fetchedAt,
          origin: 'network',
          recordIndex: mapRecordIndex(recordIndex),
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
          status: 'idle',
          error: apiError,
          filters: nextFilters,
          pagination: {
            ...current.pagination,
            page: nextPage,
            pageSize,
          },
        }));
      }
    },
    [client, session]
  );

  useEffect(() => {
    if (!session) {
      const cache = ensureMapRef(cacheRef);
      const recordIndex = ensureMapRef(recordIndexRef);
      cache.clear();
      recordIndex.clear();
      setState(createInitialState());
      return;
    }
    const controller = new AbortController();
    loadPlans({ signal: controller.signal, page: 0, force: true });
    return () => {
      controller.abort();
    };
  }, [session, loadPlans]);

  const refresh = useCallback(async () => {
    await loadPlans({ force: true });
  }, [loadPlans]);

  const applyFilters = useCallback(
    async (filters: Partial<PlanListFilters>) => {
      await loadPlans({ filters, page: 0 });
    },
    [loadPlans]
  );

  const resetFilters = useCallback(async () => {
    await loadPlans({
      filters: createEmptyFilters(),
      page: 0,
    });
  }, [loadPlans]);

  const changePage = useCallback(
    async (page: number) => {
      await loadPlans({ page: clampPage(page) });
    },
    [loadPlans]
  );

  const changePageSize = useCallback(
    async (pageSize: number) => {
      const normalized = clampPageSize(pageSize);
      await loadPlans({ page: 0, pageSize: normalized });
    },
    [loadPlans]
  );

  const restore = useCallback(
    async (snapshot: { filters: PlanListFilters; page: number; pageSize: number }) => {
      const normalizedFilters = normalizeFilters(snapshot.filters);
      const nextPage = clampPage(snapshot.page);
      const nextPageSize = clampPageSize(snapshot.pageSize);
      const current = stateRef.current;
      if (
        current &&
        arePlanListFiltersEqual(current.filters, normalizedFilters) &&
        current.pagination.page === nextPage &&
        current.pagination.pageSize === nextPageSize
      ) {
        return;
      }
      await loadPlans({ filters: normalizedFilters, page: nextPage, pageSize: nextPageSize });
    },
    [loadPlans]
  );

  const getCachedPlan = useCallback(
    (id: string) => {
      return recordIndexRef.current?.get(id) ?? null;
    },
    []
  );

  return useMemo(
    () => ({
      state,
      refresh,
      applyFilters,
      resetFilters,
      changePage,
      changePageSize,
      restore,
      getCachedPlan,
    }),
    [state, refresh, applyFilters, resetFilters, changePage, changePageSize, restore, getCachedPlan]
  );
}

export function arePlanListFiltersEqual(a: PlanListFilters, b: PlanListFilters): boolean {
  return (
    a.owner === b.owner &&
    a.keyword === b.keyword &&
    a.status === b.status &&
    a.from === b.from &&
    a.to === b.to
  );
}

function normalizeQueryValue<T extends string | PlanStatus>(value: T | ''): string | null {
  const trimmed = (value ?? '').toString().trim();
  return trimmed.length > 0 ? trimmed : null;
}

function resolvePagination(
  fallbackPageSize: number,
  payload: PageResponse<PlanSummary>
): PlanListState['pagination'] {
  const page = clampPage(Number(payload.page ?? 0));
  const size = clampPageSize(Number(payload.pageSize ?? fallbackPageSize));
  const total =
    typeof payload.total === 'number' && Number.isFinite(payload.total)
      ? Math.max(0, Math.floor(payload.total))
      : payload.list?.length ?? 0;
  return {
    page,
    pageSize: size,
    total,
  };
}

function updateRecordIndex(target: Map<string, PlanSummary>, records: PlanSummary[]) {
  records.forEach((record) => {
    if (record && record.id) {
      target.set(record.id, record);
    }
  });
}

function mapRecordIndex(source: Map<string, PlanSummary>): Record<string, PlanSummary> {
  const result: Record<string, PlanSummary> = {};
  source.forEach((value, key) => {
    result[key] = value;
  });
  return result;
}

function ensureMapRef<K, V>(ref: RefObject<Map<K, V>>): Map<K, V> {
  if (!ref.current) {
    ref.current = new Map<K, V>();
  }
  return ref.current;
}
