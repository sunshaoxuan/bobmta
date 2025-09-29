import React from '../../vendor/react/index.js';
import { Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { PlanNode } from '../api/types';
import type { Locale } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_NODE_STATUS_COLOR, PLAN_NODE_STATUS_LABEL } from '../constants/planNode';
import { formatDateTime } from '../utils/planFormatting';

const { Text } = Typography;

type PlanNodeTreeProps = {
  nodes: PlanNode[];
  translate: LocalizationState['translate'];
  locale: Locale;
  depth?: number;
};

export function PlanNodeTree({ nodes, translate, locale, depth = 0 }: PlanNodeTreeProps) {
  if (!nodes || nodes.length === 0) {
    return null;
  }

  return (
    <ul className={depth === 0 ? 'plan-preview-node-tree' : 'plan-preview-node-children'}>
      {nodes.map((node) => {
        const statusLabel = translate(PLAN_NODE_STATUS_LABEL[node.status]);
        const statusColor = PLAN_NODE_STATUS_COLOR[node.status];
        const plannedDuration =
          typeof node.expectedDurationMinutes === 'number' ? node.expectedDurationMinutes : null;
        const actualStart = node.actualStartTime
          ? formatDateTime(node.actualStartTime, locale) ?? node.actualStartTime
          : null;
        const actualEnd = node.actualEndTime
          ? formatDateTime(node.actualEndTime, locale) ?? node.actualEndTime
          : null;

        return (
          <li key={node.id} className="plan-preview-node" data-depth={depth}>
            <div className="plan-preview-node-card">
              <div className="plan-preview-node-header">
                <Text strong>{node.name}</Text>
                <Tag color={statusColor}>{statusLabel}</Tag>
              </div>
              <Space size={8} wrap className="plan-preview-node-meta">
                <Tag color="default">
                  {translate('planDetailNodeOrderTag', { order: node.order ?? 0 })}
                </Tag>
                {node.actionType ? (
                  <Tag color="processing">
                    {translate('planDetailNodeActionTag', { action: node.actionType })}
                  </Tag>
                ) : null}
                {node.assignee ? (
                  <Tag color="purple">
                    {translate('planDetailNodeAssigneeTag', { name: node.assignee.name })}
                  </Tag>
                ) : null}
                {plannedDuration !== null ? (
                  <Tag color="cyan">
                    {translate('planDetailNodeDurationPlannedTag', { minutes: plannedDuration })}
                  </Tag>
                ) : null}
                {actualStart ? (
                  <Tag color="geekblue">
                    {translate('planDetailNodeActualStartTag', { time: actualStart })}
                  </Tag>
                ) : null}
                {actualEnd ? (
                  <Tag color="geekblue">
                    {translate('planDetailNodeActualEndTag', { time: actualEnd })}
                  </Tag>
                ) : null}
                {typeof node.resultSummary === 'string' && node.resultSummary.trim().length > 0 ? (
                  <Tag color="gold">
                    {translate('planDetailNodeResultTag', { result: node.resultSummary })}
                  </Tag>
                ) : null}
              </Space>
              {node.children && node.children.length > 0 ? (
                <PlanNodeTree
                  nodes={node.children}
                  translate={translate}
                  locale={locale}
                  depth={depth + 1}
                />
              ) : null}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
