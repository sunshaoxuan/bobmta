import type { PlanAnalyticsOverview } from '../api/types.js';

const MOCK_ANALYTICS: PlanAnalyticsOverview = {
  totalPlans: 42,
  designCount: 3,
  scheduledCount: 11,
  inProgressCount: 15,
  completedCount: 10,
  canceledCount: 3,
  overdueCount: 4,
  upcomingPlans: [
    {
      id: 'plan-1001',
      title: '东京数据中心巡检',
      status: 'SCHEDULED',
      plannedStartTime: '2024-04-02T01:00:00+09:00',
      plannedEndTime: '2024-04-02T05:00:00+09:00',
      owner: 'alice',
      customerId: 'cust-01',
      progress: 20,
    },
    {
      id: 'plan-1010',
      title: '上海机房例行保养',
      status: 'SCHEDULED',
      plannedStartTime: '2024-04-02T07:00:00+08:00',
      plannedEndTime: '2024-04-02T09:30:00+08:00',
      owner: 'carol',
      customerId: 'cust-03',
      progress: 0,
    },
  ],
  ownerLoads: [
    {
      ownerId: 'alice',
      totalPlans: 8,
      activePlans: 6,
      overduePlans: 2,
    },
    {
      ownerId: 'bob',
      totalPlans: 7,
      activePlans: 5,
      overduePlans: 1,
    },
    {
      ownerId: 'carol',
      totalPlans: 5,
      activePlans: 3,
      overduePlans: 1,
    },
  ],
  riskPlans: [
    {
      id: 'plan-0995',
      title: '香港IDC 机房复检',
      status: 'IN_PROGRESS',
      plannedEndTime: '2024-04-01T18:00:00+08:00',
      owner: 'bob',
      customerId: 'cust-05',
      riskLevel: 'OVERDUE',
      minutesUntilDue: 0,
      minutesOverdue: 180,
    },
    {
      id: 'plan-1010',
      title: '上海机房例行保养',
      status: 'SCHEDULED',
      plannedEndTime: '2024-04-02T09:30:00+08:00',
      owner: 'carol',
      customerId: 'cust-03',
      riskLevel: 'DUE_SOON',
      minutesUntilDue: 120,
      minutesOverdue: 0,
    },
  ],
};

export function createMockPlanAnalyticsOverview(): PlanAnalyticsOverview {
  return JSON.parse(JSON.stringify(MOCK_ANALYTICS));
}

