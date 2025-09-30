import { test } from 'node:test';
import assert from 'node:assert/strict';

import { parsePlanRoute, buildPlanDetailPath } from '../dist/router/planRoutes.js';

test('parsePlanRoute returns list route for root path', () => {
  assert.deepEqual(parsePlanRoute('/'), { type: 'list', planId: null });
  assert.deepEqual(parsePlanRoute(''), { type: 'list', planId: null });
});

test('parsePlanRoute returns detail route for plan path', () => {
  assert.deepEqual(parsePlanRoute('/plans/PLAN-123'), {
    type: 'detail',
    planId: 'PLAN-123',
  });
});

test('parsePlanRoute decodes encoded identifiers', () => {
  assert.deepEqual(parsePlanRoute('/plans/PLAN%20123'), {
    type: 'detail',
    planId: 'PLAN 123',
  });
});

test('parsePlanRoute marks unknown paths', () => {
  assert.deepEqual(parsePlanRoute('/other/path'), {
    type: 'unknown',
    path: '/other/path',
  });
});

test('buildPlanDetailPath encodes identifiers', () => {
  assert.equal(buildPlanDetailPath('PLAN 123'), '/plans/PLAN%20123');
});

test('buildPlanDetailPath falls back to base segment when identifier empty', () => {
  assert.equal(buildPlanDetailPath(''), '/plans');
  assert.equal(buildPlanDetailPath('   '), '/plans');
});
