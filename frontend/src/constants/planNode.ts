import type { PlanNodeStatus } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

export const PLAN_NODE_STATUS_COLOR: Record<PlanNodeStatus, string> = {
  PENDING: 'default',
  IN_PROGRESS: 'blue',
  DONE: 'green',
  CANCELLED: 'red',
  SKIPPED: 'orange',
};

export const PLAN_NODE_STATUS_LABEL: Record<PlanNodeStatus, UiMessageKey> = {
  PENDING: 'planDetailNodeStatusPending',
  IN_PROGRESS: 'planDetailNodeStatusInProgress',
  DONE: 'planDetailNodeStatusDone',
  CANCELLED: 'planDetailNodeStatusCancelled',
  SKIPPED: 'planDetailNodeStatusSkipped',
};
