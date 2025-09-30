import type { PlanListFilters } from '../state/planList';
import {
  clampPage,
  clampPageSize,
  normalizeFilters,
} from '../state/planListCache.js';

export type PlanListUrlState = {
  filters: PlanListFilters;
  page: number;
  pageSize: number;
};

const OWNER_PARAM_KEY = 'owner';
const STATUS_PARAM_KEY = 'status';
const KEYWORD_PARAM_KEY = 'keyword';
const FROM_PARAM_KEY = 'from';
const TO_PARAM_KEY = 'to';
const PAGE_PARAM_KEY = 'page';
const PAGE_SIZE_PARAM_KEY = 'size';
const DEFAULT_PAGE_SIZE = 10;

export function parsePlanListUrlState(search: string): PlanListUrlState {
  const params = new URLSearchParams(search);
  const statusParam = params.get(STATUS_PARAM_KEY);
  const filters = normalizeFilters({
    owner: params.get(OWNER_PARAM_KEY) ?? undefined,
    keyword: params.get(KEYWORD_PARAM_KEY) ?? undefined,
    status: (statusParam ?? undefined) as PlanListFilters['status'] | undefined,
    from: params.get(FROM_PARAM_KEY) ?? undefined,
    to: params.get(TO_PARAM_KEY) ?? undefined,
  });
  const page = clampPage(readNumber(params.get(PAGE_PARAM_KEY), 0));
  const pageSize = clampPageSize(readNumber(params.get(PAGE_SIZE_PARAM_KEY), DEFAULT_PAGE_SIZE));
  return { filters, page, pageSize };
}

export function buildPlanListSearch(
  search: string,
  state: {
    filters?: PlanListFilters;
    page?: number;
    pageSize?: number;
  }
): string {
  const params = new URLSearchParams(search);
  if (state.filters) {
    applyFilterParam(params, OWNER_PARAM_KEY, state.filters.owner);
    applyFilterParam(params, STATUS_PARAM_KEY, state.filters.status);
    applyFilterParam(params, KEYWORD_PARAM_KEY, state.filters.keyword);
    applyFilterParam(params, FROM_PARAM_KEY, state.filters.from);
    applyFilterParam(params, TO_PARAM_KEY, state.filters.to);
  }
  if (typeof state.page === 'number') {
    applyPageParam(params, PAGE_PARAM_KEY, state.page);
  }
  if (typeof state.pageSize === 'number') {
    applyPageSizeParam(params, PAGE_SIZE_PARAM_KEY, state.pageSize);
  }
  const next = params.toString();
  return next ? `?${next}` : '';
}

function applyFilterParam(
  params: URLSearchParams,
  key: string,
  value: string | null | undefined
) {
  if (!value || value.trim().length === 0) {
    params.delete(key);
    return;
  }
  params.set(key, value.trim());
}

function applyPageParam(params: URLSearchParams, key: string, value: number) {
  const normalized = clampPage(value);
  if (normalized <= 0) {
    params.delete(key);
    return;
  }
  params.set(key, String(normalized));
}

function applyPageSizeParam(params: URLSearchParams, key: string, value: number) {
  const normalized = clampPageSize(value);
  if (normalized === DEFAULT_PAGE_SIZE) {
    params.delete(key);
    return;
  }
  params.set(key, String(normalized));
}

function readNumber(value: string | null, fallback: number): number {
  if (!value) {
    return fallback;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}
