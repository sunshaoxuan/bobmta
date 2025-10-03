import React, { useCallback, useEffect, useMemo, useState } from '../../vendor/react/index.js';
import {
  Calendar,
  Card,
  Empty,
  List,
  Segmented,
  Space,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import {
  createPlanCalendarEvents,
  groupCalendarEvents,
  type PlanCalendarBucket,
  type PlanCalendarEvent,
  type PlanCalendarGranularity,
  type PlanSummaryWithCustomer,
} from '../state/planList';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import { listMockPlans } from '../mocks/planList';
import {
  getPlanCalendarEventTime,
  mapPlanCalendarEventsByDate,
  selectUpcomingPlanCalendarEvents,
} from '../utils/planList';
import type { ReactNode } from '../../vendor/react/index.js';

const { Text } = Typography;

type CalendarMode = 'month' | 'year';

export type PlanCalendarViewProps = {
  plans?: PlanSummary[];
  events?: PlanCalendarEvent[];
  buckets?: PlanCalendarBucket[];
  translate: LocalizationState['translate'];
  wrapWithCard?: boolean;
};

export function PlanCalendarView({
  plans,
  events,
  buckets: prefetchedBuckets,
  translate,
  wrapWithCard = true,
}: PlanCalendarViewProps) {
  const [calendarMode, setCalendarMode] = useState<CalendarMode>('month');
  const [granularity, setGranularity] = useState<PlanCalendarGranularity>('month');

  useEffect(() => {
    setCalendarMode((current) => {
      if (granularity === 'year' && current !== 'year') {
        return 'year';
      }
      if (granularity !== 'year' && current !== 'month') {
        return 'month';
      }
      return current;
    });
  }, [granularity]);

  const granularityOptions = useMemo(
    () => [
      { value: 'day' as PlanCalendarGranularity, label: translate('planCalendarGranularityDay') },
      { value: 'week' as PlanCalendarGranularity, label: translate('planCalendarGranularityWeek') },
      { value: 'month' as PlanCalendarGranularity, label: translate('planCalendarGranularityMonth') },
      { value: 'year' as PlanCalendarGranularity, label: translate('planCalendarGranularityYear') },
    ],
    [translate]
  );

  const calendarEvents = useMemo(() => {
    if (events && events.length > 0) {
      return normalizeCalendarEvents(events);
    }
    const source: PlanSummaryWithCustomer[] =
      plans && plans.length > 0
        ? (plans as PlanSummaryWithCustomer[])
        : (listMockPlans() as PlanSummaryWithCustomer[]);
    return normalizeCalendarEvents(createPlanCalendarEvents(source));
  }, [plans, events]);

  const eventsByDate = useMemo(
    () => mapPlanCalendarEventsByDate(calendarEvents),
    [calendarEvents]
  );

  const buckets = useMemo(() => {
    if (
      prefetchedBuckets &&
      prefetchedBuckets.length > 0 &&
      prefetchedBuckets.every((bucket) => bucket.granularity === granularity)
    ) {
      return prefetchedBuckets;
    }
    return groupCalendarEvents(calendarEvents, { granularity });
  }, [calendarEvents, granularity, prefetchedBuckets]);

  const granularityLabel = useMemo(() => {
    const option = granularityOptions.find((item) => item.value === granularity);
    return option ? option.label : translate('planCalendarGranularityMonth');
  }, [granularityOptions, granularity, translate]);

  const upcomingEvents = useMemo(
    () => selectUpcomingPlanCalendarEvents(calendarEvents),
    [calendarEvents]
  );

  const handlePanelChange = useCallback((_: unknown, nextMode: CalendarMode) => {
    setCalendarMode(nextMode);
  }, []);

  const renderCalendarCell = useCallback(
    (current: any, info: { type: 'date' | 'month'; originNode: ReactNode }) => {
      if (info.type === 'month') {
        return info.originNode;
      }
      const dateKey = current?.format ? current.format('YYYY-MM-DD') : String(current);
      const dayEvents = eventsByDate[dateKey] ?? [];
      if (dayEvents.length === 0) {
        return info.originNode;
      }
      return (
        <div className="plan-calendar-cell">
          <div className="plan-calendar-cell-value">{info.originNode}</div>
          <ul className="plan-calendar-cell-events">
            {dayEvents.slice(0, 3).map((event) => (
              <li key={event.plan.id} className="plan-calendar-cell-event">
                <Tag color={PLAN_STATUS_COLOR[event.plan.status]}>
                  {translate(PLAN_STATUS_LABEL[event.plan.status])}
                </Tag>
                <span>{event.plan.title}</span>
              </li>
            ))}
            {dayEvents.length > 3 && (
              <li className="plan-calendar-cell-more">+{dayEvents.length - 3}</li>
            )}
          </ul>
        </div>
      );
    },
    [eventsByDate, translate]
  );

  const control = (
    <Segmented
      size="small"
      value={granularity}
      options={granularityOptions}
      onChange={(value) => {
        if (typeof value === 'string') {
          const nextGranularity = value as PlanCalendarGranularity;
          if (nextGranularity !== granularity) {
            setGranularity(nextGranularity);
          }
        }
      }}
    />
  );

  const content = calendarEvents.length === 0 ? (
    <Empty description={translate('planDetailTimelineEmpty')} />
  ) : (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Calendar
        fullscreen={false}
        mode={calendarMode}
        onPanelChange={handlePanelChange}
        cellRender={renderCalendarCell}
      />

      <List
        header={<Text strong>{granularityLabel}</Text>}
        dataSource={buckets}
        renderItem={(bucket: PlanCalendarBucket) => (
          <CalendarBucketItem bucket={bucket} translate={translate} />
        )}
      />

      <List
        header={<Text strong>{translate('planDetailTimelineTitle')}</Text>}
        dataSource={upcomingEvents}
        renderItem={(event: PlanCalendarEvent) => (
          <List.Item key={event.plan.id} className="plan-calendar-event">
            <Space direction="vertical" style={{ width: '100%' }} size={0}>
              <Space align="center" size="small">
                <Tag color={PLAN_STATUS_COLOR[event.plan.status]}>
                  {translate(PLAN_STATUS_LABEL[event.plan.status])}
                </Tag>
                <Text strong>{event.plan.title}</Text>
              </Space>
              <Text type="secondary">{formatEventRange(event, translate)}</Text>
            </Space>
          </List.Item>
        )}
      />
    </Space>
  );

  if (!wrapWithCard) {
    return (
      <div className="plan-calendar-view">
        <div className="plan-calendar-toolbar">{control}</div>
        {content}
      </div>
    );
  }

  return (
    <Card
      title={translate('planDetailTimelineTitle')}
      bordered={false}
      className="card-block"
      extra={control}
    >
      {content}
    </Card>
  );
}

type CalendarBucketItemProps = {
  bucket: PlanCalendarBucket;
  translate: LocalizationState['translate'];
};

function CalendarBucketItem({ bucket, translate }: CalendarBucketItemProps) {
  return (
    <List.Item className="plan-calendar-bucket" key={bucket.key}>
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        <Space align="center" size="large" wrap>
          <Tag color="purple">{bucket.label}</Tag>
          <Text type="secondary">{formatRange(bucket.start, bucket.end)}</Text>
        </Space>
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          {bucket.events.map((event) => (
            <Space key={event.plan.id} direction="vertical" size={0} className="plan-calendar-bucket-event">
              <Space align="center" size="small">
                <Tag color={PLAN_STATUS_COLOR[event.plan.status]}>
                  {translate(PLAN_STATUS_LABEL[event.plan.status])}
                </Tag>
                <Text strong>{event.plan.title}</Text>
              </Space>
              <Text type="secondary">{formatEventRange(event, translate)}</Text>
            </Space>
          ))}
        </Space>
      </Space>
    </List.Item>
  );
}

function formatEventRange(event: PlanCalendarEvent, translate: LocalizationState['translate']): string {
  const empty = translate('planPreviewEmptyValue');
  const start = event.startTime ? new Date(event.startTime).toLocaleString() : empty;
  const end = event.endTime ? new Date(event.endTime).toLocaleString() : empty;
  const duration =
    typeof event.durationMinutes === 'number' && event.durationMinutes > 0
      ? ` · ${event.durationMinutes} min`
      : '';
  return `${start} → ${end}${duration}`;
}

function formatRange(startIso: string, endIso: string): string {
  const start = new Date(startIso).toLocaleDateString();
  const end = new Date(endIso).toLocaleDateString();
  return `${start} → ${end}`;
}

function normalizeCalendarEvents<T extends PlanSummaryWithCustomer>(
  events: readonly PlanCalendarEvent<T>[]
): PlanCalendarEvent<T>[] {
  if (!events || events.length === 0) {
    return [];
  }
  return events
    .slice()
    .sort((a, b) => {
      const timeA = getPlanCalendarEventTime(a);
      const timeB = getPlanCalendarEventTime(b);
      if (timeA === timeB) {
        return (a.plan.title ?? '').localeCompare(b.plan.title ?? '');
      }
      return timeA - timeB;
    });
}
