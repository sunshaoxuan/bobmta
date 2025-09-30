import test from 'node:test';
import assert from 'node:assert/strict';
import {
  extractTimelineCategories,
  filterTimelineEntries,
  isTimelineHighlightVisible,
} from '../dist/utils/planTimeline.js';

const sampleTimeline = [
  { id: 'e1', occurredAt: '2025-01-01T00:00:00Z', message: 'A', category: 'PLAN' },
  { id: 'e2', occurredAt: '2025-01-02T00:00:00Z', message: 'B', category: 'NODE' },
  { id: 'e3', occurredAt: '2025-01-03T00:00:00Z', message: 'C', category: 'NODE' },
  { id: 'e4', occurredAt: '2025-01-04T00:00:00Z', message: 'D', category: 'REMINDER' },
];

test('extractTimelineCategories keeps first occurrence order without duplicates', () => {
  const categories = extractTimelineCategories(sampleTimeline);
  assert.deepEqual(categories, ['PLAN', 'NODE', 'REMINDER']);
});

test('filterTimelineEntries returns all entries when no categories selected', () => {
  const filtered = filterTimelineEntries(sampleTimeline, []);
  assert.equal(filtered.length, sampleTimeline.length);
});

test('filterTimelineEntries narrows entries to selected categories', () => {
  const filtered = filterTimelineEntries(sampleTimeline, ['NODE']);
  assert.deepEqual(
    filtered.map((entry) => entry.id),
    ['e2', 'e3']
  );
});

test('isTimelineHighlightVisible validates highlight against active filters', () => {
  assert.equal(isTimelineHighlightVisible(sampleTimeline, ['PLAN'], 'e2'), false);
  assert.equal(isTimelineHighlightVisible(sampleTimeline, ['PLAN', 'NODE'], 'e2'), true);
  assert.equal(isTimelineHighlightVisible(sampleTimeline, [], 'e4'), true);
  assert.equal(isTimelineHighlightVisible(sampleTimeline, ['NODE'], null), true);
});
