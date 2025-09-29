import React, { useEffect, useMemo, useState, type ReactNode } from '../../vendor/react/index.js';
import {
  Alert,
  Button,
  Card,
  Space,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type {
  PlanDetail,
  PlanReminderChannel,
  PlanReminderSummary,
  PlanSummary,
  PlanTimelineEntry,
} from '../api/types';
import type { ApiError } from '../api/client';
import type { Locale, UiMessageKey } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import { PLAN_REMINDER_CHANNEL_COLOR, PLAN_REMINDER_CHANNEL_LABEL } from '../constants/planReminder';
import { formatDateTime, formatPlanWindow } from '../utils/planFormatting';
import type { PlanDetailState } from '../state/planDetail';
import { PlanNodeTree } from './PlanNodeTree';
import { PlanDetailSection } from './PlanDetailSection';
import { PlanNodeActions, type PlanNodeActionIntent } from './PlanNodeActions';
import { PlanReminderBoard } from './PlanReminderBoard';
import { getActionablePlanNodes, type PlanNodeWithPath, type PlanNodeActionType } from '../utils/planNodes';

const { Text, Paragraph } = Typography;

type PlanPreviewProps = {
  plan: PlanSummary | null;
  translate: LocalizationState['translate'];
  locale: Locale;
  onClose: () => void;
  detailState: PlanDetailState;
  onRefreshDetail: () => void;
  detailErrorDetail: string | null;
};

export function PlanPreview({
  plan,
  translate,
  locale,
  onClose,
  detailState,
  onRefreshDetail,
  detailErrorDetail,
}: PlanPreviewProps) {
  const isActiveDetail = Boolean(plan && detailState.activePlanId === plan.id);
  const detail: PlanDetail | null = isActiveDetail ? detailState.detail : null;
  const timeline: PlanTimelineEntry[] = isActiveDetail ? detailState.timeline : [];
  const reminders: PlanReminderSummary[] = isActiveDetail ? detailState.reminders : [];
  const nodes = detail?.nodes ?? [];
  const detailStatus = isActiveDetail ? detailState.status : 'idle';
  const detailError = isActiveDetail ? detailState.error : null;
  const detailOrigin = isActiveDetail ? detailState.origin : null;
  const lastUpdatedLabel =
    isActiveDetail && detailState.lastUpdated
      ? translate('planDetailLastUpdated', {
          time: formatDateTime(detailState.lastUpdated, locale) ?? detailState.lastUpdated,
        })
      : null;

  const summary = detail ?? plan;
  const description = detail?.description ?? translate('planDetailDescriptionFallback');
  const tags = detail?.tags ?? [];
  const participantNames = detail
    ? detail.participants.map((participant) => participant.name)
    : plan?.participants ?? [];
  const [pendingAction, setPendingAction] = useState<PlanNodeActionIntent | null>(null);
  const [reminderDrafts, setReminderDrafts] = useState<Record<string, boolean>>({});
  const [selectedReminderId, setSelectedReminderId] = useState<string | null>(null);
  const [pendingReminderIntent, setPendingReminderIntent] = useState<ReminderActionIntent | null>(null);

  useEffect(() => {
    setPendingAction(null);
    setReminderDrafts({});
    setSelectedReminderId(null);
    setPendingReminderIntent(null);
  }, [detail?.id, plan?.id]);

  const actionableNodes: PlanNodeWithPath[] = useMemo(
    () => getActionablePlanNodes(nodes),
    [nodes]
  );

  const effectiveReminders = useMemo(
    () =>
      reminders.map((reminder) => {
        const override = reminderDrafts[reminder.id];
        if (typeof override === 'boolean') {
          return { ...reminder, active: override };
        }
        return reminder;
      }),
    [reminders, reminderDrafts]
  );

  const actionHelper = pendingAction
    ? (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailActionPending', {
            action: translate(ACTION_LABEL_KEY[pendingAction.action]),
            node: pendingAction.nodeName,
          })}
        />
      )
    : null;

  const reminderHelper = pendingReminderIntent
    ? (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailReminderActionPending', {
            action: translate(
              pendingReminderIntent.action === 'edit'
                ? 'planDetailReminderActionEdit'
                : 'planDetailReminderActionToggle'
            ),
            channel: translate(PLAN_REMINDER_CHANNEL_LABEL[pendingReminderIntent.channel]),
            offset: translate('planDetailReminderOffsetMinutes', {
              minutes: pendingReminderIntent.offset,
            }),
          })}
        />
      )
    : null;

  const handleNodeAction = (intent: PlanNodeActionIntent) => {
    setPendingAction(intent);
  };

  const handleReminderToggle = (reminder: PlanReminderSummary) => {
    setReminderDrafts((current) => {
      const currentActive = Object.prototype.hasOwnProperty.call(current, reminder.id)
        ? current[reminder.id]
        : reminder.active;
      return { ...current, [reminder.id]: !currentActive };
    });
    setSelectedReminderId(reminder.id);
    setPendingReminderIntent({
      reminderId: reminder.id,
      action: 'toggle',
      channel: reminder.channel,
      offset: reminder.offsetMinutes,
    });
  };

  const handleReminderEdit = (reminder: PlanReminderSummary) => {
    setSelectedReminderId(reminder.id);
    setPendingReminderIntent({
      reminderId: reminder.id,
      action: 'edit',
      channel: reminder.channel,
      offset: reminder.offsetMinutes,
    });
  };

  return (
    <Card
      title={translate('planPreviewHeader')}
      className="plan-preview"
      extra={
        plan ? (
          <Button type="text" className="plan-preview-close" onClick={onClose}>
            {translate('planPreviewClose')}
          </Button>
        ) : null
      }
    >
      {!plan ? (
        <Alert
          type="info"
          showIcon
          message={translate('planPreviewEmptyTitle')}
          description={<Paragraph className="plan-preview-empty-text">{translate('planPreviewEmptyDescription')}</Paragraph>}
        />
      ) : (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div className="plan-preview-header">
            <Text className="plan-preview-title">{plan.title}</Text>
            <Tag color={PLAN_STATUS_COLOR[plan.status]}>
              {translate(PLAN_STATUS_LABEL[plan.status])}
            </Tag>
          </div>
          <Paragraph className="plan-preview-description">{description}</Paragraph>
          <div className="plan-preview-meta">
            {lastUpdatedLabel ? (
              <Text type="secondary" className="plan-preview-meta-item">
                {lastUpdatedLabel}
              </Text>
            ) : null}
            {detailOrigin ? (
              <Tag color={detailOrigin === 'cache' ? 'gold' : 'geekblue'}>
                {translate(
                  detailOrigin === 'cache' ? 'planDetailOriginCache' : 'planDetailOriginNetwork'
                )}
              </Tag>
            ) : null}
            <Button
              type="link"
              size="small"
              onClick={onRefreshDetail}
              loading={detailStatus === 'loading'}
            >
              {translate('planDetailRefresh')}
            </Button>
          </div>
          <div className="plan-preview-grid">
            <PreviewField label={translate('planPreviewOwnerLabel')}>
              <Tag color="blue">{plan.owner}</Tag>
            </PreviewField>
            <PreviewField label={translate('planDetailCustomerLabel')}>
              <Text>{detail?.customer?.name ?? translate('planPreviewEmptyValue')}</Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewWindowLabel')}>
              <Text>{formatPlanWindow(plan, locale, translate)}</Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewParticipantsLabel')}>
              <Space size="small" wrap>
                {participantNames.length > 0 ? (
                  participantNames.map((participant) => (
                    <Tag key={participant} color="purple">
                      {participant}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">{translate('planPreviewParticipantsEmpty')}</Text>
                )}
              </Space>
            </PreviewField>
            <PreviewField label={translate('planDetailTagsLabel')}>
              <Space size="small" wrap>
                {tags.length > 0 ? (
                  tags.map((tag) => (
                    <Tag key={tag} color="blue">
                      {tag}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">{translate('planPreviewParticipantsEmpty')}</Text>
                )}
              </Space>
            </PreviewField>
            <PreviewField label={translate('planPreviewStartLabel')}>
              <Text>
                {formatDateTime(summary?.plannedStartTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewEndLabel')}>
              <Text>
                {formatDateTime(summary?.plannedEndTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewProgressLabel')}>
              <Text strong>{normalizeProgress(summary?.progress ?? plan.progress)}%</Text>
            </PreviewField>
          </div>
          {detailError ? (
            <Alert
              type="error"
              showIcon
              message={translate('planError', {
                error: detailErrorDetail ?? translate('commonStateErrorDescription'),
              })}
            />
          ) : null}
          <div className="plan-preview-sections">
            <PlanDetailSection
              title={translate('planDetailNodesTitle')}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={detailStatus === 'success' && nodes.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailNodesEmpty')}
              className="plan-preview-nodes-section"
            >
              <PlanNodeTree nodes={nodes} translate={translate} locale={locale} />
            </PlanDetailSection>
            <PlanDetailSection
              title={translate('planDetailActionsTitle')}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={actionableNodes.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailActionsEmpty')}
              helper={actionHelper}
            >
              <PlanNodeActions
                candidates={actionableNodes}
                translate={translate}
                onAction={handleNodeAction}
                pendingAction={pendingAction}
              />
            </PlanDetailSection>
            <PlanDetailSection
              title={translate('planDetailTimelineTitle')}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={timeline.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailTimelineEmpty')}
            >
              <ul className="plan-preview-timeline">
                {timeline.map((entry) => (
                  <li key={entry.id} className="plan-preview-timeline-item">
                    <div className="plan-preview-timeline-time">
                      {formatDateTime(entry.occurredAt, locale) ?? entry.occurredAt}
                    </div>
                    <div className="plan-preview-timeline-body">
                      <Text>{entry.message}</Text>
                      {entry.actor ? (
                        <Tag color="cyan" className="plan-preview-timeline-actor">
                          {entry.actor.name}
                        </Tag>
                      ) : null}
                    </div>
                  </li>
                ))}
              </ul>
            </PlanDetailSection>
            <PlanDetailSection
              title={translate('planDetailRemindersTitle')}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={effectiveReminders.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailRemindersEmpty')}
              helper={reminderHelper}
            >
              <PlanReminderBoard
                reminders={effectiveReminders}
                translate={translate}
                onEdit={handleReminderEdit}
                onToggle={handleReminderToggle}
                selectedReminderId={selectedReminderId}
              />
            </PlanDetailSection>
          </div>
        </Space>
      )}
    </Card>
  );
}

type PreviewFieldProps = {
  label: string;
  children: ReactNode;
};

function PreviewField({ label, children }: PreviewFieldProps) {
  return (
    <div className="plan-preview-field">
      <Text type="secondary" className="plan-preview-field-label">
        {label}
      </Text>
      <div className="plan-preview-field-value">{children}</div>
    </div>
  );
}

function normalizeProgress(value: number | undefined): number {
  if (!Number.isFinite(value ?? Number.NaN)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(value ?? 0)));
}

const ACTION_LABEL_KEY: Record<PlanNodeActionType, UiMessageKey> = {
  start: 'planDetailActionStart',
  complete: 'planDetailActionComplete',
  handover: 'planDetailActionHandover',
};

type ReminderActionIntent = {
  reminderId: string;
  action: 'edit' | 'toggle';
  channel: PlanReminderChannel;
  offset: number;
};
