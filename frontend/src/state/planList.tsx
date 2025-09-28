import { useCallback, useEffect, useMemo, useState } from '../../vendor/react/index.js';
import { fetchPlans } from '../api/plans';
import type { ApiClient, ApiError } from '../api/client';
import type { LoginResponse, PageResponse, PlanSummary } from '../api/types';

export type PlanListState = {
  records: PlanSummary[];
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
};

const initialState: PlanListState = {
  records: [],
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
    async (signal?: AbortSignal) => {
      if (!session) {
        setState(initialState);
        return;
      }
      setState((current) => ({ ...current, status: 'loading', error: null }));
      try {
        const response = await fetchPlans(client, session.token, {
          page: 0,
          size: state.pagination.pageSize,
          signal,
        });
        setState((current) => ({
          records: response.list ?? [],
          pagination: resolvePagination(current, response),
          status: 'success',
          error: null,
        }));
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setState((current) => ({ ...current, status: 'idle', error: apiError }));
      }
    },
    [client, session, state.pagination.pageSize]
  );

  useEffect(() => {
    if (!session) {
      setState(initialState);
      return;
    }
    const controller = new AbortController();
    loadPlans(controller.signal);
    return () => {
      controller.abort();
    };
  }, [session, loadPlans]);

  const refresh = useCallback(async () => {
    await loadPlans();
  }, [loadPlans]);

  return useMemo(
    () => ({
      state,
      refresh,
    }),
    [state, refresh]
  );
}
