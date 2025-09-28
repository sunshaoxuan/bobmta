import { useCallback, useEffect, useMemo, useState } from '../../vendor/react/index.js';
import { fetchPlans } from '../api/plans';
import type { ApiClient, ApiError } from '../api/client';
import type {
  LoginResponse,
  PageResponse,
  PlanStatus,
  PlanSummary,
} from '../api/types';

export type PlanListFilters = {
  owner: string;
  keyword: string;
  status: PlanStatus | '';
};

export type PlanListState = {
  records: PlanSummary[];
  filters: PlanListFilters;
  pagination: {
    page: number;
    pageSize: number;
    total: number;
  };
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
};

export type PlanListController = {
  state: PlanListState;
  refresh: () => Promise<void>;
  applyFilters: (filters: Partial<PlanListFilters>) => Promise<void>;
  resetFilters: () => Promise<void>;
};

const initialState: PlanListState = {
  records: [],
  filters: {
    owner: '',
    keyword: '',
    status: '',
  },
  pagination: {
    page: 0,
    pageSize: 20,
    total: 0,
  },
  status: 'idle',
  error: null,
};

function resolvePagination(
  previous: PlanListState,
  payload: PageResponse<PlanSummary>
): PlanListState['pagination'] {
  const list = payload.list ?? [];
  return {
    page: payload.page ?? 0,
    pageSize: payload.pageSize ?? previous.pagination.pageSize,
    total: payload.total ?? list.length ?? 0,
  };
}

export function usePlanListController(
  client: ApiClient,
  session: LoginResponse | null
): PlanListController {
  const [state, setState] = useState<PlanListState>(initialState);

  const loadPlans = useCallback(
    async (options?: { signal?: AbortSignal; filters?: Partial<PlanListFilters>; page?: number }) => {
      if (!session) {
        setState(initialState);
        return;
      }
      let nextFilters: PlanListFilters = initialState.filters;
      let nextPage = initialState.pagination.page;
      let pageSize = initialState.pagination.pageSize;
      setState((current) => {
        nextFilters = { ...current.filters, ...options?.filters };
        nextPage = options?.page ?? current.pagination.page;
        pageSize = current.pagination.pageSize;
        return {
          ...current,
          status: 'loading',
          error: null,
          filters: nextFilters,
          pagination: {
            ...current.pagination,
            page: nextPage,
          },
        };
      });
      try {
        const response = await fetchPlans(client, session.token, {
          page: nextPage,
          size: pageSize,
          owner: normalizeQueryValue(nextFilters.owner),
          keyword: normalizeQueryValue(nextFilters.keyword),
          status: normalizeQueryValue(nextFilters.status),
          signal: options?.signal,
        });
        setState((current) => ({
          records: response.list ?? [],
          pagination: resolvePagination(current, response),
          status: 'success',
          error: null,
          filters: nextFilters,
        }));
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
          },
        }));
      }
    },
    [client, session]
  );

  useEffect(() => {
    if (!session) {
      setState(initialState);
      return;
    }
    const controller = new AbortController();
    loadPlans({ signal: controller.signal, page: 0 });
    return () => {
      controller.abort();
    };
  }, [session, loadPlans]);

  const refresh = useCallback(async () => {
    await loadPlans();
  }, [loadPlans]);

  const applyFilters = useCallback(
    async (filters: Partial<PlanListFilters>) => {
      await loadPlans({ filters, page: 0 });
    },
    [loadPlans]
  );

  const resetFilters = useCallback(async () => {
    await loadPlans({
      filters: {
        owner: '',
        keyword: '',
        status: '',
      },
      page: 0,
    });
  }, [loadPlans]);

  return useMemo(
    () => ({
      state,
      refresh,
      applyFilters,
      resetFilters,
    }),
    [state, refresh, applyFilters, resetFilters]
  );
}

function normalizeQueryValue<T extends string | PlanStatus>(value: T | ''): string | null {
  const trimmed = (value ?? '').toString().trim();
  return trimmed.length > 0 ? trimmed : null;
}
