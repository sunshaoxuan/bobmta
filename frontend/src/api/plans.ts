import type { ApiClient, ApiError } from './client';
import type {
  ApiEnvelope,
  PageResponse,
  PlanAnalyticsOverview,
  PlanDetailPayload,
  PlanSummary,
} from './types';

export type PlanNodeActionKind = 'start' | 'complete' | 'handover';

export type PlanNodeActionRequest =
  | {
      type: 'start';
      operatorId: string;
    }
  | {
      type: 'complete';
      operatorId: string;
      resultSummary?: string | null;
    }
  | {
      type: 'handover';
      operatorId: string;
      assigneeId: string;
      comment?: string | null;
    };

export type PlanReminderUpdateRequest = {
  active: boolean;
  offsetMinutes?: number | null;
};

export type PlanAnalyticsQuery = {
  ownerId?: string | null;
  customerId?: string | null;
  tenantId?: string | null;
  from?: string | null;
  to?: string | null;
  signal?: AbortSignal;
};

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

export async function fetchPlanAnalytics(
  client: ApiClient,
  token: string,
  query: PlanAnalyticsQuery = {}
): Promise<PlanAnalyticsOverview> {
  const search = new URLSearchParams();
  if (query.ownerId) {
    search.set('ownerId', query.ownerId);
  }
  if (query.customerId) {
    search.set('customerId', query.customerId);
  }
  if (query.tenantId) {
    search.set('tenantId', query.tenantId);
  }
  if (query.from) {
    search.set('from', query.from);
  }
  if (query.to) {
    search.set('to', query.to);
  }

  const queryString = search.toString();
  const endpoint = queryString
    ? `/api/v1/plans/analytics?${queryString}`
    : '/api/v1/plans/analytics';

  try {
    const response = await client.get<ApiEnvelope<PlanAnalyticsOverview | null>>(
      endpoint,
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
    if (
      (error as ApiError)?.type === 'network' ||
      (error as ApiError)?.type === 'status'
    ) {
      throw error as ApiError;
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

export async function executePlanNodeAction(
  client: ApiClient,
  token: string,
  planId: string,
  nodeId: string,
  request: PlanNodeActionRequest
): Promise<PlanDetailPayload> {
  const actionPath = getNodeActionPath(request.type);
  try {
    const response = await client.post<ApiEnvelope<PlanDetailPayload | null>>(
      `/api/v1/plans/${encodeURIComponent(planId)}/nodes/${encodeURIComponent(nodeId)}/${actionPath}`,
      sanitizeNodeActionBody(request),
      { authToken: token }
    );
    if (!response || !response.data) {
      throw { type: 'network' } as ApiError;
    }
    return response.data;
  } catch (error) {
    if ((error as ApiError)?.type === 'network' || (error as ApiError)?.type === 'status') {
      throw error as ApiError;
    }
    throw { type: 'network' } as ApiError;
  }
}

export async function updatePlanReminder(
  client: ApiClient,
  token: string,
  planId: string,
  reminderId: string,
  request: PlanReminderUpdateRequest
): Promise<PlanDetailPayload> {
  try {
    const response = await client.put<ApiEnvelope<PlanDetailPayload | null>>(
      `/api/v1/plans/${encodeURIComponent(planId)}/reminders/${encodeURIComponent(reminderId)}`,
      sanitizeReminderBody(request),
      { authToken: token }
    );
    if (!response || !response.data) {
      throw { type: 'network' } as ApiError;
    }
    return response.data;
  } catch (error) {
    if ((error as ApiError)?.type === 'network' || (error as ApiError)?.type === 'status') {
      throw error as ApiError;
    }
    throw { type: 'network' } as ApiError;
  }
}

function getNodeActionPath(action: PlanNodeActionKind): string {
  switch (action) {
    case 'start':
      return 'start';
    case 'complete':
      return 'complete';
    case 'handover':
      return 'handover';
  }
  const exhaustiveCheck: never = action;
  throw new Error(`Unsupported plan node action: ${exhaustiveCheck}`);
}

function sanitizeNodeActionBody(request: PlanNodeActionRequest): Record<string, unknown> {
  switch (request.type) {
    case 'start':
      return { operatorId: request.operatorId };
    case 'complete': {
      const body: Record<string, unknown> = { operatorId: request.operatorId };
      if (request.resultSummary && request.resultSummary.trim()) {
        body.resultSummary = request.resultSummary.trim();
      }
      return body;
    }
    case 'handover': {
      const body: Record<string, unknown> = {
        operatorId: request.operatorId,
        assigneeId: request.assigneeId,
      };
      if (request.comment && request.comment.trim()) {
        body.comment = request.comment.trim();
      }
      return body;
    }
  }
  const exhaustiveCheck: never = request;
  throw new Error(`Unsupported plan node action body: ${exhaustiveCheck}`);
}

function sanitizeReminderBody(request: PlanReminderUpdateRequest): Record<string, unknown> {
  const body: Record<string, unknown> = {
    active: Boolean(request.active),
  };
  if (typeof request.offsetMinutes === 'number' && Number.isFinite(request.offsetMinutes)) {
    body.offsetMinutes = Math.round(request.offsetMinutes);
  }
  return body;
}
