import type { ApiClient, ApiError } from './client';
import type {
  ApiEnvelope,
  PageResponse,
  PlanDetailPayload,
  PlanSummary,
} from './types';

export type PlanListQuery = {
  page: number;
  size: number;
  owner?: string | null;
  keyword?: string | null;
  status?: string | null;
  from?: string | null;
  to?: string | null;
  signal?: AbortSignal;
};

export async function fetchPlans(
  client: ApiClient,
  token: string,
  query: PlanListQuery
): Promise<PageResponse<PlanSummary>> {
  const search = new URLSearchParams();
  search.set('page', String(query.page));
  search.set('size', String(query.size));
  if (query.owner) {
    search.set('owner', query.owner);
  }
  if (query.keyword) {
    search.set('keyword', query.keyword);
  }
  if (query.status) {
    search.set('status', query.status);
  }
  if (query.from) {
    search.set('from', query.from);
  }
  if (query.to) {
    search.set('to', query.to);
  }

  try {
    const response = await client.get<ApiEnvelope<PageResponse<PlanSummary>>>(
      `/api/v1/plans?${search.toString()}`,
      {
        authToken: token,
        signal: query.signal,
      }
    );
    if (!response || !response.data) {
      throw { type: 'network' } as ApiError;
    }
    return response.data;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    if ((error as ApiError)?.type === 'network' || (error as ApiError)?.type === 'status') {
      throw error;
    }
    throw { type: 'network' } as ApiError;
  }
}

export async function fetchPlanDetail(
  client: ApiClient,
  token: string,
  planId: string,
  options: { signal?: AbortSignal } = {}
): Promise<PlanDetailPayload> {
  try {
    const response = await client.get<ApiEnvelope<PlanDetailPayload | null>>(
      `/api/v1/plans/${encodeURIComponent(planId)}`,
      {
        authToken: token,
        signal: options.signal,
      }
    );
    if (!response || !response.data) {
      throw { type: 'network' } as ApiError;
    }
    return response.data;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    if ((error as ApiError)?.type === 'network' || (error as ApiError)?.type === 'status') {
      throw error;
    }
    throw { type: 'network' } as ApiError;
  }
}
