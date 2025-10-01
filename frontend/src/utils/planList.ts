import type { PlanSummary } from '../api/types';
import type { Locale } from '../i18n/localization';
import type {
  PlanCalendarEvent,
  PlanSummaryWithCustomer,
} from '../state/planList';

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

export type PlanCalendarEventMap<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer,
> = Record<string, PlanCalendarEvent<T>[]>;

export function mapPlanCalendarEventsByDate<
  T extends PlanSummaryWithCustomer = PlanSummaryWithCustomer,
>(
  events: readonly PlanCalendarEvent<T>[],
  options?: { timeZone?: string }
): PlanCalendarEventMap<T> {
  if (!events || events.length === 0) {
    return {};
  }
  const timeZone = options?.timeZone;
  const map: PlanCalendarEventMap<T> = {};
  events.forEach((event) => {
    const anchor = resolveEventAnchor(event);
    if (!anchor) {
      return;
    }
    const key = formatDateKey(anchor, timeZone);
    if (!key) {
      return;
    }
    if (!map[key]) {
      map[key] = [];
    }
    map[key].push(event);
  });
  Object.keys(map).forEach((key) => {
    map[key].sort((a, b) => {
      const timeA = resolveEventTime(a);
      const timeB = resolveEventTime(b);
      if (timeA === timeB) {
        return a.plan.id.localeCompare(b.plan.id);
      }
      return timeA - timeB;
    });
  });
  return map;
}

function resolveEventAnchor(event: PlanCalendarEvent): string | null {
  if (event.startTime) {
    return event.startTime;
  }
  if (event.endTime) {
    return event.endTime;
  }
  const plan = event.plan as PlanSummaryWithCustomer;
  return plan?.plannedStartTime ?? plan?.plannedEndTime ?? null;
}

function resolveEventTime(event: PlanCalendarEvent): number {
  const anchor = resolveEventAnchor(event);
  if (!anchor) {
    return Number.POSITIVE_INFINITY;
  }
  const time = new Date(anchor).getTime();
  return Number.isFinite(time) ? time : Number.POSITIVE_INFINITY;
}

function formatDateKey(anchor: string, timeZone?: string): string | null {
  const date = new Date(anchor);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  if (!timeZone) {
    return date.toISOString().slice(0, 10);
  }
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  const parts = formatter.formatToParts(date);
  const partMap = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  const year = partMap.year ?? String(date.getUTCFullYear());
  const month = (partMap.month ?? String(date.getUTCMonth() + 1)).padStart(2, '0');
  const day = (partMap.day ?? String(date.getUTCDate())).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
