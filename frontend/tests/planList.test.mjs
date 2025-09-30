import test from 'node:test';
import assert from 'node:assert/strict';
import { extractPlanOwners } from '../dist/utils/planList.js';

const samplePlans = [
  { id: 'p1', owner: '佐藤', title: 'A' },
  { id: 'p2', owner: '  佐藤  ', title: 'B' },
  { id: 'p3', owner: '鈴木', title: 'C' },
  { id: 'p4', owner: '', title: 'D' },
  { id: 'p5', owner: null, title: 'E' },
];

test('extractPlanOwners removes duplicates and trims whitespace', () => {
  const owners = extractPlanOwners(samplePlans, 'ja-JP');
  assert.deepEqual(owners, ['佐藤', '鈴木']);
});

test('extractPlanOwners falls back to default locale when missing', () => {
  const owners = extractPlanOwners(
    [
      { id: 'p1', owner: 'zeta', title: 'A' },
      { id: 'p2', owner: 'Alpha', title: 'B' },
    ],
    null
  );
  assert.deepEqual(owners, ['Alpha', 'zeta']);
});
