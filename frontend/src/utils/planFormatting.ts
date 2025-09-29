import type { PlanSummary } from '../api/types';
import type { Locale } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';

export function formatDateTime(value?: string | null, locale?: Locale): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return new Intl.DateTimeFormat(locale ?? 'ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatPlanWindow(
  plan: Pick<PlanSummary, 'plannedStartTime' | 'plannedEndTime'>,
  locale: Locale,
  translate: LocalizationState['translate']
): string {
  const start = formatDateTime(plan.plannedStartTime ?? null, locale);
  const end = formatDateTime(plan.plannedEndTime ?? null, locale);
  if (start && end) {
    return translate('planWindowRange', { start, end });
  }
  if (start) {
    return start;
  }
  if (end) {
    return end;
  }
  return translate('planWindowMissing');
}
