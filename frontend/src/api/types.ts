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
};

export type PingResponse = {
  status: string;
};
