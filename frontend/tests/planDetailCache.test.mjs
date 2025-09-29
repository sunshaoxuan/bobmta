import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  PLAN_DETAIL_CACHE_TTL_MS,
  evictPlanDetailCacheEntries,
  isPlanDetailCacheEntryFresh,
  prunePlanDetailCache,
} from '../dist/state/planDetailCache.js';

const createPayload = (planId) => ({
  detail: {
    id: planId,
    title: `PLAN_${planId}`,
    owner: 'owner',
    status: 'IN_PROGRESS',
    description: null,
    customer: null,
    plannedStartTime: null,
    plannedEndTime: null,
    actualStartTime: null,
    actualEndTime: null,
    tags: [],
    participants: [],
    progress: 0,
    nodes: [],
  },
  timeline: [],
  reminders: [],
});

test('isPlanDetailCacheEntryFresh respects TTL boundaries', () => {
  const now = Date.UTC(2025, 8, 28, 0, 0, 0);
  const freshEntry = {
    payload: createPayload('plan-fresh'),
    fetchedAt: now - PLAN_DETAIL_CACHE_TTL_MS + 1_000,
  };
  const staleEntry = {
    payload: createPayload('plan-stale'),
    fetchedAt: now - PLAN_DETAIL_CACHE_TTL_MS - 1,
  };

  assert.equal(isPlanDetailCacheEntryFresh(freshEntry, now, PLAN_DETAIL_CACHE_TTL_MS), true);
  assert.equal(isPlanDetailCacheEntryFresh(staleEntry, now, PLAN_DETAIL_CACHE_TTL_MS), false);
  assert.equal(isPlanDetailCacheEntryFresh(null, now, PLAN_DETAIL_CACHE_TTL_MS), false);
});

test('evictPlanDetailCacheEntries removes the oldest entries first', () => {
  const cache = new Map();
  const base = Date.UTC(2025, 8, 28, 1, 0, 0);

  for (let index = 0; index < 4; index += 1) {
    cache.set(`plan-${index}`, {
      payload: createPayload(`plan-${index}`),
      fetchedAt: base + index * 1_000,
    });
  }

  evictPlanDetailCacheEntries(cache, 2);

  assert.equal(cache.size, 2);
  assert.ok(!cache.has('plan-0'));
  assert.ok(!cache.has('plan-1'));
  assert.ok(cache.has('plan-2'));
  assert.ok(cache.has('plan-3'));
});

test('prunePlanDetailCache keeps allowed plans and enforces limit', () => {
  const cache = new Map();
  const base = Date.UTC(2025, 8, 28, 2, 0, 0);

  for (let index = 0; index < 5; index += 1) {
    cache.set(`plan-${index}`, {
      payload: createPayload(`plan-${index}`),
      fetchedAt: base + index * 1_000,
    });
  }

  prunePlanDetailCache(cache, ['plan-1', 'plan-2', 'plan-3', 'plan-4'], 2);

  assert.equal(cache.size, 2);
  assert.ok(!cache.has('plan-0'));
  assert.ok(cache.has('plan-3'));
  assert.ok(cache.has('plan-4'));
});
