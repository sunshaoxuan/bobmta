import React from '../../vendor/react/index.js';
import { Button, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { PlanReminderSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_REMINDER_CHANNEL_COLOR, PLAN_REMINDER_CHANNEL_LABEL } from '../constants/planReminder';
import type { PlanViewMode } from '../constants/planMode';

const { Text } = Typography;

type PlanReminderBoardProps = {
  reminders: PlanReminderSummary[];
  translate: LocalizationState['translate'];
  mode: PlanViewMode;
  onEdit: (reminder: PlanReminderSummary) => void;
  onToggle: (reminder: PlanReminderSummary) => void;
  selectedReminderId: string | null;
  pendingReminderId: string | null;
  pendingStatus: 'idle' | 'loading';
};

export function PlanReminderBoard({
  reminders,
  translate,
  mode,
  onEdit,
  onToggle,
  selectedReminderId,
  pendingReminderId,
  pendingStatus,
}: PlanReminderBoardProps) {
  if (!reminders || reminders.length === 0) {
    return null;
  }

  const allowEdit = mode === 'design';

  return (
    <div className="plan-reminder-board" data-mode={mode}>
      {reminders.map((reminder) => {
        const selected = reminder.id === selectedReminderId;
        const isPending = pendingReminderId === reminder.id && pendingStatus === 'loading';
        return (
          <article
            key={reminder.id}
            className={['plan-reminder-card', selected ? 'plan-reminder-card-selected' : '']
              .filter(Boolean)
              .join(' ')}
          >
            <header className="plan-reminder-card-header">
              <Tag color={PLAN_REMINDER_CHANNEL_COLOR[reminder.channel]}>
                {translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])}
              </Tag>
              {!reminder.active ? (
                <Tag color="default">{translate('planDetailReminderInactive')}</Tag>
              ) : null}
            </header>
            <Text>{translate('planDetailReminderOffsetMinutes', { minutes: reminder.offsetMinutes })}</Text>
            {reminder.description ? (
              <Text type="secondary" className="plan-reminder-card-description">
                {reminder.description}
              </Text>
            ) : null}
            <Space size="small" className="plan-reminder-card-actions" wrap>
              {allowEdit ? (
                <Button
                  type="primary"
                  size="small"
                  onClick={() => onEdit(reminder)}
                  loading={isPending}
                  disabled={isPending}
                >
                  {translate('planDetailReminderActionEdit')}
                </Button>
              ) : null}
              <Button
                type="default"
                size="small"
                onClick={() => onToggle(reminder)}
                loading={isPending}
                disabled={isPending}
              >
                {translate('planDetailReminderActionToggle')}
              </Button>
            </Space>
          </article>
        );
      })}
      <Text type="secondary" className="plan-reminder-board-helper">
        {translate('planDetailReminderSelectionHint')}
      </Text>
    </div>
  );
}
