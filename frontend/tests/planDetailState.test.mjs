import { test } from 'node:test';
import assert from 'node:assert/strict';

import { derivePlanDetailContext } from '../dist/state/planDetail.js';

function createDetail(overrides = {}) {
  return {
    id: 'PLAN-1',
    title: 'Test Plan',
    owner: 'Owner',
    status: 'DESIGN',
    description: 'desc',
    customer: null,
    plannedStartTime: null,
    plannedEndTime: null,
    actualStartTime: null,
    actualEndTime: null,
    tags: [],
    participants: [],
    progress: 0,
    nodes: [
      { id: 'NODE-1', name: 'Design Node', order: 1, status: 'PENDING', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Execution Node', order: 2, status: 'DONE', actionType: 'MANUAL' },
    ],
    ...overrides,
  };
}

test('derivePlanDetailContext returns design mode and first actionable node for design status', () => {
  const detail = createDetail();
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'DESIGN');
  assert.equal(context.mode, 'design');
  assert.equal(context.currentNodeId, 'NODE-1');
});

test('derivePlanDetailContext prefers in-progress node in execution mode', () => {
  const detail = createDetail({
    status: 'IN_PROGRESS',
    nodes: [
      { id: 'NODE-1', name: 'Design Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Execution Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      { id: 'NODE-3', name: 'Pending Node', order: 3, status: 'PENDING', actionType: 'MANUAL' },
    ],
  });
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'IN_PROGRESS');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-2');
});

test('derivePlanDetailContext returns null current node when all nodes are completed', () => {
  const detail = createDetail({
    status: 'COMPLETED',
    nodes: [
      { id: 'NODE-1', name: 'Done Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Skipped Node', order: 2, status: 'SKIPPED', actionType: 'MANUAL' },
    ],
  });
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'COMPLETED');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, null);
});

test('derivePlanDetailContext returns execution mode for scheduled plans awaiting start', () => {
  const detail = createDetail({
    status: 'SCHEDULED',
    nodes: [
      { id: 'NODE-1', name: 'Pending Node', order: 1, status: 'PENDING', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Future Node', order: 2, status: 'PENDING', actionType: 'MANUAL' },
    ],
  });
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'SCHEDULED');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-2');
});

test('derivePlanDetailContext stays in execution mode when plan is cancelled', () => {
  const detail = createDetail({
    status: 'CANCELLED',
    nodes: [
      { id: 'NODE-1', name: 'Done Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Pending Node', order: 2, status: 'PENDING', actionType: 'MANUAL' },
    ],
  });
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'CANCELLED');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-2');
});

test('derivePlanDetailContext switches mode when plan moves from design to execution', () => {
  const designContext = derivePlanDetailContext(
    createDetail({
      status: 'DESIGN',
      nodes: [
        { id: 'NODE-1', name: 'Design Node', order: 1, status: 'PENDING', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Execution Node', order: 2, status: 'PENDING', actionType: 'MANUAL' },
      ],
    })
  );

  const executionContext = derivePlanDetailContext(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Design Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Execution Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(designContext.mode, 'design');
  assert.equal(designContext.currentNodeId, 'NODE-2');
  assert.equal(executionContext.mode, 'execution');
  assert.equal(executionContext.currentNodeId, 'NODE-2');
});

test('derivePlanDetailContext reverts to design mode when plan returns to draft', () => {
  const executionContext = derivePlanDetailContext(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Completed Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Running Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      ],
    })
  );

  const revertedContext = derivePlanDetailContext(
    createDetail({
      status: 'DESIGN',
      nodes: [
        { id: 'NODE-3', name: 'Draft Node', order: 1, status: 'PENDING', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(executionContext.mode, 'execution');
  assert.equal(executionContext.currentNodeId, 'NODE-2');
  assert.equal(revertedContext.mode, 'design');
  assert.equal(revertedContext.currentNodeId, 'NODE-3');
});

test('derivePlanDetailContext keeps execution mode but clears current node when cancelled plan is fully completed', () => {
  const context = derivePlanDetailContext(
    createDetail({
      status: 'CANCELLED',
      nodes: [
        { id: 'NODE-1', name: 'Done Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Skipped Node', order: 2, status: 'SKIPPED', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, null);
});

test('derivePlanDetailContext defaults to design mode when detail is missing', () => {
  const context = derivePlanDetailContext(null);

  assert.equal(context.planStatus, null);
  assert.equal(context.mode, 'design');
  assert.equal(context.currentNodeId, null);
});
