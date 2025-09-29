import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import {
  flattenPlanNodes,
  getActionablePlanNodes,
  getPrimaryActionForStatus,
} from '../dist/utils/planNodes.js';

describe('planNodes utilities', () => {
  const tree = [
    {
      id: 'root',
      name: 'Root',
      order: 1,
      status: 'PENDING',
      actionType: 'CHECK',
      children: [
        {
          id: 'child-1',
          name: 'Child 1',
          order: 1,
          status: 'IN_PROGRESS',
          actionType: 'RUN',
          children: [],
        },
        {
          id: 'child-2',
          name: 'Child 2',
          order: 2,
          status: 'DONE',
          actionType: 'VERIFY',
          children: [
            {
              id: 'child-2-1',
              name: 'Nested',
              order: 1,
              status: 'PENDING',
              actionType: 'VALIDATE',
              children: [],
            },
          ],
        },
      ],
    },
  ];

  it('flattens nodes with hierarchical path', () => {
    const flattened = flattenPlanNodes(tree);
    const ids = flattened.map((entry) => entry.node.id);
    assert.deepEqual(ids.sort(), ['child-1', 'child-2', 'child-2-1', 'root'].sort());

    const nested = flattened.find((entry) => entry.node.id === 'child-2-1');
    assert(nested);
    assert.deepEqual(nested.path, ['Root', 'Child 2', 'Nested']);
  });

  it('filters actionable nodes', () => {
    const actionable = getActionablePlanNodes(tree);
    const ids = actionable.map((entry) => entry.node.id);
    assert.deepEqual(ids.sort(), ['child-1', 'child-2-1', 'root'].sort());
  });

  it('returns primary actions based on status', () => {
    assert.equal(getPrimaryActionForStatus('PENDING'), 'start');
    assert.equal(getPrimaryActionForStatus('IN_PROGRESS'), 'complete');
    assert.equal(getPrimaryActionForStatus('DONE'), null);
    assert.equal(getPrimaryActionForStatus('CANCELLED'), null);
    assert.equal(getPrimaryActionForStatus('SKIPPED'), null);
  });
});
