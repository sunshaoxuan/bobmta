import type {
  PlanDetail,
  PlanDetailPayload,
  PlanNode,
  PlanReminderSummary,
  PlanTimelineEntry,
} from '../api/types';

const DETAIL_CACHE: Record<string, PlanDetailPayload> = {
  'PLN-2025-001': createPlanDetail(
    {
      id: 'PLN-2025-001',
      title: 'データセンター電源点検',
      owner: 'Tanaka',
      status: 'IN_PROGRESS',
      description: '主要ラックの計画停電に合わせた電源冗長性点検を実施します。',
      customer: { id: 'CUST-101', name: '北見工業大学' },
      plannedStartTime: '2025-10-01T01:00:00+09:00',
      plannedEndTime: '2025-10-01T05:00:00+09:00',
      actualStartTime: '2025-10-01T01:05:00+09:00',
      actualEndTime: null,
      tags: ['データセンター', '定期点検'],
      participants: [
        { id: 'USR-001', name: 'Tanaka', role: 'Owner' },
        { id: 'USR-002', name: 'Suzuki', role: 'Reviewer' },
        { id: 'USR-005', name: 'Li', role: 'Technician' },
      ],
      progress: 45,
      nodes: [
        createNode({
          id: 'NODE-001-1',
          name: '事前周知メール送信',
          order: 1,
          status: 'DONE',
          actionType: 'EMAIL',
          actionRef: { id: 'TPL-EMAIL-21', name: '停電通知テンプレート', type: 'EMAIL' },
          assignee: { id: 'USR-002', name: 'Suzuki', role: 'Reviewer' },
          expectedDurationMinutes: 10,
          actualStartTime: '2025-09-30T18:00:00+09:00',
          actualEndTime: '2025-09-30T18:08:00+09:00',
          resultSummary: '主要顧客へ完了報告済み',
        }),
        createNode({
          id: 'NODE-001-2',
          name: 'UPS 冗長構成確認',
          order: 2,
          status: 'IN_PROGRESS',
          actionType: 'MANUAL',
          assignee: { id: 'USR-005', name: 'Li', role: 'Technician' },
          expectedDurationMinutes: 90,
          actualStartTime: '2025-10-01T01:05:00+09:00',
          resultSummary: '系統 A 点検完了、系統 B 実施中',
          children: [
            createNode({
              id: 'NODE-001-2A',
              name: '系統 A フェイルオーバーテスト',
              order: 1,
              status: 'DONE',
              actionType: 'MANUAL',
              expectedDurationMinutes: 30,
              actualStartTime: '2025-10-01T01:05:00+09:00',
              actualEndTime: '2025-10-01T01:33:00+09:00',
              resultSummary: '問題なし',
            }),
            createNode({
              id: 'NODE-001-2B',
              name: '系統 B 電圧監視',
              order: 2,
              status: 'IN_PROGRESS',
              actionType: 'MANUAL',
              expectedDurationMinutes: 45,
            }),
          ],
        }),
        createNode({
          id: 'NODE-001-3',
          name: '結果レポート共有',
          order: 3,
          status: 'PENDING',
          actionType: 'EMAIL',
          actionRef: { id: 'TPL-EMAIL-45', name: '点検レポートテンプレート', type: 'EMAIL' },
          assignee: { id: 'USR-001', name: 'Tanaka', role: 'Owner' },
          expectedDurationMinutes: 20,
        }),
      ],
    },
    [
      createTimeline({
        id: 'TL-001',
        occurredAt: '2025-09-28T10:12:00+09:00',
        message: 'プランを作成しました。',
        actor: { id: 'USR-001', name: 'Tanaka' },
        category: 'PLAN_CREATED',
      }),
      createTimeline({
        id: 'TL-002',
        occurredAt: '2025-09-30T18:08:15+09:00',
        message: '「事前周知メール送信」を完了しました。',
        actor: { id: 'USR-002', name: 'Suzuki' },
        category: 'NODE_COMPLETED',
      }),
      createTimeline({
        id: 'TL-003',
        occurredAt: '2025-10-01T01:05:00+09:00',
        message: '計画を開始しました。',
        actor: { id: 'USR-001', name: 'Tanaka' },
        category: 'PLAN_STARTED',
      }),
    ],
    [
      createReminder({
        id: 'RM-001',
        channel: 'EMAIL',
        offsetMinutes: -60,
        active: true,
        description: '開始 60 分前に所有者へ通知',
      }),
      createReminder({
        id: 'RM-002',
        channel: 'IM',
        offsetMinutes: 30,
        active: true,
        description: '実行中に Teams チャネルへ進捗投稿',
      }),
    ]
  ),
  'PLN-2025-002': createPlanDetail(
    {
      id: 'PLN-2025-002',
      title: 'クラウド基盤障害訓練',
      owner: 'Suzuki',
      status: 'SCHEDULED',
      description: '年次ディザスタリカバリ訓練。仮想マシン停止と復旧手順を検証します。',
      customer: { id: 'CUST-205', name: '北海データリンク' },
      plannedStartTime: '2025-10-03T09:00:00+09:00',
      plannedEndTime: '2025-10-03T12:00:00+09:00',
      actualStartTime: null,
      actualEndTime: null,
      tags: ['演習', 'DR'],
      participants: [
        { id: 'USR-002', name: 'Suzuki', role: 'Owner' },
        { id: 'USR-003', name: 'Wang', role: 'Operator' },
      ],
      progress: 0,
      nodes: [
        createNode({
          id: 'NODE-002-1',
          name: '訓練計画レビュー',
          order: 1,
          status: 'PENDING',
          actionType: 'MEETING',
          expectedDurationMinutes: 45,
        }),
        createNode({
          id: 'NODE-002-2',
          name: '障害シナリオ投入',
          order: 2,
          status: 'PENDING',
          actionType: 'MANUAL',
          expectedDurationMinutes: 30,
        }),
        createNode({
          id: 'NODE-002-3',
          name: '復旧プロセス検証',
          order: 3,
          status: 'PENDING',
          actionType: 'SCRIPT',
          expectedDurationMinutes: 60,
        }),
      ],
    },
    [
      createTimeline({
        id: 'TL-010',
        occurredAt: '2025-09-25T09:30:00+09:00',
        message: '訓練の実施日を決定しました。',
        actor: { id: 'USR-002', name: 'Suzuki' },
        category: 'PLAN_UPDATED',
      }),
    ],
    [
      createReminder({
        id: 'RM-010',
        channel: 'EMAIL',
        offsetMinutes: -1440,
        active: true,
        description: '開始 1 日前に参加者へ通知',
      }),
    ]
  ),
  'PLN-2025-003': createPlanDetail(
    {
      id: 'PLN-2025-003',
      title: 'ネットワーク冗長化リリース',
      owner: 'Wang',
      status: 'DESIGN',
      description: 'バックボーン増強のためのルータ構成変更を段階的に反映します。',
      customer: { id: 'CUST-318', name: '札幌メディカル' },
      plannedStartTime: '2025-10-10T20:00:00+09:00',
      plannedEndTime: '2025-10-11T02:00:00+09:00',
      actualStartTime: null,
      actualEndTime: null,
      tags: ['ネットワーク', 'リリース'],
      participants: [
        { id: 'USR-003', name: 'Wang', role: 'Owner' },
        { id: 'USR-006', name: 'Zhang', role: 'Reviewer' },
      ],
      progress: 5,
      nodes: [
        createNode({
          id: 'NODE-003-1',
          name: '構成変更案レビュー',
          order: 1,
          status: 'DONE',
          actionType: 'MEETING',
          actualStartTime: '2025-09-29T15:00:00+09:00',
          actualEndTime: '2025-09-29T16:00:00+09:00',
          resultSummary: 'レビュー完了、軽微な修正指摘あり',
        }),
        createNode({
          id: 'NODE-003-2',
          name: '切替手順ドライラン',
          order: 2,
          status: 'PENDING',
          actionType: 'SCRIPT',
          expectedDurationMinutes: 90,
        }),
        createNode({
          id: 'NODE-003-3',
          name: '本番切替',
          order: 3,
          status: 'PENDING',
          actionType: 'REMOTE',
          expectedDurationMinutes: 120,
        }),
      ],
    },
    [
      createTimeline({
        id: 'TL-020',
        occurredAt: '2025-09-27T11:00:00+09:00',
        message: 'レビュー会議を招集しました。',
        actor: { id: 'USR-003', name: 'Wang' },
        category: 'PLAN_CREATED',
      }),
    ],
    [
      createReminder({
        id: 'RM-020',
        channel: 'IM',
        offsetMinutes: -120,
        active: false,
        description: '切替開始 2 時間前にチームへ通知（無効化）',
      }),
    ]
  ),
};

function createPlanDetail(
  detail: PlanDetail,
  timeline: PlanTimelineEntry[],
  reminders: PlanReminderSummary[]
): PlanDetailPayload {
  return {
    detail,
    timeline: [...timeline].sort((a, b) => a.occurredAt.localeCompare(b.occurredAt)),
    reminders,
  };
}

function createNode(node: PlanNode): PlanNode {
  return {
    ...node,
    children: (node.children ?? []).map((child) => createNode(child)),
  };
}

function createTimeline(entry: PlanTimelineEntry): PlanTimelineEntry {
  return { ...entry };
}

function createReminder(reminder: PlanReminderSummary): PlanReminderSummary {
  return { ...reminder };
}

export function getMockPlanDetail(planId: string): PlanDetailPayload | null {
  return DETAIL_CACHE[planId] ?? null;
}
