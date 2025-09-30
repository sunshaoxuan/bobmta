import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  applyTimelineCategorySelection,
  derivePlanDetailFilters,
  INITIAL_PLAN_DETAIL_FILTERS,
  INITIAL_PLAN_DETAIL_FILTER_SNAPSHOT,
} from '../dist/state/planDetailFilters.js';

const createTimelineEntry = (id, category) => ({
  id,
  category,
  occurredAt: new Date().toISOString(),
  message: `entry-${id}`,
});

const createPayload = (categories) => ({
  detail: {
    id: 'plan-1',
    title: 'Plan 1',
    owner: 'Owner',
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
  timeline: categories.map((category, index) =>
    createTimelineEntry(`entry-${index}`, category)
  ),
  reminders: [],
});

test('derivePlanDetailFilters keeps persisted category when available', () => {
  const payload = createPayload(['CREATE', 'EXECUTE']);
  const snapshot = {
    timeline: { category: 'EXECUTE' },
  };

  const { filters, snapshot: nextSnapshot } = derivePlanDetailFilters(payload, snapshot);

  assert.deepEqual(filters.timeline.categories, ['CREATE', 'EXECUTE']);
  assert.equal(filters.timeline.activeCategory, 'EXECUTE');
  assert.equal(nextSnapshot.timeline.category, 'EXECUTE');
});

test('derivePlanDetailFilters resets category when not present', () => {
  const payload = createPayload(['CREATE']);
  const snapshot = {
    timeline: { category: 'EXECUTE' },
  };

  const { filters, snapshot: nextSnapshot } = derivePlanDetailFilters(payload, snapshot);

  assert.deepEqual(filters.timeline.categories, ['CREATE']);
  assert.equal(filters.timeline.activeCategory, null);
  assert.equal(nextSnapshot.timeline.category, null);
});

test('applyTimelineCategorySelection normalizes invalid input', () => {
  const { filters: filtersFromPayload } = derivePlanDetailFilters(
    createPayload(['CREATE', 'EXECUTE']),
    INITIAL_PLAN_DETAIL_FILTER_SNAPSHOT
  );

  const { filters: validFilters, snapshot: validSnapshot } = applyTimelineCategorySelection(
    filtersFromPayload,
    'EXECUTE'
  );
  assert.equal(validFilters.timeline.activeCategory, 'EXECUTE');
  assert.equal(validSnapshot.timeline.category, 'EXECUTE');

  const { filters: invalidFilters, snapshot: invalidSnapshot } = applyTimelineCategorySelection(
    validFilters,
    'UNKNOWN'
  );

  assert.equal(invalidFilters.timeline.activeCategory, null);
  assert.equal(invalidSnapshot.timeline.category, null);
});

test('initial filter constants are empty and null', () => {
  assert.deepEqual(INITIAL_PLAN_DETAIL_FILTERS.timeline.categories, []);
  assert.equal(INITIAL_PLAN_DETAIL_FILTERS.timeline.activeCategory, null);
  assert.equal(INITIAL_PLAN_DETAIL_FILTER_SNAPSHOT.timeline.category, null);
});
