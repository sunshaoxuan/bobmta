import type { PlanNode, PlanNodeStatus } from '../api/types';

export type PlanNodeWithPath = {
  node: PlanNode;
  path: string[];
};

export type PlanNodeActionType = 'start' | 'complete' | 'handover';

export function flattenPlanNodes(nodes: PlanNode[]): PlanNodeWithPath[] {
  const results: PlanNodeWithPath[] = [];
  const stack: Array<{ node: PlanNode; path: string[] }> = nodes.map((node) => ({
    node,
    path: [node.name],
  }));

  while (stack.length > 0) {
    const current = stack.pop();
    if (!current) {
      continue;
    }
    results.push(current);
    const children = current.node.children ?? [];
    for (let index = children.length - 1; index >= 0; index -= 1) {
      const child = children[index];
      stack.push({
        node: child,
        path: [...current.path, child.name],
      });
    }
  }

  return results;
}

export function getActionablePlanNodes(nodes: PlanNode[]): PlanNodeWithPath[] {
  return flattenPlanNodes(nodes).filter(({ node }) => isActionableStatus(node.status));
}

export function getPrimaryActionForStatus(status: PlanNodeStatus): PlanNodeActionType | null {
  switch (status) {
    case 'PENDING':
      return 'start';
    case 'IN_PROGRESS':
      return 'complete';
    default:
      return null;
  }
}

function isActionableStatus(status: PlanNodeStatus): boolean {
  return status === 'PENDING' || status === 'IN_PROGRESS';
}
