import type {
  PlanDetail,
  PlanDetailPayload,
  PlanNode,
  PlanNodeStatus,
  PlanParticipant,
  PlanReminderSummary,
  PlanTimelineEntry,
} from '../api/types';

const FALLBACK_NODE_STATUS: PlanNodeStatus = 'PENDING';

export function normalizePlanDetailPayload(payload: PlanDetailPayload): PlanDetailPayload {
  const detail = normalizePlanDetail(payload.detail);
  const timeline = normalizeTimeline(payload.timeline);
  const reminders = normalizeReminders(payload.reminders);
  return {
    detail,
    timeline,
    reminders,
  };
}

function normalizePlanDetail(detail: PlanDetail): PlanDetail {
  return {
    ...detail,
    description: typeof detail.description === 'string' ? detail.description : null,
    customer: normalizeCustomer(detail.customer),
    tags: normalizeTags(detail.tags),
    participants: normalizeParticipants(detail.participants),
    nodes: normalizeNodes(detail.nodes),
    plannedStartTime: sanitizeDate(detail.plannedStartTime),
    plannedEndTime: sanitizeDate(detail.plannedEndTime),
    actualStartTime: sanitizeDate(detail.actualStartTime),
    actualEndTime: sanitizeDate(detail.actualEndTime),
    progress: sanitizeProgress(detail.progress),
  };
}

function normalizeCustomer(
  customer: PlanDetail['customer']
): PlanDetail['customer'] {
  if (!customer || typeof customer.id !== 'string' || typeof customer.name !== 'string') {
    return null;
  }
  return {
    id: customer.id,
    name: customer.name,
  };
}

function normalizeTags(tags: PlanDetail['tags']): string[] {
  if (!Array.isArray(tags)) {
    return [];
  }
  const seen = new Set<string>();
  const normalized: string[] = [];
  for (const tag of tags) {
    if (typeof tag !== 'string') {
      continue;
    }
    const trimmed = tag.trim();
    if (!trimmed || seen.has(trimmed)) {
      continue;
    }
    seen.add(trimmed);
    normalized.push(trimmed);
  }
  return normalized;
}

function normalizeParticipants(participants: PlanParticipant[]): PlanParticipant[] {
  if (!Array.isArray(participants)) {
    return [];
  }
  return participants
    .filter(
      (participant): participant is PlanParticipant =>
        typeof participant?.id === 'string' && typeof participant?.name === 'string'
    )
    .map((participant) => ({
      id: participant.id,
      name: participant.name,
      role: typeof participant.role === 'string' ? participant.role : null,
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function normalizeNodes(nodes: PlanNode[] | undefined | null): PlanNode[] {
  if (!Array.isArray(nodes) || nodes.length === 0) {
    return [];
  }
  const normalized = nodes
    .filter((node): node is PlanNode => typeof node?.id === 'string' && typeof node?.name === 'string')
    .map((node) => normalizeNode(node));
  normalized.sort((a, b) => {
    const orderDiff = (a.order ?? 0) - (b.order ?? 0);
    if (orderDiff !== 0) {
      return orderDiff;
    }
    return a.name.localeCompare(b.name);
  });
  return normalized;
}

function normalizeNode(node: PlanNode): PlanNode {
  const children = normalizeNodes(node.children);
  const actionRef =
    node.actionRef &&
    typeof node.actionRef.id === 'string' &&
    typeof node.actionRef.name === 'string' &&
    typeof node.actionRef.type === 'string'
      ? { ...node.actionRef }
      : null;
  const assignee =
    node.assignee &&
    typeof node.assignee.id === 'string' &&
    typeof node.assignee.name === 'string'
      ? {
          id: node.assignee.id,
          name: node.assignee.name,
          role: typeof node.assignee.role === 'string' ? node.assignee.role : null,
        }
      : null;
  const status = isValidNodeStatus(node.status) ? node.status : FALLBACK_NODE_STATUS;
  return {
    ...node,
    status,
    actionType: typeof node.actionType === 'string' ? node.actionType : '',
    actionRef,
    assignee,
    expectedDurationMinutes: sanitizeDuration(node.expectedDurationMinutes),
    actualStartTime: sanitizeDate(node.actualStartTime),
    actualEndTime: sanitizeDate(node.actualEndTime),
    resultSummary:
      typeof node.resultSummary === 'string'
        ? node.resultSummary.trim() || null
        : null,
    children,
  };
}

function normalizeTimeline(entries: PlanTimelineEntry[]): PlanTimelineEntry[] {
  if (!Array.isArray(entries)) {
    return [];
  }
  return entries
    .filter(
      (entry): entry is PlanTimelineEntry =>
        typeof entry?.id === 'string' && typeof entry?.occurredAt === 'string'
    )
    .map((entry) => ({
      ...entry,
      message: typeof entry.message === 'string' ? entry.message : '',
      actor:
        entry.actor && typeof entry.actor.id === 'string' && typeof entry.actor.name === 'string'
          ? { id: entry.actor.id, name: entry.actor.name, role: entry.actor.role ?? null }
          : null,
      category: typeof entry.category === 'string' ? entry.category : 'UNKNOWN',
    }))
    .sort((a, b) => parseDate(b.occurredAt) - parseDate(a.occurredAt));
}

function normalizeReminders(reminders: PlanReminderSummary[]): PlanReminderSummary[] {
  if (!Array.isArray(reminders)) {
    return [];
  }
  const seen = new Set<string>();
  const normalized: PlanReminderSummary[] = [];
  for (const reminder of reminders) {
    if (!reminder || typeof reminder.id !== 'string' || typeof reminder.channel !== 'string') {
      continue;
    }
    if (seen.has(reminder.id)) {
      continue;
    }
    seen.add(reminder.id);
    normalized.push({
      id: reminder.id,
      channel: reminder.channel,
      offsetMinutes: sanitizeOffset(reminder.offsetMinutes),
      active: Boolean(reminder.active),
      description: typeof reminder.description === 'string' ? reminder.description : null,
    });
  }
  normalized.sort((a, b) => a.offsetMinutes - b.offsetMinutes);
  return normalized;
}

function sanitizeDate(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null;
  }
  if (Number.isNaN(Date.parse(value))) {
    return null;
  }
  return value;
}

function sanitizeProgress(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return 0;
  }
  if (value < 0) {
    return 0;
  }
  if (value > 100) {
    return 100;
  }
  return value;
}

function sanitizeDuration(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return null;
  }
  if (value < 0) {
    return null;
  }
  return Math.round(value);
}

function sanitizeOffset(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return 0;
  }
  return Math.round(value);
}

function parseDate(value: string): number {
  const parsed = Date.parse(value);
  if (Number.isNaN(parsed)) {
    return 0;
  }
  return parsed;
}

function isValidNodeStatus(status: unknown): status is PlanNodeStatus {
  return status === 'PENDING' ||
    status === 'IN_PROGRESS' ||
    status === 'DONE' ||
    status === 'CANCELLED' ||
    status === 'SKIPPED';
}
