import type { PageResponse, PlanStatus, PlanSummary } from '../api/types';

export type MockPlanQuery = {
  owner?: string;
  keyword?: string;
  status?: PlanStatus | string;
  page?: number;
  size?: number;
  from?: string;
  to?: string;
};

const MOCK_PLANS: PlanSummary[] = [
  {
    id: 'PLN-2025-001',
    title: 'データセンター電源点検',
    owner: 'Tanaka',
    status: 'IN_PROGRESS',
    plannedStartTime: '2025-10-01T01:00:00+09:00',
    plannedEndTime: '2025-10-01T05:00:00+09:00',
    participants: ['Tanaka', 'Suzuki', 'Li'],
    progress: 45,
    customer: { id: 'CUST-101', name: '北見工業大学' },
    customerId: 'CUST-101',
    customerName: '北見工業大学',
  },
  {
    id: 'PLN-2025-002',
    title: 'クラウド基盤障害訓練',
    owner: 'Suzuki',
    status: 'SCHEDULED',
    plannedStartTime: '2025-10-03T09:00:00+09:00',
    plannedEndTime: '2025-10-03T12:00:00+09:00',
    participants: ['Suzuki', 'Wang'],
    progress: 0,
    customer: { id: 'CUST-205', name: '北海データリンク' },
    customerId: 'CUST-205',
    customerName: '北海データリンク',
  },
  {
    id: 'PLN-2025-003',
    title: 'ネットワーク冗長化リリース',
    owner: 'Wang',
    status: 'DESIGN',
    plannedStartTime: '2025-10-10T20:00:00+09:00',
    plannedEndTime: '2025-10-11T02:00:00+09:00',
    participants: ['Wang', 'Zhang'],
    progress: 5,
    customer: { id: 'CUST-318', name: '札幌メディカル' },
    customerId: 'CUST-318',
    customerName: '札幌メディカル',
  },
  {
    id: 'PLN-2025-004',
    title: 'バックアップ復旧訓練',
    owner: 'Li',
    status: 'COMPLETED',
    plannedStartTime: '2025-09-15T09:00:00+09:00',
    plannedEndTime: '2025-09-15T11:30:00+09:00',
    participants: ['Li', 'Tanaka'],
    progress: 100,
    customer: { id: 'CUST-410', name: '函館通信' },
    customerId: 'CUST-410',
    customerName: '函館通信',
  },
  {
    id: 'PLN-2025-005',
    title: 'セキュリティパッチ一斉適用',
    owner: 'Chen',
    status: 'IN_PROGRESS',
    plannedStartTime: '2025-09-30T22:00:00+09:00',
    plannedEndTime: '2025-10-01T02:00:00+09:00',
    participants: ['Chen', 'Suzuki', 'Abe'],
    progress: 60,
    customer: { id: 'CUST-512', name: '帯広物流' },
    customerId: 'CUST-512',
    customerName: '帯広物流',
  },
  {
    id: 'PLN-2025-006',
    title: '運用ポータルリリース調整',
    owner: 'Abe',
    status: 'CANCELLED',
    plannedStartTime: '2025-09-20T10:00:00+09:00',
    plannedEndTime: '2025-09-20T17:00:00+09:00',
    participants: ['Abe', 'Tanaka'],
    progress: 10,
    customer: { id: 'CUST-777', name: '北方システム' },
    customerId: 'CUST-777',
    customerName: '北方システム',
  },
];

export function listMockPlans(): readonly PlanSummary[] {
  return MOCK_PLANS;
}

export function queryMockPlanSummaries(query: MockPlanQuery = {}): PageResponse<PlanSummary> {
  const page = Math.max(0, query.page ?? 0);
  const size = Math.max(1, query.size ?? 20);
  const owner = normalize(query.owner);
  const keyword = normalize(query.keyword);
  const status = normalize(query.status);
  const from = toEpoch(query.from);
  const to = toEpoch(query.to);

  const filtered = MOCK_PLANS.filter((plan) => {
    if (owner && normalize(plan.owner) !== owner) {
      return false;
    }
    if (status && normalize(plan.status) !== status) {
      return false;
    }
    if (keyword) {
      const tokens = [plan.id, plan.title, plan.owner, describeStatus(plan.status)];
      if (!tokens.some((token) => normalize(token).includes(keyword))) {
        return false;
      }
    }
    const planStart = toEpoch(plan.plannedStartTime);
    const planEnd = toEpoch(plan.plannedEndTime);
    if (from && planEnd && planEnd < from) {
      return false;
    }
    if (to && planStart && planStart > to) {
      return false;
    }
    return true;
  });

  const start = page * size;
  const end = start + size;
  const pageItems = filtered.slice(start, end);

  return {
    list: pageItems,
    total: filtered.length,
    page,
    pageSize: size,
  };
}

function normalize(value?: string | null): string {
  return (value ?? '').trim().toLowerCase();
}

function toEpoch(value?: string | null): number | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.getTime();
}

function describeStatus(status: PlanStatus): string {
  switch (status) {
    case 'DESIGN':
      return 'design';
    case 'SCHEDULED':
      return 'scheduled';
    case 'IN_PROGRESS':
      return 'in-progress';
    case 'COMPLETED':
      return 'completed';
    case 'CANCELLED':
      return 'cancelled';
    default:
      return status;
  }
}
