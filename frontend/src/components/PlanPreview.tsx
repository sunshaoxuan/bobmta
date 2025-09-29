import React, { type ReactNode } from '../../vendor/react/index.js';
import {
  Alert,
  Button,
  Card,
  Space,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type { PlanDetail, PlanReminderSummary, PlanSummary, PlanTimelineEntry } from '../api/types';
import type { ApiError } from '../api/client';
import type { Locale } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import { PLAN_REMINDER_CHANNEL_COLOR, PLAN_REMINDER_CHANNEL_LABEL } from '../constants/planReminder';
import { formatDateTime, formatPlanWindow } from '../utils/planFormatting';
import type { PlanDetailState } from '../state/planDetail';
import { PlanNodeTree } from './PlanNodeTree';

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
            <section className="plan-preview-section plan-preview-nodes-section">
              <div className="plan-preview-section-header">
                <Text strong>{translate('planDetailNodesTitle')}</Text>
              </div>
              <DetailRemoteSection
                status={detailStatus}
                error={detailError}
                translate={translate}
                empty={detailStatus === 'success' && nodes.length === 0}
                onRetry={onRefreshDetail}
                errorDetail={detailErrorDetail}
                emptyMessage={translate('planDetailNodesEmpty')}
              >
                <PlanNodeTree nodes={nodes} translate={translate} locale={locale} />
              </DetailRemoteSection>
            </section>
            <section>
              <div className="plan-preview-section-header">
                <Text strong>{translate('planDetailTimelineTitle')}</Text>
              </div>
              <DetailRemoteSection
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
              </DetailRemoteSection>
            </section>
            <section>
              <div className="plan-preview-section-header">
                <Text strong>{translate('planDetailRemindersTitle')}</Text>
              </div>
              <DetailRemoteSection
                status={detailStatus}
                error={detailError}
                translate={translate}
                empty={reminders.length === 0}
                onRetry={onRefreshDetail}
                errorDetail={detailErrorDetail}
                emptyMessage={translate('planDetailRemindersEmpty')}
              >
                <ul className="plan-preview-reminders">
                  {reminders.map((reminder) => (
                    <li key={reminder.id} className="plan-preview-reminders-item">
                      <Space size="small" align="center" wrap>
                        <Tag color={PLAN_REMINDER_CHANNEL_COLOR[reminder.channel]}>
                          {translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])}
                        </Tag>
                        <Text>
                          {translate('planDetailReminderOffsetMinutes', {
                            minutes: reminder.offsetMinutes,
                          })}
                        </Text>
                        {!reminder.active ? (
                          <Tag color="default">{translate('planDetailReminderInactive')}</Tag>
                        ) : null}
                      </Space>
                      {reminder.description ? (
                        <Text type="secondary" className="plan-preview-reminder-description">
                          {reminder.description}
                        </Text>
                      ) : null}
                    </li>
                  ))}
                </ul>
              </DetailRemoteSection>
            </section>
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

type DetailRemoteSectionProps = {
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  translate: LocalizationState['translate'];
  empty: boolean;
  onRetry: () => void;
  children: ReactNode;
  errorDetail: string | null;
  emptyMessage: string;
};

function DetailRemoteSection({
  status,
  error,
  translate,
  empty,
  onRetry,
  children,
  errorDetail,
  emptyMessage,
}: DetailRemoteSectionProps) {
  if (status === 'loading') {
    return (
      <Alert
        type="info"
        showIcon
        message={translate('commonStateLoadingTitle')}
        description={translate('commonStateLoadingDescription')}
      />
    );
  }

  if (error) {
    return (
      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        <Alert
          type="error"
          showIcon
          message={translate('commonStateErrorTitle')}
          description={errorDetail ?? translate('commonStateErrorDescription')}
        />
        <div>
          <Button type="primary" size="small" onClick={onRetry}>
            {translate('commonStateRetry')}
          </Button>
        </div>
      </Space>
    );
  }

  if (empty) {
    return (
      <Alert
        type="warning"
        showIcon
        message={emptyMessage}
      />
    );
  }

  return <>{children}</>;
}
