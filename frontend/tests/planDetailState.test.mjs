import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  derivePlanDetailContext,
  selectPlanDetailContext,
  selectPlanDetailCurrentNodeId,
  selectPlanDetailMode,
  selectPlanDetailPlanStatus,
} from '../dist/state/planDetail.js';
import { INITIAL_PLAN_DETAIL_FILTERS } from '../dist/state/planDetailFilters.js';

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

function createState(detail) {
  const context = derivePlanDetailContext(detail);
  return {
    activePlanId: detail?.id ?? null,
    detail,
    timeline: [],
    reminders: [],
    status: detail ? 'success' : 'idle',
    error: null,
    lastUpdated: null,
    origin: detail ? 'network' : null,
    mutation: { status: 'idle', context: null, error: null, completedAt: null },
    filters: {
      timeline: {
        categories: [...INITIAL_PLAN_DETAIL_FILTERS.timeline.categories],
        activeCategory: INITIAL_PLAN_DETAIL_FILTERS.timeline.activeCategory,
      },
    },
    mode: context.mode,
    currentNodeId: context.currentNodeId,
    context,
  };
}

test('derivePlanDetailContext returns design mode and first actionable node for design status', () => {
  const detail = createDetail();
  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'DESIGN');
  assert.equal(context.mode, 'design');
  assert.equal(context.currentNodeId, 'NODE-1');
});

test('derivePlanDetailContext keeps design mode when draft plan has no actionable nodes', () => {
  const detail = createDetail({
    status: 'DESIGN',
    nodes: [
      { id: 'NODE-1', name: 'Archived Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Skipped Node', order: 2, status: 'SKIPPED', actionType: 'MANUAL' },
    ],
  });

  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'DESIGN');
  assert.equal(context.mode, 'design');
  assert.equal(context.currentNodeId, null);
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

test('derivePlanDetailContext keeps execution mode even when in-progress plan has no pending nodes', () => {
  const detail = createDetail({
    status: 'IN_PROGRESS',
    nodes: [
      { id: 'NODE-1', name: 'Done Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
      { id: 'NODE-2', name: 'Skipped Node', order: 2, status: 'SKIPPED', actionType: 'MANUAL' },
    ],
  });

  const context = derivePlanDetailContext(detail);

  assert.equal(context.planStatus, 'IN_PROGRESS');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, null);
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

test('derivePlanDetailContext finds nested in-progress nodes during execution', () => {
  const context = derivePlanDetailContext(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        {
          id: 'NODE-ROOT',
          name: 'Root Node',
          order: 1,
          status: 'DONE',
          actionType: 'MANUAL',
          children: [
            {
              id: 'NODE-CHILD',
              name: 'Child Node',
              order: 1,
              status: 'IN_PROGRESS',
              actionType: 'MANUAL',
            },
          ],
        },
      ],
    })
  );

  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-CHILD');
});

test('derivePlanDetailContext locates deepest pending node when execution has not started', () => {
  const context = derivePlanDetailContext(
    createDetail({
      status: 'SCHEDULED',
      nodes: [
        {
          id: 'NODE-ROOT',
          name: 'Root Node',
          order: 1,
          status: 'PENDING',
          actionType: 'MANUAL',
          children: [
            {
              id: 'NODE-BRANCH',
              name: 'Branch Node',
              order: 1,
              status: 'PENDING',
              actionType: 'MANUAL',
              children: [
                {
                  id: 'NODE-LEAF',
                  name: 'Leaf Node',
                  order: 1,
                  status: 'PENDING',
                  actionType: 'MANUAL',
                },
              ],
            },
          ],
        },
      ],
    })
  );

  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-ROOT');
});

test('selectPlanDetailMode follows context across design and execution transitions', () => {
  const designState = createState(
    createDetail({
      status: 'DESIGN',
      nodes: [
        { id: 'NODE-1', name: 'Draft Node', order: 1, status: 'PENDING', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Next Node', order: 2, status: 'DONE', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(selectPlanDetailMode(designState), 'design');
  assert.equal(selectPlanDetailCurrentNodeId(designState), 'NODE-1');

  const executionState = createState(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Draft Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Active Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
        { id: 'NODE-3', name: 'Next Node', order: 3, status: 'PENDING', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(selectPlanDetailMode(executionState), 'execution');
  assert.equal(selectPlanDetailCurrentNodeId(executionState), 'NODE-2');
});

test('selectPlanDetailContext exposes status metadata and active node marker', () => {
  const state = createState(
    createDetail({
      status: 'SCHEDULED',
      nodes: [
        { id: 'NODE-1', name: 'Preparation', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Pending Node', order: 2, status: 'PENDING', actionType: 'MANUAL' },
      ],
    })
  );

  const context = selectPlanDetailContext(state);

  assert.equal(context.planStatus, 'SCHEDULED');
  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, 'NODE-2');
});

test('selectPlanDetailPlanStatus mirrors context regardless of legacy mode fields', () => {
  const baseState = createState(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Completed Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Running Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      ],
    })
  );

  const mutatedState = {
    ...baseState,
    mode: 'design',
    currentNodeId: null,
  };

  assert.equal(selectPlanDetailPlanStatus(baseState), 'IN_PROGRESS');
  assert.equal(selectPlanDetailPlanStatus(mutatedState), 'IN_PROGRESS');
});

test('selectPlanDetailCurrentNodeId resets when detail context is cleared', () => {
  const executionState = createState(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Completed Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Running Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      ],
    })
  );

  assert.equal(selectPlanDetailMode(executionState), 'execution');
  assert.equal(selectPlanDetailCurrentNodeId(executionState), 'NODE-2');

  const clearedState = createState(null);

  assert.equal(selectPlanDetailMode(clearedState), 'design');
  assert.equal(selectPlanDetailCurrentNodeId(clearedState), null);
  const clearedContext = selectPlanDetailContext(clearedState);
  assert.equal(clearedContext.planStatus, null);
  assert.equal(clearedContext.mode, 'design');
  assert.equal(clearedContext.currentNodeId, null);
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

test('derivePlanDetailContext handles missing node list gracefully', () => {
  const context = derivePlanDetailContext(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: null,
    })
  );

  assert.equal(context.mode, 'execution');
  assert.equal(context.currentNodeId, null);
});

test('selectPlanDetail selectors rely on context over legacy state fields', () => {
  const baseState = createState(
    createDetail({
      status: 'IN_PROGRESS',
      nodes: [
        { id: 'NODE-1', name: 'Completed Node', order: 1, status: 'DONE', actionType: 'MANUAL' },
        { id: 'NODE-2', name: 'Running Node', order: 2, status: 'IN_PROGRESS', actionType: 'MANUAL' },
      ],
    })
  );

  const tamperedState = {
    ...baseState,
    mode: 'design',
    currentNodeId: 'NODE-LEGACY',
  };

  assert.equal(selectPlanDetailMode(tamperedState), 'execution');
  assert.equal(selectPlanDetailCurrentNodeId(tamperedState), 'NODE-2');
  const tamperedContext = selectPlanDetailContext(tamperedState);
  assert.equal(tamperedContext.planStatus, 'IN_PROGRESS');
  assert.equal(tamperedContext.mode, 'execution');
  assert.equal(tamperedContext.currentNodeId, 'NODE-2');
});

test('derivePlanDetailContext defaults to design mode when detail is missing', () => {
  const context = derivePlanDetailContext(null);

  assert.equal(context.planStatus, null);
  assert.equal(context.mode, 'design');
  assert.equal(context.currentNodeId, null);
});
