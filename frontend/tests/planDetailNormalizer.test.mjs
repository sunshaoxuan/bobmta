import { test } from 'node:test';
import assert from 'node:assert/strict';

import { normalizePlanDetailPayload } from '../dist/state/planDetailNormalizer.js';

function createPayload(overrides = {}) {
  return {
    detail: {
      id: 'PLAN-TEST',
      title: 'Test Plan',
      owner: 'Owner',
      status: 'IN_PROGRESS',
      description: 'description',
      customer: { id: 'CUST-1', name: 'Customer' },
      plannedStartTime: '2025-10-01T00:00:00Z',
      plannedEndTime: '2025-10-02T00:00:00Z',
      actualStartTime: null,
      actualEndTime: null,
      tags: ['alpha', 'alpha ', 'beta', null],
      participants: [
        { id: 'USR-2', name: 'Yamada' },
        { id: 'USR-1', name: 'Abe', role: 'Owner' },
        { id: 'USR-3', name: null },
      ],
      progress: 120,
      nodes: [
        {
          id: 'NODE-2',
          name: 'Node B',
          order: 2,
          status: 'UNKNOWN',
          actionType: null,
          assignee: { id: 'USR-2', name: 'Yamada', role: 'Reviewer' },
          expectedDurationMinutes: 30.4,
          actualStartTime: 'invalid-date',
          actualEndTime: '2025-10-02T03:00:00Z',
          resultSummary: '  Completed  ',
          children: [
            { id: 'NODE-2B', name: 'Child B', order: 2, status: 'DONE', actionType: 'MANUAL' },
            { id: 'NODE-2A', name: 'Child A', order: 1, status: 'IN_PROGRESS', actionType: 'MANUAL' },
          ],
        },
        {
          id: 'NODE-1',
          name: 'Node A',
          order: 1,
          status: 'DONE',
          actionType: 'SCRIPT',
          assignee: null,
          expectedDurationMinutes: 15,
          actualStartTime: '2025-10-01T01:00:00Z',
          actualEndTime: null,
          resultSummary: null,
        },
        { id: null, name: 'Invalid', order: 3 },
      ],
    },
    timeline: [
      {
        id: 'TL-OLD',
        occurredAt: '2024-01-01T00:00:00Z',
        message: 'Old event',
        actor: { id: 'USR-1', name: 'Abe' },
        category: '',
      },
      {
        id: 'TL-NEW',
        occurredAt: '2025-01-01T00:00:00Z',
        message: 'New event',
        actor: { id: 'USR-2', name: 'Yamada' },
        category: 'EVENT',
      },
      {
        id: 'TL-INVALID',
        occurredAt: 'invalid-date',
        message: 'Invalid date',
      },
      {
        id: null,
        occurredAt: '2025-02-01T00:00:00Z',
        message: 'Should drop',
      },
    ],
    reminders: [
      { id: 'RM-1', channel: 'EMAIL', offsetMinutes: -60.2, active: true, description: 'Before start' },
      { id: 'RM-2', channel: 'SMS', offsetMinutes: 30.6, active: false },
      { id: 'RM-1', channel: 'EMAIL', offsetMinutes: 999, active: true },
      { id: null, channel: 'EMAIL', offsetMinutes: 10 },
    ],
    ...overrides,
  };
}

test('normalizePlanDetailPayload sorts and sanitizes nodes, timeline, and reminders', () => {
  const payload = createPayload();
  const normalized = normalizePlanDetailPayload(payload);

  assert.equal(normalized.detail.tags.length, 2);
  assert.deepEqual(normalized.detail.tags, ['alpha', 'beta']);

  assert.equal(normalized.detail.participants.length, 2);
  assert.deepEqual(normalized.detail.participants.map((p) => p.id), ['USR-1', 'USR-2']);

  assert.equal(normalized.detail.progress, 100);

  assert.equal(normalized.detail.nodes.length, 2);
  assert.deepEqual(
    normalized.detail.nodes.map((node) => node.id),
    ['NODE-1', 'NODE-2']
  );
  const childOrder = normalized.detail.nodes[1].children.map((child) => child.id);
  assert.deepEqual(childOrder, ['NODE-2A', 'NODE-2B']);
  assert.equal(normalized.detail.nodes[1].status, 'PENDING');
  assert.equal(normalized.detail.nodes[1].actionType, '');
  assert.equal(normalized.detail.nodes[1].expectedDurationMinutes, 30);
  assert.equal(normalized.detail.nodes[1].actualStartTime, null);
  assert.equal(normalized.detail.nodes[1].actualEndTime, '2025-10-02T03:00:00Z');
  assert.equal(normalized.detail.nodes[1].resultSummary, 'Completed');

  assert.equal(normalized.timeline.length, 3);
  assert.deepEqual(
    normalized.timeline.map((entry) => entry.id),
    ['TL-NEW', 'TL-OLD', 'TL-INVALID']
  );

  assert.equal(normalized.reminders.length, 2);
  assert.deepEqual(
    normalized.reminders.map((reminder) => ({ id: reminder.id, offset: reminder.offsetMinutes })),
    [
      { id: 'RM-1', offset: -60 },
      { id: 'RM-2', offset: 31 },
    ]
  );
});
