import type { PlanSummary } from '../api/types';
import type { PlanListFilters } from './planList';

export type PlanListCacheEntry = {
  records: PlanSummary[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
  };
  fetchedAt: string;
};

export function createPlanListCacheKey(
  filters: PlanListFilters,
  page: number,
  pageSize: number
): string {
  const normalized = normalizeFilters(filters);
  return JSON.stringify({
    page: clampPage(page),
    pageSize: clampPageSize(pageSize),
    filters: normalized,
  });
}

export function clampPage(page: number): number {
  if (!Number.isFinite(page)) {
    return 0;
  }
  return page < 0 ? 0 : Math.floor(page);
}

export function clampPageSize(pageSize: number): number {
  if (!Number.isFinite(pageSize)) {
    return 1;
  }
  return pageSize < 1 ? 1 : Math.floor(pageSize);
}

export function normalizeFilters(
  filters: Partial<PlanListFilters> | PlanListFilters
): PlanListFilters {
  const owner = normalizeValue(filters.owner);
  const keyword = normalizeValue(filters.keyword);
  const status = normalizeValue(filters.status) as PlanStatusValue;
  const from = normalizeValue(filters.from);
  const to = normalizeValue(filters.to);
  return {
    owner,
    keyword,
    status,
    from,
    to,
  };
}

type PlanStatusValue = PlanListFilters['status'];

function normalizeValue(value: string | PlanStatusValue | null | undefined): string {
  if (typeof value !== 'string') {
    return '';
  }
  return value.trim();
}

