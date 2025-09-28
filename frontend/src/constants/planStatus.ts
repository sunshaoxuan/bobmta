import type { PlanStatus } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

export const PLAN_STATUS_LABEL: Record<PlanStatus, UiMessageKey> = {
  DESIGN: 'planStatusDesign',
  SCHEDULED: 'planStatusScheduled',
  IN_PROGRESS: 'planStatusInProgress',
  COMPLETED: 'planStatusCompleted',
  CANCELLED: 'planStatusCancelled',
};

export const PLAN_STATUS_COLOR: Record<
  PlanStatus,
  'default' | 'processing' | 'success' | 'error' | 'warning'
> = {
  DESIGN: 'default',
  SCHEDULED: 'warning',
  IN_PROGRESS: 'processing',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

export const PLAN_STATUS_ORDER: PlanStatus[] = [
  'DESIGN',
  'SCHEDULED',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
];
