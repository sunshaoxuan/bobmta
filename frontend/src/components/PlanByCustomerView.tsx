import React, { useMemo } from '../../vendor/react/index.js';
import { Card, Empty, Progress, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import {
  aggregatePlansByCustomer,
  type PlanCustomerGroup,
  type PlanSummaryWithCustomer,
} from '../state/planList';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL, PLAN_STATUS_ORDER } from '../constants/planStatus';
import { listMockPlans } from '../mocks/planList';

const { Text, Title } = Typography;

export type PlanByCustomerViewProps = {
  plans?: PlanSummary[];
  groups?: PlanCustomerGroup[];
  translate: LocalizationState['translate'];
};

export function PlanByCustomerView({ plans, groups, translate }: PlanByCustomerViewProps) {
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

  if (dataSource.length === 0) {
    return (
      <Card title={translate('planDetailCustomerLabel')} bordered={false} className="card-block">
        <Empty description={translate('planEmpty')} />
      </Card>
    );
  }

  return (
    <Card title={translate('planDetailCustomerLabel')} bordered={false} className="card-block">
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {dataSource.map((group) => (
          <div key={`${group.customerId ?? 'unknown'}:${group.customerName}`} className="plan-customer-group">
            <CustomerGroupCard group={group} translate={translate} />
          </div>
        ))}
      </Space>
    </Card>
  );
}

type CustomerGroupCardProps = {
  group: PlanCustomerGroup;
  translate: LocalizationState['translate'];
};

function CustomerGroupCard({ group, translate }: CustomerGroupCardProps) {
  const displayName = group.hasCustomer
    ? group.customerName
    : `${translate('planDetailCustomerLabel')} - ${translate('planPreviewEmptyValue')}`;
  const progressValue = Math.max(
    0,
    Math.min(100, Math.round(group.progressAverage ?? 0))
  );
  const statusTags = useMemo(
    () =>
      PLAN_STATUS_ORDER.filter((status) => group.statusCounts[status] > 0).map((status) => ({
        status,
        count: group.statusCounts[status],
      })),
    [group.statusCounts]
  );

  return (
    <Card type="inner" title={<Title level={5}>{displayName}</Title>} extra={<Tag color="blue">{group.total}</Tag>}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space size="small" direction="vertical" style={{ width: '100%' }}>
          <Text type="secondary">{translate('planTableHeaderProgress')}</Text>
          <Progress percent={progressValue} size="small" />
        </Space>
        {statusTags.length > 0 && (
          <Space size={8} wrap>
            {statusTags.map((item) => (
              <Tag key={item.status} color={PLAN_STATUS_COLOR[item.status]}>
                {translate(PLAN_STATUS_LABEL[item.status])}: {item.count}
              </Tag>
            ))}
          </Space>
        )}
        {group.owners.length > 0 && (
          <Space size={8} wrap>
            <Tag color="geekblue">{translate('planTableHeaderOwner')}</Tag>
            {group.owners.map((owner) => (
              <Tag key={owner}>{owner}</Tag>
            ))}
          </Space>
        )}
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          {group.plans.map((plan) => (
            <div key={plan.id} className="plan-customer-item">
              <Space size="small" align="center">
                <Tag color={PLAN_STATUS_COLOR[plan.status]}>
                  {translate(PLAN_STATUS_LABEL[plan.status])}
                </Tag>
                <Text strong>{plan.title}</Text>
              </Space>
              <Text type="secondary">
                {translate('planTableHeaderWindow')}: {formatPlanWindowLabel(plan, translate)}
              </Text>
            </div>
          ))}
        </Space>
      </Space>
    </Card>
  );
}

function formatPlanWindowLabel(plan: PlanSummaryWithCustomer, translate: LocalizationState['translate']): string {
  const start = plan.plannedStartTime ?? translate('planPreviewEmptyValue');
  const end = plan.plannedEndTime ?? translate('planPreviewEmptyValue');
  return `${start} → ${end}`;
}
