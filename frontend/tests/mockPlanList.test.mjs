import { test } from 'node:test';
import assert from 'node:assert/strict';

import { listMockPlans, queryMockPlanSummaries } from '../dist/mocks/index.js';

test('mock plan listing exposes seeded records', () => {
  const plans = listMockPlans();
  assert.ok(plans.length >= 6);
  assert.ok(plans.every((plan) => plan.id && plan.title && plan.owner));
});

test('mock plan query filters by owner and keyword', () => {
  const ownerPage = queryMockPlanSummaries({ owner: 'suzuki' });
  assert.equal(ownerPage.total, 1);
  assert.equal(ownerPage.list[0].owner, 'Suzuki');

  const keywordPage = queryMockPlanSummaries({ keyword: 'リリース' });
  assert.ok(keywordPage.total >= 1);
  assert.ok(keywordPage.list.every((plan) => plan.title.includes('リリース')));
});

test('mock plan query filters by status', () => {
  const inProgressPage = queryMockPlanSummaries({ status: 'IN_PROGRESS' });
  assert.ok(inProgressPage.total >= 1);
  assert.ok(inProgressPage.list.every((plan) => plan.status === 'IN_PROGRESS'));
});

test('mock plan query paginates results', () => {
  const firstPage = queryMockPlanSummaries({ size: 2 });
  const secondPage = queryMockPlanSummaries({ size: 2, page: 1 });

  assert.equal(firstPage.list.length, 2);
  assert.equal(firstPage.page, 0);
  assert.equal(firstPage.pageSize, 2);
  assert.equal(secondPage.list.length, 2);
  assert.equal(secondPage.page, 1);
  assert.equal(secondPage.pageSize, 2);
  assert.notDeepEqual(firstPage.list.map((plan) => plan.id), secondPage.list.map((plan) => plan.id));
});

test('mock plan query filters by planned window overlap', () => {
  const from = new Date('2025-09-30T00:00:00+09:00').toISOString();
  const to = new Date('2025-10-02T00:00:00+09:00').toISOString();
  const windowPage = queryMockPlanSummaries({ from, to });

  assert.ok(windowPage.total >= 2);
  assert.ok(
    windowPage.list.every((plan) => {
      const start = new Date(plan.plannedStartTime ?? '').getTime();
      const end = new Date(plan.plannedEndTime ?? '').getTime();
      return end >= new Date(from).getTime() && start <= new Date(to).getTime();
    })
  );
});
