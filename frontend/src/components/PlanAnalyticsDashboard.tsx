import React, { useMemo } from '../../vendor/react/index.js';
import type { FC } from '../../vendor/react/index.js';
import {
  Alert,
  Button,
  Card,
  Progress,
  Space,
  Spin,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type { ApiError } from '../api/client.js';
import type {
  PlanAnalyticsOverview,
  PlanAnalyticsOwnerLoad,
  PlanAnalyticsRiskPlan,
} from '../api/types.js';
import type { Locale } from '../i18n/localization.js';
import { formatDateTime } from '../utils/planFormatting.js';

const { Title, Text } = Typography;

type StatusKey =
  | 'designCount'
  | 'scheduledCount'
  | 'inProgressCount'
  | 'completedCount'
  | 'canceledCount';

type PlanAnalyticsDashboardProps = {
  analytics: PlanAnalyticsOverview | null;
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  source: 'mock' | 'network' | null;
  lastUpdated: string | null;
  locale?: Locale;
  onRetry?: () => void;
};

type StatusBreakdown = {
  key: StatusKey;
  label: string;
  value: number;
  color: string;
  percent: number;
};

type RiskPalette = {
  label: string;
  background: string;
  color: string;
};

const STATUS_TOKENS: Array<{
  key: StatusKey;
  label: string;
  color: string;
}> = [
  { key: 'designCount', label: '设计中', color: '#5a5e66' },
  { key: 'scheduledCount', label: '已排期', color: '#1677ff' },
  { key: 'inProgressCount', label: '执行中', color: '#13c2c2' },
  { key: 'completedCount', label: '已完成', color: '#52c41a' },
  { key: 'canceledCount', label: '已取消', color: '#bfbfbf' },
];

const RISK_PALETTE: Record<PlanAnalyticsRiskPlan['riskLevel'], RiskPalette> = {
  OVERDUE: {
    label: '已逾期',
    background: '#fff1f0',
    color: '#cf1322',
  },
  DUE_SOON: {
    label: '即将到期',
    background: '#fffbe6',
    color: '#d48806',
  },
};

const SOURCE_LABEL: Record<'mock' | 'network', string> = {
  mock: 'Mock 数据',
  network: '实时数据',
};

export function PlanAnalyticsDashboard({
  analytics,
  status,
  error,
  source,
  lastUpdated,
  locale = 'ja-JP',
  onRetry,
}: PlanAnalyticsDashboardProps) {
  const breakdown = useMemo<StatusBreakdown[]>(() => {
    if (!analytics) {
      return [];
    }
    const total = analytics.totalPlans || 0;
    return STATUS_TOKENS.map((token) => {
      const value = analytics[token.key] ?? 0;
      const percent = total > 0 ? Math.round((value / total) * 100) : 0;
      return {
        key: token.key,
        label: token.label,
        value,
        color: token.color,
        percent,
      };
    });
  }, [analytics]);

  const ownerLoads = analytics?.ownerLoads ?? [];
  const riskPlans = analytics?.riskPlans ?? [];
  const upcomingPlans = analytics?.upcomingPlans ?? [];

  const sourceExtra = useMemo(() => {
    if (!source && !lastUpdated) {
      return null;
    }
    return (
      <Space size="small" align="center" className="plan-analytics-meta">
        {source ? (
          <Tag
            className="plan-analytics-source"
            style={{
              backgroundColor:
                source === 'network' ? 'rgba(19, 194, 194, 0.16)' : 'rgba(22, 119, 255, 0.12)',
              color: source === 'network' ? '#08979c' : '#1677ff',
            }}
          >
            {SOURCE_LABEL[source]}
          </Tag>
        ) : null}
        {lastUpdated ? (
          <Text type="secondary" className="plan-analytics-updated">
            更新于 {formatDateTime(lastUpdated, locale)}
          </Text>
        ) : null}
      </Space>
    );
  }, [source, lastUpdated, locale]);

  const empty = !analytics || analytics.totalPlans === 0;

  return (
    <Card
      className="plan-analytics-card"
      bordered={false}
      title={<Title level={4}>计划驾驶舱总览</Title>}
      extra={sourceExtra}
    >
      <div className="plan-analytics-dashboard">
        {status === 'loading' ? (
          <div className="plan-analytics-loading" aria-live="polite">
            <Space size="small" align="center">
              <Spin size="small" />
              <Text type="secondary">正在加载驾驶舱统计...</Text>
            </Space>
          </div>
        ) : null}
        {error ? (
          <Alert
            type="warning"
            showIcon
            message={
              onRetry
                ? '实时接口暂不可用，已回退至 Mock 数据。'
                : '实时接口暂不可用，当前展示 Mock 数据。'
            }
          />
        ) : null}
        {empty ? (
          <Alert type="info" showIcon message="暂无计划统计数据" />
        ) : (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <section className="plan-analytics-section">
              <header className="plan-analytics-section-header">
                <Text className="plan-analytics-section-title">状态分布</Text>
                <Tag className="plan-analytics-total" data-variant="total">
                  总计 {analytics?.totalPlans ?? 0} 个计划
                </Tag>
              </header>
              <div className="plan-analytics-status-grid">
                {breakdown.map((item) => (
                  <div key={item.key as string} className="plan-analytics-status-card">
                    <Text className="plan-analytics-status-label" style={{ color: item.color }}>
                      {item.label}
                    </Text>
                    <Text className="plan-analytics-status-value">{item.value}</Text>
                    <Progress percent={item.percent} size="small" className="plan-analytics-status-progress" />
                  </div>
                ))}
                <div className="plan-analytics-status-card plan-analytics-status-risk">
                  <Text className="plan-analytics-status-label" style={{ color: '#fa541c' }}>
                    风险计划
                  </Text>
                  <Text className="plan-analytics-status-value">{analytics?.overdueCount ?? 0}</Text>
                  <Text className="plan-analytics-status-hint">24 小时内需关注的逾期计划</Text>
                </div>
              </div>
            </section>

            <section className="plan-analytics-section">
              <header className="plan-analytics-section-header">
                <Text className="plan-analytics-section-title">负责人负载</Text>
              </header>
              <div className="plan-analytics-owner-grid">
                {ownerLoads.length === 0 ? (
                  <Text type="secondary">暂无负责人负载统计。</Text>
                ) : (
                  ownerLoads.map((owner) => (
                    <div key={owner.ownerId} className="plan-analytics-owner-card-wrapper">
                      <OwnerLoadCard owner={owner} />
                    </div>
                  ))
                )}
              </div>
            </section>

            <section className="plan-analytics-section">
              <header className="plan-analytics-section-header">
                <Text className="plan-analytics-section-title">风险计划提醒</Text>
                {onRetry ? (
                  <Button type="link" size="small" onClick={onRetry}>
                    重新拉取
                  </Button>
                ) : null}
              </header>
              <div className="plan-analytics-risk-list">
                {riskPlans.length === 0 ? (
                  <Text type="secondary">暂无风险计划。</Text>
                ) : (
                  riskPlans.map((plan) => (
                    <div key={plan.id} className="plan-analytics-risk-item-wrapper">
                      <RiskPlanItem plan={plan} locale={locale} />
                    </div>
                  ))
                )}
              </div>
            </section>

            <section className="plan-analytics-section">
              <header className="plan-analytics-section-header">
                <Text className="plan-analytics-section-title">即将开始</Text>
              </header>
              <div className="plan-analytics-upcoming-list">
                {upcomingPlans.length === 0 ? (
                  <Text type="secondary">未来 24 小时无新计划。</Text>
                ) : (
                  upcomingPlans.map((plan) => (
                    <div key={plan.id} className="plan-analytics-upcoming-item">
                      <div className="plan-analytics-upcoming-title">{plan.title}</div>
                      <div className="plan-analytics-upcoming-meta">
                        <span>负责人：{plan.owner || '未指派'}</span>
                        <span>
                          计划窗口：
                          {formatDateTime(plan.plannedStartTime, locale)} ~
                          {formatDateTime(plan.plannedEndTime, locale)}
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </section>
          </Space>
        )}
      </div>
    </Card>
  );
}

type OwnerLoadCardProps = { owner: PlanAnalyticsOwnerLoad };

const OwnerLoadCard: FC<OwnerLoadCardProps> = ({ owner }) => {
  return (
    <div className="plan-analytics-owner-card">
      <div className="plan-analytics-owner-id">{owner.ownerId}</div>
      <div className="plan-analytics-owner-metric">
        <span>计划总数</span>
        <strong>{owner.totalPlans}</strong>
      </div>
      <div className="plan-analytics-owner-metric">
        <span>活跃计划</span>
        <strong>{owner.activePlans}</strong>
      </div>
      <div className="plan-analytics-owner-metric">
        <span>逾期计划</span>
        <strong>{owner.overduePlans}</strong>
      </div>
    </div>
  );
};

type RiskPlanItemProps = { plan: PlanAnalyticsRiskPlan; locale: Locale };

const RiskPlanItem: FC<RiskPlanItemProps> = ({ plan, locale }) => {
  const palette = RISK_PALETTE[plan.riskLevel];
  const dueInfo = plan.riskLevel === 'OVERDUE'
    ? `已逾期 ${Math.max(0, plan.minutesOverdue)} 分钟`
    : `距到期 ${Math.max(0, plan.minutesUntilDue)} 分钟`;
  return (
    <div className="plan-analytics-risk-item">
      <div className="plan-analytics-risk-header">
        <div className="plan-analytics-risk-title">{plan.title}</div>
        <Tag
          className="plan-analytics-risk-tag"
          style={{ backgroundColor: palette.background, color: palette.color }}
        >
          {palette.label}
        </Tag>
      </div>
      <div className="plan-analytics-risk-meta">
        <span>负责人：{plan.owner || '未指派'}</span>
        <span>到期时间：{formatDateTime(plan.plannedEndTime, locale) || '未设置'}</span>
        <span>{dueInfo}</span>
      </div>
    </div>
  );
};
