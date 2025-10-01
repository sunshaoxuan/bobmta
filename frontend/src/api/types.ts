export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T | null;
};

export type LoginResponse = {
  token: string;
  expiresAt: string;
  displayName: string;
  roles: string[];
};

export type PageResponse<T> = {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
};

export type PlanStatus =
  | 'DESIGN'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type PlanSummary = {
  id: string;
  title: string;
  owner: string;
  status: PlanStatus;
  plannedStartTime?: string | null;
  plannedEndTime?: string | null;
  participants: string[];
  progress: number;
  customer?: {
    id?: string | null;
    name?: string | null;
  } | null;
  customerId?: string | null;
  customerName?: string | null;
};

export type PlanParticipant = {
  id: string;
  name: string;
  role?: string | null;
};

export type PlanNodeStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED' | 'SKIPPED';

export type PlanNode = {
  id: string;
  name: string;
  order: number;
  status: PlanNodeStatus;
  actionType: string;
  actionRef?: {
    id: string;
    name: string;
    type: string;
  } | null;
  assignee?: PlanParticipant | null;
  expectedDurationMinutes?: number | null;
  actualStartTime?: string | null;
  actualEndTime?: string | null;
  resultSummary?: string | null;
  children?: PlanNode[];
};

export type PlanTimelineEntry = {
  id: string;
  occurredAt: string;
  message: string;
  actor?: PlanParticipant | null;
  category: string;
};

export type PlanReminderChannel = 'EMAIL' | 'SMS' | 'IM' | 'WEBHOOK';

export type PlanReminderSummary = {
  id: string;
  channel: PlanReminderChannel;
  offsetMinutes: number;
  active: boolean;
  description?: string | null;
};

export type PlanDetail = {
  id: string;
  title: string;
  owner: string;
  status: PlanStatus;
  description?: string | null;
  customer?: {
    id: string;
    name: string;
  } | null;
  plannedStartTime?: string | null;
  plannedEndTime?: string | null;
  actualStartTime?: string | null;
  actualEndTime?: string | null;
  tags: string[];
  participants: PlanParticipant[];
  progress: number;
  nodes: PlanNode[];
};

export type PlanDetailPayload = {
  detail: PlanDetail;
  timeline: PlanTimelineEntry[];
  reminders: PlanReminderSummary[];
};

export type PingResponse = {
  status: string;
};
