import test from 'node:test';
import assert from 'node:assert/strict';
import {
  aggregatePlansByCustomer,
  createPlanCalendarEvents,
  groupCalendarEvents,
  transformPlansToCalendarBuckets,
} from '../dist/state/planList.js';
import {
  getPlanCalendarEventAnchor,
  getPlanCalendarEventTime,
  mapPlanCalendarEventsByDate,
} from '../dist/utils/planList.js';

function createPlan(id, overrides = {}) {
  return {
    id,
    title: `Plan ${id}`,
    status: 'SCHEDULED',
    progress: 40,
    owner: 'Alice',
    plannedStartTime: '2025-05-01T08:00:00.000Z',
    plannedEndTime: '2025-05-01T12:00:00.000Z',
    customer: { id: 'c-1', name: 'Contoso' },
    customerId: 'c-1',
    customerName: 'Contoso',
    ...overrides,
  };
}

test('aggregatePlansByCustomer groups by normalized customer and aggregates stats', () => {
  const plans = [
    createPlan('p-1', { progress: 60, status: 'IN_PROGRESS', owner: 'Alice' }),
    createPlan('p-2', { progress: 20, status: 'SCHEDULED', owner: 'Bob' }),
    createPlan('p-3', {
      customer: { id: 'c-2', name: 'Fabrikam' },
      customerId: 'c-2',
      customerName: 'Fabrikam',
      progress: 100,
      status: 'COMPLETED',
      owner: 'Alice',
    }),
    createPlan('p-4', {
      customer: null,
      customerId: null,
      customerName: null,
      progress: null,
      status: 'DESIGN',
      owner: 'Charlie',
    }),
  ];

  const groups = aggregatePlansByCustomer(plans, { sortBy: 'total', descending: true });
  assert.equal(groups.length, 3);

  const contoso = groups.find((group) => group.customerName === 'Contoso');
  assert.ok(contoso);
  assert.equal(contoso.total, 2);
  assert.deepEqual(contoso.statusCounts, {
    DESIGN: 0,
    SCHEDULED: 1,
    IN_PROGRESS: 1,
    COMPLETED: 0,
    CANCELLED: 0,
  });
  assert.equal(Math.round(contoso.progressAverage ?? 0), 40);
  assert.deepEqual(contoso.owners, ['Alice', 'Bob']);

  const fabrikam = groups.find((group) => group.customerName === 'Fabrikam');
  assert.ok(fabrikam);
  assert.equal(fabrikam.total, 1);
  assert.equal(fabrikam.statusCounts.COMPLETED, 1);
  assert.equal(fabrikam.progressAverage, 100);

  const unassigned = groups.find((group) => !group.hasCustomer);
  assert.ok(unassigned);
  assert.equal(unassigned.customerName, 'Unassigned');
  assert.equal(unassigned.total, 1);
  assert.equal(unassigned.statusCounts.DESIGN, 1);
  assert.equal(unassigned.progressAverage, null);
});

test('createPlanCalendarEvents sorts by anchor time and computes duration', () => {
  const plans = [
    createPlan('p-5', {
      plannedStartTime: '2025-05-03T09:00:00.000Z',
      plannedEndTime: '2025-05-03T10:30:00.000Z',
    }),
    createPlan('p-6', {
      plannedStartTime: null,
      plannedEndTime: '2025-05-02T10:00:00.000Z',
    }),
    createPlan('ignored', {
      id: 'ignored',
      plannedStartTime: null,
      plannedEndTime: null,
    }),
  ];

  const events = createPlanCalendarEvents(plans);
  assert.equal(events.length, 2);
  assert.equal(events[0].plan.id, 'p-6');
  assert.equal(events[1].plan.id, 'p-5');
  assert.equal(events[1].durationMinutes, 90);
});

test('groupCalendarEvents buckets events by granularity and preserves ordering', () => {
  const plans = [
    createPlan('p-7', {
      plannedStartTime: '2025-05-01T10:00:00.000Z',
      plannedEndTime: '2025-05-01T11:00:00.000Z',
    }),
    createPlan('p-8', {
      plannedStartTime: '2025-05-15T15:00:00.000Z',
      plannedEndTime: '2025-05-15T16:00:00.000Z',
    }),
    createPlan('p-9', {
      plannedStartTime: '2025-06-02T09:00:00.000Z',
      plannedEndTime: '2025-06-02T11:00:00.000Z',
    }),
  ];

  const events = createPlanCalendarEvents(plans);
  const monthBuckets = groupCalendarEvents(events, { granularity: 'month' });
  assert.equal(monthBuckets.length, 2);
  assert.deepEqual(
    monthBuckets.map((bucket) => bucket.label),
    ['2025-05', '2025-06']
  );
  assert.equal(monthBuckets[0].events.length, 2);

  const weekBuckets = groupCalendarEvents(events, { granularity: 'week', weekStartsOn: 1 });
  assert.ok(weekBuckets.length >= 2);
  assert.ok(weekBuckets.every((bucket) => bucket.events.length >= 1));
});

