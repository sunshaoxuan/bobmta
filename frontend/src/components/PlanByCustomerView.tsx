import React, { useMemo } from '../../vendor/react/index.js';
import {
  Card,
  Empty,
  List,
  Progress,
  Space,
  Tag,
  Tree,
  Typography,
} from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import {
  aggregatePlansByCustomer,
  type PlanCustomerGroup,
  type PlanSummaryWithCustomer,
} from '../state/planList';
import {
  PLAN_STATUS_COLOR,
  PLAN_STATUS_LABEL,
  PLAN_STATUS_ORDER,
} from '../constants/planStatus';
import { listMockPlans } from '../mocks/planList';
import type { ReactNode } from '../../vendor/react/index.js';

const { Text } = Typography;

export type PlanByCustomerViewProps = {
  plans?: PlanSummary[];
  groups?: PlanCustomerGroup[];
  translate: LocalizationState['translate'];
  wrapWithCard?: boolean;
};

type CustomerTreeNode = {
  key: string;
  title: ReactNode;
  children?: CustomerTreeNode[];
};

export function PlanByCustomerView({
  plans,
  groups,
  translate,
  wrapWithCard = true,
}: PlanByCustomerViewProps) {
  const dataSource = useMemo(() => {
    if (groups && groups.length > 0) {
      return groups;
    }
    const source: PlanSummaryWithCustomer[] =
      plans && plans.length > 0
        ? (plans as PlanSummaryWithCustomer[])
        : (listMockPlans() as PlanSummaryWithCustomer[]);
    return aggregatePlansByCustomer(source, { sortBy: 'total', descending: true });
  }, [plans, groups]);

  const content = dataSource.length === 0 ? (
    <Empty description={translate('planEmpty')} />
  ) : (
    <List
      itemLayout="vertical"
      dataSource={dataSource}
      renderItem={(group: PlanCustomerGroup) => (
        <List.Item
          key={`${group.customerId ?? 'unknown'}:${group.customerName}`}
          className="plan-customer-group"
        >
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Space align="center" size="large" wrap>
              <Typography.Title level={5} style={{ margin: 0 }}>
                {resolveGroupName(group, translate)}
              </Typography.Title>
              <Tag color="blue">{group.total}</Tag>
              <Space direction="vertical" size={4}>
                <Text type="secondary">{translate('planTableHeaderProgress')}</Text>
                <Progress
                  percent={Math.max(0, Math.min(100, Math.round(group.progressAverage ?? 0)))}
                  size="small"
                />
              </Space>
            </Space>

            {group.owners.length > 0 && (
              <Space size={8} wrap>
                <Tag color="geekblue">{translate('planTableHeaderOwner')}</Tag>
                {group.owners.map((owner: string) => (
                  <Tag key={owner}>{owner}</Tag>
                ))}
              </Space>
            )}

            <Tree
              showLine
              selectable={false}
              defaultExpandAll
              treeData={buildCustomerTreeData(group, translate)}
            />
          </Space>
        </List.Item>
      )}
    />
  );

  if (!wrapWithCard) {
    return <div className="plan-by-customer-view">{content}</div>;
  }

  return (
    <Card title={translate('planDetailCustomerLabel')} bordered={false} className="card-block">
      {content}
    </Card>
  );
}

function resolveGroupName(group: PlanCustomerGroup, translate: LocalizationState['translate']) {
  if (group.hasCustomer) {
    return group.customerName;
  }
  return `${translate('planDetailCustomerLabel')} - ${translate('planPreviewEmptyValue')}`;
}

function buildCustomerTreeData(
  group: PlanCustomerGroup,
  translate: LocalizationState['translate']
): CustomerTreeNode[] {
  const nodes: CustomerTreeNode[] = [];
  PLAN_STATUS_ORDER.forEach((status) => {
    const plans = group.plans.filter((plan) => plan.status === status);
    if (plans.length === 0) {
      return;
    }
    const statusNode: CustomerTreeNode = {
      key: `${group.customerId ?? 'unknown'}:${status}`,
      title: (
        <Space size={6} align="center">
          <Tag color={PLAN_STATUS_COLOR[status]}>{translate(PLAN_STATUS_LABEL[status])}</Tag>
          <Text type="secondary">({plans.length})</Text>
        </Space>
      ),
      children: sortPlansForDisplay(plans).map((plan) => ({
        key: plan.id,
        title: (
          <Space direction="vertical" size={0} className="plan-customer-tree-leaf">
            <Text strong>{plan.title}</Text>
            <Text type="secondary">
              {translate('planTableHeaderWindow')}: {formatPlanWindowLabel(plan, translate)}
            </Text>
          </Space>
        ),
      })),
    };
    nodes.push(statusNode);
  });
  return nodes;
}

function formatPlanWindowLabel(
  plan: PlanSummaryWithCustomer,
  translate: LocalizationState['translate']
): string {
  const empty = translate('planPreviewEmptyValue');
  const start = plan.plannedStartTime ? new Date(plan.plannedStartTime).toLocaleString() : empty;
  const end = plan.plannedEndTime ? new Date(plan.plannedEndTime).toLocaleString() : empty;
  return `${start} → ${end}`;
}

function sortPlansForDisplay(
  plans: readonly PlanSummaryWithCustomer[]
): PlanSummaryWithCustomer[] {
  if (!plans || plans.length === 0) {
    return [];
  }
  if (plans.length === 1) {
    return plans.slice();
  }
  return plans
    .slice()
    .sort((a, b) => {
      const timeA = getPlanTimeValue(a);
      const timeB = getPlanTimeValue(b);
      if (timeA === timeB) {
        return a.title.localeCompare(b.title);
      }
      return timeA - timeB;
    });
}

function getPlanTimeValue(plan: PlanSummaryWithCustomer): number {
  const anchor = plan.plannedStartTime ?? plan.plannedEndTime ?? null;
  if (!anchor) {
    return Number.POSITIVE_INFINITY;
  }
  const value = new Date(anchor).getTime();
  return Number.isFinite(value) ? value : Number.POSITIVE_INFINITY;
}
