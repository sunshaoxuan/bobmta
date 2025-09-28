import type { ApiClient, ApiError } from './client';
import type { ApiEnvelope, PageResponse, PlanSummary } from './types';

export type PlanListQuery = {
  page: number;
  size: number;
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
