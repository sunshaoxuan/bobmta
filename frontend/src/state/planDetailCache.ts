import type { PlanDetailPayload } from '../api/types';

export type PlanDetailCacheEntry = {
  payload: PlanDetailPayload;
  fetchedAt: number;
};

export const PLAN_DETAIL_CACHE_TTL_MS = 5 * 60 * 1000;
export const PLAN_DETAIL_CACHE_LIMIT = 16;

export function isPlanDetailCacheEntryFresh(
  entry: PlanDetailCacheEntry | null,
  now: number,
  ttlMs: number = PLAN_DETAIL_CACHE_TTL_MS
): boolean {
  if (!entry) {
    return false;
  }
  return now - entry.fetchedAt <= ttlMs;
}

export function evictPlanDetailCacheEntries(
  cache: Map<string, PlanDetailCacheEntry>,
  limit: number = PLAN_DETAIL_CACHE_LIMIT
): void {
  if (cache.size <= limit) {
    return;
  }
  const entries = Array.from(cache.entries()).sort(
    (first, second) => first[1].fetchedAt - second[1].fetchedAt
  );
  while (entries.length > 0 && cache.size > limit) {
    const [key] = entries.shift()!;
    cache.delete(key);
  }
}

export function prunePlanDetailCache(
  cache: Map<string, PlanDetailCacheEntry>,
  allowedPlanIds: readonly string[],
  limit: number = PLAN_DETAIL_CACHE_LIMIT
): void {
  const allowed = new Set(allowedPlanIds);
  for (const planId of Array.from(cache.keys())) {
    if (!allowed.has(planId)) {
      cache.delete(planId);
    }
  }
  evictPlanDetailCacheEntries(cache, limit);
}
