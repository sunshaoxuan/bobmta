import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type RefObject,
} from '../../vendor/react/index.js';
import { fetchPlans } from '../api/plans.js';
import type { ApiClient, ApiError } from '../api/client.js';
import type {
  LoginResponse,
  PageResponse,
  PlanStatus,
  PlanSummary,
} from '../api/types.js';
import {
  clampPage,
  clampPageSize,
  createPlanListCacheKey,
  evictPlanListCacheEntries,
  type PlanListCacheEntry,
  normalizeFilters,
} from './planListCache.js';

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
export type PlanListViewMode = 'table' | 'customer' | 'calendar';
export const DEFAULT_PLAN_LIST_VIEW_MODE: PlanListViewMode = 'table';

export type PlanSummaryWithCustomer = PlanSummary & {
  customer?: { id?: string | null; name?: string | null } | null;
  customerId?: string | null;
  customerName?: string | null;
};

export type PlanCustomerGroup<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer
> = {
  customerId: string | null;
  customerName: string;
  hasCustomer: boolean;
  plans: T[];
  total: number;
  statusCounts: Record<PlanStatus, number>;
  progressAverage: number | null;
  owners: string[];
};

export type PlanCalendarGranularity = 'day' | 'week' | 'month' | 'year';

export type PlanCalendarEvent<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer
> = {
  plan: T;
  startTime: string | null;
  endTime: string | null;
  durationMinutes: number | null;
};

export type PlanCalendarBucket<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer
> = {
  key: string;
  granularity: PlanCalendarGranularity;
  start: string;
  end: string;
  label: string;
  events: PlanCalendarEvent<T>[];
};

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

const PLAN_STATUS_ORDER: PlanStatus[] = [
  'DESIGN',
  'SCHEDULED',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
];

export function aggregatePlansByCustomer<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer
>(
  plans: readonly T[],
  options?: { sortBy?: 'name' | 'total'; descending?: boolean }
): PlanCustomerGroup<T>[] {
  if (!plans || plans.length === 0) {
    return [];
  }
  const sortBy = options?.sortBy ?? 'name';
  const descending = Boolean(options?.descending);
  const map = new Map<
    string,
    {
      id: string | null;
      name: string;
      hasCustomer: boolean;
      plans: T[];
      statusCounts: Record<PlanStatus, number>;
      progressSum: number;
      progressCount: number;
      owners: Set<string>;
    }
  >();

  plans.forEach((plan) => {
    if (!plan || !plan.id) {
      return;
    }
    const customer = resolvePlanCustomer(plan);
    const key = customer.id ?? `name:${customer.name.toLowerCase()}`;
    let group = map.get(key);
    if (!group) {
      group = {
        id: customer.id,
        name: customer.name,
        hasCustomer: customer.hasCustomer,
        plans: [],
        statusCounts: createEmptyStatusCounts(),
        progressSum: 0,
        progressCount: 0,
        owners: new Set<string>(),
      };
      map.set(key, group);
    }
    group.plans.push(plan);
    if (PLAN_STATUS_ORDER.includes(plan.status)) {
      group.statusCounts[plan.status] += 1;
    }
    if (typeof plan.progress === 'number' && Number.isFinite(plan.progress)) {
      group.progressSum += plan.progress;
      group.progressCount += 1;
    }
    const ownerName = (plan.owner ?? '').trim();
    if (ownerName.length > 0) {
      group.owners.add(ownerName);
    }
  });

  const groups = Array.from(map.values()).map<PlanCustomerGroup<T>>((group) => ({
    customerId: group.id,
    customerName: group.name,
    hasCustomer: group.hasCustomer,
    plans: group.plans,
    total: group.plans.length,
    statusCounts: group.statusCounts,
    progressAverage:
      group.progressCount > 0 ? group.progressSum / group.progressCount : null,
    owners: Array.from(group.owners).sort((a, b) => a.localeCompare(b)),
  }));

  const comparator = sortBy === 'total' ? compareByTotal : compareByName;
  groups.sort((a, b) => comparator(a, b) * (descending ? -1 : 1));
  return groups;
}

export function transformPlansToCalendarBuckets<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer
>(
  plans: readonly T[],
  options?: { granularity?: PlanCalendarGranularity; weekStartsOn?: number }
): PlanCalendarBucket<T>[] {
  if (!plans || plans.length === 0) {
    return [];
  }
  const granularity = options?.granularity ?? 'month';
  const weekStartsOn = normalizeWeekStartsOn(options?.weekStartsOn);
  const bucketMap = new Map<string, PlanCalendarBucket<T>>();

  plans.forEach((plan) => {
    const anchor = selectAnchorTime(plan);
    if (!anchor) {
      return;
    }
    const anchorDate = new Date(anchor);
    if (Number.isNaN(anchorDate.getTime())) {
      return;
    }
    const range = resolveBucketRange(anchorDate, granularity, weekStartsOn);
    const bucketKey = `${granularity}:${range.start.toISOString()}`;
    let bucket = bucketMap.get(bucketKey);
    if (!bucket) {
      bucket = {
        key: bucketKey,
        granularity,
        start: range.start.toISOString(),
        end: range.end.toISOString(),
        label: formatBucketLabel(range.start, granularity),
        events: [],
      } as PlanCalendarBucket<T>;
      bucketMap.set(bucketKey, bucket);
    }
    const event: PlanCalendarEvent<T> = {
      plan,
      startTime: sanitizeTime(plan.plannedStartTime),
      endTime: sanitizeTime(plan.plannedEndTime),
      durationMinutes: resolveDurationMinutes(plan.plannedStartTime, plan.plannedEndTime),
    };
    bucket.events.push(event);
  });

  const buckets = Array.from(bucketMap.values());
  buckets.sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
  buckets.forEach((bucket) => {
    bucket.events.sort((a, b) => {
      const startA = toTimeValue(a.startTime);
      const startB = toTimeValue(b.startTime);
      if (startA === startB) {
        return a.plan.id.localeCompare(b.plan.id);
      }
      return startA - startB;
    });
  });
  return buckets;
}

