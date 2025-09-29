import React from '../../vendor/react/index.js';
import { Button, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { LocalizationState } from '../i18n/useLocalization';
import type { UiMessageKey } from '../i18n/localization';
import { PLAN_NODE_STATUS_COLOR, PLAN_NODE_STATUS_LABEL } from '../constants/planNode';
import type { PlanNodeWithPath, PlanNodeActionType } from '../utils/planNodes';
import { getPrimaryActionForStatus } from '../utils/planNodes';

const { Text } = Typography;

export type PlanNodeActionIntent = {
  nodeId: string;
  nodeName: string;
  path: string[];
  action: PlanNodeActionType;
};

type PlanNodeActionsProps = {
  candidates: PlanNodeWithPath[];
  translate: LocalizationState['translate'];
  onAction: (intent: PlanNodeActionIntent) => void;
  pendingAction: PlanNodeActionIntent | null;
};

const ACTION_LABEL_KEY: Record<PlanNodeActionType, UiMessageKey> = {
  start: 'planDetailActionStart',
  complete: 'planDetailActionComplete',
  handover: 'planDetailActionHandover',
};

export function PlanNodeActions({
  candidates,
  translate,
  onAction,
  pendingAction,
}: PlanNodeActionsProps) {
  if (!candidates || candidates.length === 0) {
    return null;
  }

  return (
    <div className="plan-node-actions">
      {candidates.map(({ node, path }) => {
        const primaryAction = getPrimaryActionForStatus(node.status);
        const breadcrumb = path.join(' / ');
        const isPending = pendingAction?.nodeId === node.id;
        return (
          <article
            key={node.id}
            className={['plan-node-action-card', isPending ? 'plan-node-action-card-pending' : '']
              .filter(Boolean)
              .join(' ')}
          >
            <header className="plan-node-action-header">
              <Text strong>{node.name}</Text>
              <Tag color={PLAN_NODE_STATUS_COLOR[node.status]}>
                {translate(PLAN_NODE_STATUS_LABEL[node.status])}
              </Tag>
            </header>
            <Text type="secondary" className="plan-node-action-path">
              {breadcrumb}
            </Text>
            <Space size="small" className="plan-node-action-buttons" wrap>
              <Button
                type="primary"
                size="small"
                disabled={!primaryAction}
                onClick={() => {
                  if (!primaryAction) {
                    return;
                  }
                  onAction({
                    nodeId: node.id,
                    nodeName: node.name,
                    path,
                    action: primaryAction,
                  });
                }}
              >
                {translate(primaryAction ? ACTION_LABEL_KEY[primaryAction] : 'planDetailActionUnavailable')}
              </Button>
              <Button
                type="default"
                size="small"
                onClick={() =>
                  onAction({ nodeId: node.id, nodeName: node.name, path, action: 'handover' })
                }
              >
                {translate(ACTION_LABEL_KEY.handover)}
              </Button>
            </Space>
            {node.assignee ? (
              <Text type="secondary" className="plan-node-action-assignee">
                {translate('planDetailNodeAssigneeTag', { name: node.assignee.name })}
              </Text>
            ) : (
              <Text type="secondary" className="plan-node-action-assignee">
                {translate('planDetailActionAssigneeMissing')}
              </Text>
            )}
          </article>
        );
      })}
      <Text type="secondary" className="plan-node-action-helper">
        {translate('planDetailActionPermissionHint')}
      </Text>
    </div>
  );
}
