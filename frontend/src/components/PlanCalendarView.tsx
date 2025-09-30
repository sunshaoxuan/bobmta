import React, { useMemo, useState } from '../../vendor/react/index.js';
import { Button, Card, Empty, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import {
  transformPlansToCalendarBuckets,
  type PlanCalendarBucket,
  type PlanCalendarGranularity,
  type PlanSummaryWithCustomer,
} from '../state/planList';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import { listMockPlans } from '../mocks/planList';

const { Text } = Typography;

const GRANULARITY_OPTIONS: Array<{ label: string; value: PlanCalendarGranularity }> = [
  { label: 'Day', value: 'day' },
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
  { label: 'Year', value: 'year' },
];

export type PlanCalendarViewProps = {
  plans?: PlanSummary[];
  translate: LocalizationState['translate'];
};

export function PlanCalendarView({ plans, translate }: PlanCalendarViewProps) {
  const [granularity, setGranularity] = useState<PlanCalendarGranularity>('month');
  const buckets = useMemo(() => {
    const source: PlanSummaryWithCustomer[] =
      plans && plans.length > 0 ? (plans as PlanSummaryWithCustomer[]) : (listMockPlans() as PlanSummaryWithCustomer[]);
    return transformPlansToCalendarBuckets(source, { granularity });
  }, [plans, granularity]);

  return (
    <Card
      title={translate('planDetailTimelineTitle')}
      bordered={false}
      className="card-block"
      extra={
        <Space size="small">
          {GRANULARITY_OPTIONS.map((option) => (
            <Button
              key={option.value}
              type={granularity === option.value ? 'primary' : 'default'}
              size="small"
              onClick={() => setGranularity(option.value)}
            >
              {option.label}
            </Button>
          ))}
        </Space>
      }
    >
      {buckets.length === 0 ? (
        <Empty description={translate('planDetailTimelineEmpty')} />
      ) : (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          {buckets.map((bucket) => (
            <div key={bucket.key} className="plan-calendar-bucket-wrapper">
              <CalendarBucketItem bucket={bucket} translate={translate} />
            </div>
          ))}
        </Space>
      )}
    </Card>
  );
}

type CalendarBucketItemProps = {
  bucket: PlanCalendarBucket;
  translate: LocalizationState['translate'];
};

function CalendarBucketItem({ bucket, translate }: CalendarBucketItemProps) {
  return (
    <div className="plan-calendar-bucket">
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center" size="large" wrap>
          <Tag color="purple">{bucket.label}</Tag>
          <Text type="secondary">
            {translate('planTableHeaderWindow')}: {formatRange(bucket.start, bucket.end)}
          </Text>
        </Space>
        <Space direction="vertical" style={{ width: '100%' }} size="small">
          {bucket.events.map((event) => (
            <div key={event.plan.id} className="plan-calendar-event">
              <Space size="small" align="center">
                <Tag color={PLAN_STATUS_COLOR[event.plan.status]}>
                  {translate(PLAN_STATUS_LABEL[event.plan.status])}
                </Tag>
                <Text strong>{event.plan.title}</Text>
              </Space>
              <Text type="secondary">
                {formatEventTime(event.startTime, event.endTime, translate)}
                {typeof event.durationMinutes === 'number' && event.durationMinutes > 0
                  ? ` · ${event.durationMinutes} min`
                  : ''}
              </Text>
            </div>
          ))}
        </Space>
      </Space>
    </div>
  );
}

function formatEventTime(
  start: string | null,
  end: string | null,
  translate: LocalizationState['translate']
): string {
  const empty = translate('planPreviewEmptyValue');
  const startLabel = start ? new Date(start).toLocaleString() : empty;
  const endLabel = end ? new Date(end).toLocaleString() : empty;
  return `${startLabel} → ${endLabel}`;
}

function formatRange(startIso: string, endIso: string): string {
  return `${new Date(startIso).toLocaleDateString()} → ${new Date(endIso).toLocaleDateString()}`;
}