test('transformPlansToCalendarBuckets reuses event aggregation', () => {
  const plans = [
    createPlan('p-10', {
      plannedStartTime: '2025-07-01T09:00:00.000Z',
      plannedEndTime: '2025-07-01T10:00:00.000Z',
    }),
    createPlan('p-11', {
      plannedStartTime: '2025-07-15T12:00:00.000Z',
      plannedEndTime: '2025-07-15T13:30:00.000Z',
    }),
  ];

  const directEvents = createPlanCalendarEvents(plans);
  const grouped = groupCalendarEvents(directEvents, { granularity: 'month' });
  const transformed = transformPlansToCalendarBuckets(plans, { granularity: 'month' });

  assert.deepEqual(transformed, grouped);
  assert.equal(transformed[0].events[1].durationMinutes, 90);
});

test('mapPlanCalendarEventsByDate groups events by local day and sorts within the day', () => {
  const plans = [
    createPlan('p-12', {
      plannedStartTime: '2025-05-01T08:00:00.000Z',
      plannedEndTime: '2025-05-01T09:00:00.000Z',
    }),
    createPlan('p-13', {
      plannedStartTime: null,
      plannedEndTime: '2025-05-01T05:00:00.000Z',
    }),
  ];

  const events = createPlanCalendarEvents(plans);
  const map = mapPlanCalendarEventsByDate(events);
  const keys = Object.keys(map).sort();
  assert.deepEqual(keys, ['2025-05-01']);
  assert.equal(map['2025-05-01'].length, 2);
  assert.deepEqual(
    map['2025-05-01'].map((event) => event.plan.id),
    ['p-13', 'p-12']
  );

  const mapWithTz = mapPlanCalendarEventsByDate(events, { timeZone: 'America/Los_Angeles' });
  const tzKeys = Object.keys(mapWithTz).sort();
  assert.deepEqual(tzKeys, ['2025-04-30', '2025-05-01']);
  assert.equal(mapWithTz['2025-04-30'].length, 1);
  assert.equal(mapWithTz['2025-04-30'][0].plan.id, 'p-13');
  assert.equal(mapWithTz['2025-05-01'].length, 1);
  assert.equal(mapWithTz['2025-05-01'][0].plan.id, 'p-12');
});

test('getPlanCalendarEventAnchor/time prefer start then end timestamps', () => {
  const plan = createPlan('p-14', {
    plannedStartTime: '2025-05-10T09:00:00.000Z',
    plannedEndTime: '2025-05-10T11:00:00.000Z',
  });
  const [event] = createPlanCalendarEvents([plan]);
  assert.ok(event);
  assert.equal(getPlanCalendarEventAnchor(event), '2025-05-10T09:00:00.000Z');
  assert.equal(getPlanCalendarEventTime(event), new Date('2025-05-10T09:00:00.000Z').getTime());

  const fallbackPlan = createPlan('p-15', {
    plannedStartTime: null,
    plannedEndTime: '2025-05-11T14:00:00.000Z',
  });
  const [fallbackEvent] = createPlanCalendarEvents([fallbackPlan]);
  assert.ok(fallbackEvent);
  assert.equal(getPlanCalendarEventAnchor(fallbackEvent), '2025-05-11T14:00:00.000Z');
  assert.equal(
    getPlanCalendarEventTime(fallbackEvent),
    new Date('2025-05-11T14:00:00.000Z').getTime()
  );

  const missingPlan = createPlan('p-16', {
    plannedStartTime: null,
    plannedEndTime: null,
  });
  const missingEvent = {
    plan: missingPlan,
    startTime: null,
    endTime: null,
    durationMinutes: null,
  };
  assert.equal(getPlanCalendarEventAnchor(missingEvent), null);
  assert.equal(getPlanCalendarEventTime(missingEvent), Number.POSITIVE_INFINITY);
});
