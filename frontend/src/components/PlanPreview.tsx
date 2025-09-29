import React, { type ReactNode } from '../../vendor/react/index.js';
import {
  Alert,
  Button,
  Card,
  Space,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { Locale } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import { formatDateTime, formatPlanWindow } from '../utils/planFormatting';

const { Text, Paragraph } = Typography;

type PlanPreviewProps = {
  plan: PlanSummary | null;
  translate: LocalizationState['translate'];
  locale: Locale;
  onClose: () => void;
};

export function PlanPreview({ plan, translate, locale, onClose }: PlanPreviewProps) {
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
          <Paragraph className="plan-preview-description">
            {translate('planPreviewDescription')}
          </Paragraph>
          <div className="plan-preview-grid">
            <PreviewField label={translate('planPreviewOwnerLabel')}>
              <Tag color="blue">{plan.owner}</Tag>
            </PreviewField>
            <PreviewField label={translate('planPreviewWindowLabel')}>
              <Text>{formatPlanWindow(plan, locale, translate)}</Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewParticipantsLabel')}>
              <Space size="small" wrap>
                {plan.participants.map((participant) => (
                  <Tag key={participant} color="purple">
                    {participant}
                  </Tag>
                ))}
                {plan.participants.length === 0 && (
                  <Text type="secondary">{translate('planPreviewParticipantsEmpty')}</Text>
                )}
              </Space>
            </PreviewField>
            <PreviewField label={translate('planPreviewStartLabel')}>
              <Text>
                {formatDateTime(plan.plannedStartTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewEndLabel')}>
              <Text>
                {formatDateTime(plan.plannedEndTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewProgressLabel')}>
              <Text strong>{normalizeProgress(plan.progress)}%</Text>
            </PreviewField>
          </div>
          <Alert
            type="success"
            showIcon
            message={translate('planPreviewComingTitle')}
            description={translate('planPreviewComingDescription')}
          />
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

function normalizeProgress(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(value)));
}
