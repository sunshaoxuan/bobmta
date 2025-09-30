import test from 'node:test';
import assert from 'node:assert/strict';

import {
  aggregatePlansByCustomer,
  transformPlansToCalendarBuckets,
} from '../dist/state/planList.js';

const samplePlans = [
  {
    id: 'plan-1',
    title: 'Alpha rollout',
    owner: 'Alice',
    status: 'IN_PROGRESS',
    plannedStartTime: '2025-10-01T09:00:00+09:00',
    plannedEndTime: '2025-10-01T12:00:00+09:00',
    participants: ['Alice'],
    progress: 45,
    customer: { id: 'c-1', name: 'Contoso' },
  },
  {
    id: 'plan-2',
    title: 'Beta migration',
    owner: 'Bob',
    status: 'SCHEDULED',
    plannedStartTime: '2025-10-02T11:00:00+09:00',
    plannedEndTime: '2025-10-02T13:00:00+09:00',
    participants: ['Bob'],
    progress: 0,
    customer: { id: 'c-1', name: 'Contoso' },
  },
  {
    id: 'plan-3',
    title: 'Gamma backup',
    owner: 'Charlie',
    status: 'COMPLETED',
    plannedStartTime: '2025-09-28T09:00:00+09:00',
    plannedEndTime: '2025-09-28T11:00:00+09:00',
    participants: ['Charlie'],
    progress: 100,
    customerName: 'Fabrikam',
  },
  {
    id: 'plan-4',
    title: 'No customer',
    owner: 'Dana',
    status: 'DESIGN',
    plannedStartTime: '2025-10-20T09:00:00+09:00',
    plannedEndTime: '2025-10-20T11:00:00+09:00',
    participants: ['Dana'],
    progress: 10,
  },
];

test('aggregatePlansByCustomer groups records and calculates averages', () => {
  const groups = aggregatePlansByCustomer(samplePlans, { sortBy: 'total', descending: true });
  assert.equal(groups.length, 3);
  assert.equal(groups[0].customerName, 'Contoso');
  assert.equal(groups[0].total, 2);
  assert.equal(Math.round(groups[0].progressAverage ?? 0), 23);
  assert.deepEqual(groups[0].statusCounts, {
    DESIGN: 0,
    SCHEDULED: 1,
    IN_PROGRESS: 1,
    COMPLETED: 0,
    CANCELLED: 0,
  });
  assert.deepEqual(groups[0].owners, ['Alice', 'Bob']);
  const noCustomerGroup = groups.find((group) => !group.hasCustomer);
  assert(noCustomerGroup);
  assert.equal(noCustomerGroup.customerName, 'Unassigned');
});

test('transformPlansToCalendarBuckets returns month buckets by default', () => {
  const buckets = transformPlansToCalendarBuckets(samplePlans);
  assert.equal(buckets.length, 2);
  const october = buckets.find((bucket) => bucket.label === '2025-10');
  assert(october);
  assert.equal(october.events.length, 3);
  assert.equal(october.events[0].plan.id, 'plan-1');
  const september = buckets.find((bucket) => bucket.label === '2025-09');
  assert(september);
  assert.equal(september.events[0].plan.id, 'plan-3');
});

test('transformPlansToCalendarBuckets groups by week with custom start day', () => {
  const buckets = transformPlansToCalendarBuckets(samplePlans, {
    granularity: 'week',
    weekStartsOn: 0,
  });
  assert(buckets.length >= 2);
  const firstBucket = buckets[0];
  assert.match(firstBucket.label, /^2025-W/);
});
