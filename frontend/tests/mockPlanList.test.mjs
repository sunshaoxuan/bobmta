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
