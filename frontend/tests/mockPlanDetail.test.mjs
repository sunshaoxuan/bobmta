import { test } from 'node:test';
import assert from 'node:assert/strict';

import { getMockPlanDetail } from '../dist/mocks/index.js';

test('mock plan detail provides nested nodes and timeline', () => {
  const detail = getMockPlanDetail('PLN-2025-001');
  assert.ok(detail);
  assert.equal(detail.detail.id, 'PLN-2025-001');
  assert.ok(detail.timeline.length >= 1);
  assert.ok(detail.reminders.length >= 1);
  const nodeWithChildren = detail.detail.nodes.find((node) => (node.children ?? []).length > 0);
  assert.ok(nodeWithChildren);
  assert.ok(nodeWithChildren.children.length > 0);
});

test('mock plan detail returns null for unknown plan', () => {
  const detail = getMockPlanDetail('UNKNOWN');
  assert.equal(detail, null);
});
