import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  clampPage,
  clampPageSize,
  createPlanListCacheKey,
  normalizeFilters,
} from '../dist/state/planListCache.js';

test('createPlanListCacheKey normalizes whitespace and pagination', () => {
  const filtersA = normalizeFilters({
    owner: '  Suzuki  ',
    keyword: ' リリース ',
    status: 'IN_PROGRESS',
    from: '2025-10-01T00:00:00+09:00',
    to: '2025-10-02T00:00:00+09:00',
  });
  const filtersB = normalizeFilters({
    owner: 'Suzuki',
    keyword: 'リリース',
    status: 'IN_PROGRESS',
    from: '2025-10-01T00:00:00+09:00',
    to: '2025-10-02T00:00:00+09:00',
  });

  const keyA = createPlanListCacheKey(filtersA, 1, 20);
  const keyB = createPlanListCacheKey(filtersB, 1.9, 20.4);

  assert.equal(keyA, keyB);
});

test('clampPage keeps values within valid range', () => {
  assert.equal(clampPage(-1), 0);
  assert.equal(clampPage(2.7), 2);
  assert.equal(clampPage(Number.NaN), 0);
});

test('clampPageSize ensures minimum size of 1', () => {
  assert.equal(clampPageSize(0), 1);
  assert.equal(clampPageSize(25.6), 25);
  assert.equal(clampPageSize(Number.NaN), 1);
});
