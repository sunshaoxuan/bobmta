import type { PlanSummary } from '../api/types';
import type { Locale } from '../i18n/localization';

export function extractPlanOwners(records: readonly PlanSummary[], locale: Locale | null): string[] {
  const owners = new Set<string>();
  for (const record of records) {
    const owner = typeof record.owner === 'string' ? record.owner.trim() : '';
    if (owner.length > 0) {
      owners.add(owner);
    }
  }
  const list = Array.from(owners);
  if (list.length <= 1) {
    return list;
  }
  const localeTag = locale ?? 'ja-JP';
  return list.sort((a, b) => a.localeCompare(b, localeTag, { sensitivity: 'base' }));
}