function createEmptyStatusCounts(): Record<PlanStatus, number> {
  return PLAN_STATUS_ORDER.reduce((acc, status) => {
    acc[status] = 0;
    return acc;
  }, {} as Record<PlanStatus, number>);
}

function compareByName(a: PlanCustomerGroup, b: PlanCustomerGroup): number {
  return a.customerName.localeCompare(b.customerName);
}

function compareByTotal(a: PlanCustomerGroup, b: PlanCustomerGroup): number {
  return a.total - b.total;
}

function resolvePlanCustomer(plan: PlanSummaryWithCustomer): {
  id: string | null;
  name: string;
  hasCustomer: boolean;
} {
  const rawCustomer = plan.customer ?? null;
  const rawId =
    rawCustomer?.id ?? (typeof plan.customerId === 'string' ? plan.customerId : null);
  const rawName =
    rawCustomer?.name ??
    (typeof plan.customerName === 'string' ? plan.customerName : null) ??
    null;
  const normalizedId = normalizeIdentifier(rawId);
  const normalizedName = normalizeName(rawName);
  if (normalizedName) {
    return { id: normalizedId, name: normalizedName, hasCustomer: true };
  }
  return { id: normalizedId, name: 'Unassigned', hasCustomer: false };
}

function normalizeIdentifier(value: string | null): string | null {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function normalizeName(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function selectAnchorTime(plan: PlanSummaryWithCustomer): string | null {
  const start = sanitizeTime(plan.plannedStartTime);
  if (start) {
    return start;
  }
  const end = sanitizeTime(plan.plannedEndTime);
  return end;
}

function sanitizeTime(value?: string | null): string | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}

function resolveDurationMinutes(
  start?: string | null,
  end?: string | null
): number | null {
  const startDate = start ? new Date(start) : null;
  const endDate = end ? new Date(end) : null;
  if (!startDate || !endDate) {
    return null;
  }
  const diffMs = endDate.getTime() - startDate.getTime();
  if (!Number.isFinite(diffMs) || diffMs <= 0) {
    return null;
  }
  return Math.round(diffMs / 60000);
}

function normalizeWeekStartsOn(value?: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return 1;
  }
  const normalized = Math.floor(value);
  if (normalized < 0 || normalized > 6) {
    return 1;
  }
  return normalized;
}

function resolveBucketRange(
  anchor: Date,
  granularity: PlanCalendarGranularity,
  weekStartsOn: number
): { start: Date; end: Date } {
  const start = new Date(anchor.getTime());
  start.setMilliseconds(0);
  start.setSeconds(0);
  start.setMinutes(0);
  start.setHours(0);
  const end = new Date(start.getTime());
  switch (granularity) {
    case 'day': {
      end.setDate(end.getDate() + 1);
      break;
    }
    case 'week': {
      const day = start.getDay();
      const diff = (day - weekStartsOn + 7) % 7;
      start.setDate(start.getDate() - diff);
      end.setTime(start.getTime());
      end.setDate(end.getDate() + 7);
      break;
    }
    case 'year': {
      start.setMonth(0, 1);
      end.setFullYear(start.getFullYear() + 1, 0, 1);
      break;
    }
    case 'month':
    default: {
      start.setDate(1);
      end.setMonth(start.getMonth() + 1, 1);
      break;
    }
  }
  return { start, end };
}

function formatBucketLabel(date: Date, granularity: PlanCalendarGranularity): string {
  const year = date.getFullYear();
  const month = padNumber(date.getMonth() + 1);
  const day = padNumber(date.getDate());
  switch (granularity) {
    case 'day':
      return `${year}-${month}-${day}`;
    case 'week': {
      const weekNumber = getIsoWeekNumber(date);
      return `${year}-W${padNumber(weekNumber)}`;
    }
    case 'year':
      return `${year}`;
    case 'month':
    default:
      return `${year}-${month}`;
  }
}

function padNumber(value: number): string {
  return value < 10 ? `0${value}` : String(value);
}

function getIsoWeekNumber(date: Date): number {
  const target = new Date(date.getTime());
  target.setHours(0, 0, 0, 0);
  target.setDate(target.getDate() + 3 - ((target.getDay() + 6) % 7));
  const firstThursday = new Date(target.getFullYear(), 0, 4);
  return (
    1 +
    Math.round(
      ((target.getTime() - firstThursday.getTime()) / 86400000 - 3 +
        ((firstThursday.getDay() + 6) % 7)) /
        7
    )
  );
}

function toTimeValue(value: string | null): number {
  if (!value) {
    return Number.POSITIVE_INFINITY;
  }
  const time = new Date(value).getTime();
  return Number.isFinite(time) ? time : Number.POSITIVE_INFINITY;
}
