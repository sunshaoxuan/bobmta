import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  buildPlanDetailSearch,
  parsePlanDetailUrlState,
} from '../dist/utils/planDetailUrl.js';

test('parsePlanDetailUrlState returns normalized values', () => {
  const state = parsePlanDetailUrlState('?plan= PLAN-001 &timelineCategory= ACTION ');
  assert.equal(state.planId, 'PLAN-001');
  assert.equal(state.timelineCategory, 'ACTION');
  assert.equal(state.hasTimelineCategory, true);
});

test('parsePlanDetailUrlState handles missing params', () => {
  const state = parsePlanDetailUrlState('');
  assert.equal(state.planId, null);
  assert.equal(state.timelineCategory, null);
  assert.equal(state.hasTimelineCategory, false);
});

test('buildPlanDetailSearch preserves unrelated params', () => {
  const current = '?foo=bar&plan=OLD&timelineCategory=OLD_CATEGORY';
  const next = buildPlanDetailSearch(current, {
    planId: 'PLAN-123',
    timelineCategory: 'REMINDER',
  });
  assert.equal(next, '?foo=bar&plan=PLAN-123&timelineCategory=REMINDER');
});

test('buildPlanDetailSearch removes params when value missing', () => {
  const current = '?plan=PLAN-123&timelineCategory=ACTION&foo=bar';
  const next = buildPlanDetailSearch(current, {
    planId: null,
    timelineCategory: null,
  });
  assert.equal(next, '?foo=bar');
});
