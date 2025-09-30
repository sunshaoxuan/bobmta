import type { PlanStatus } from '../api/types';
import type { UiMessageKey } from '../i18n/localization';

export type PlanViewMode = 'design' | 'execution';

export const PLAN_STATUS_MODE: Record<PlanStatus, PlanViewMode> = {
  DESIGN: 'design',
  SCHEDULED: 'execution',
  IN_PROGRESS: 'execution',
  COMPLETED: 'execution',
  CANCELLED: 'execution',
};

export const PLAN_MODE_LABEL: Record<PlanViewMode, UiMessageKey> = {
  design: 'planDetailModeDesign',
  execution: 'planDetailModeExecution',
};
