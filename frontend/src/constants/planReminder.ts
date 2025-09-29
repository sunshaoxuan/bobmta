import type { PlanReminderChannel } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

export const PLAN_REMINDER_CHANNEL_LABEL: Record<PlanReminderChannel, UiMessageKey> = {
  EMAIL: 'planReminderChannelEmail',
  SMS: 'planReminderChannelSms',
  IM: 'planReminderChannelIm',
  WEBHOOK: 'planReminderChannelWebhook',
};

export const PLAN_REMINDER_CHANNEL_COLOR: Record<PlanReminderChannel, string> = {
  EMAIL: 'geekblue',
  SMS: 'orange',
  IM: 'purple',
  WEBHOOK: 'green',
};
