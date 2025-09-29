import React from '../../vendor/react/index.js';
import { Button, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { PlanReminderSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_REMINDER_CHANNEL_COLOR, PLAN_REMINDER_CHANNEL_LABEL } from '../constants/planReminder';

const { Text } = Typography;

type PlanReminderBoardProps = {
  reminders: PlanReminderSummary[];
  translate: LocalizationState['translate'];
  onEdit: (reminder: PlanReminderSummary) => void;
  onToggle: (reminder: PlanReminderSummary) => void;
  selectedReminderId: string | null;
};

export function PlanReminderBoard({
  reminders,
  translate,
  onEdit,
  onToggle,
  selectedReminderId,
}: PlanReminderBoardProps) {
  if (!reminders || reminders.length === 0) {
    return null;
  }

  return (
    <div className="plan-reminder-board">
      {reminders.map((reminder) => {
        const selected = reminder.id === selectedReminderId;
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
              <Button type="primary" size="small" onClick={() => onEdit(reminder)}>
                {translate('planDetailReminderActionEdit')}
              </Button>
              <Button type="default" size="small" onClick={() => onToggle(reminder)}>
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
