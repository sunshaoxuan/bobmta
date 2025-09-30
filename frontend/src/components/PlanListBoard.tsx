import React, { useMemo } from '../../vendor/react/index.js';
import {
  Button,
  Card,
  Empty,
  Pagination,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from '../../vendor/antd/index.js';
import type { PlanSummary } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import type { Locale } from '../i18n/localization';
import type { PlanListFilters, PlanListState } from '../state/planList';
import type {
  PlanDetailState,
  PlanNodeActionInput,
  PlanReminderUpdateInput,
} from '../state/planDetail';
import { PlanFilters } from './PlanFilters';
import { RemoteState } from './RemoteState';
import { PlanPreview } from './PlanPreview';
import { extractPlanOwners } from '../utils/planList';

const { Text } = Typography;

type PlanListBoardProps = {
  translate: LocalizationState['translate'];
  locale: Locale;
  sessionActive: boolean;
  planState: PlanListState;
  planColumns: TableColumnsType<PlanSummary>;
  planErrorDetail: string | null;
  lastUpdatedLabel: string | null;
  onRefreshList: () => void;
  isRefreshDisabled: boolean;
  onApplyFilters: (filters: Partial<PlanListFilters>) => void;
  onResetFilters: () => void;
  onChangePage: (page: number) => void;
  onChangePageSize: (pageSize: number) => void;
  selectedPlanId: string | null;
  previewPlan: PlanSummary | null;
  planDetailState: PlanDetailState;
  planDetailErrorDetail: string | null;
  onRefreshDetail: () => void;
  onClosePreview: () => void;
  onSelectPlan: (planId: string) => void;
  onExecuteNodeAction: (input: PlanNodeActionInput) => Promise<void>;
  onUpdateReminder: (input: PlanReminderUpdateInput) => Promise<void>;
  currentUserName: string | null;
  onTimelineCategoryChange: (category: string | null) => void;
};

export function PlanListBoard({
  translate,
  locale,
  sessionActive,
  planState,
  planColumns,
  planErrorDetail,
  lastUpdatedLabel,
  onRefreshList,
  isRefreshDisabled,
  onApplyFilters,
  onResetFilters,
  onChangePage,
  onChangePageSize,
  selectedPlanId,
  previewPlan,
  planDetailState,
  planDetailErrorDetail,
  onRefreshDetail,
  onClosePreview,
  onSelectPlan,
  onExecuteNodeAction,
  onUpdateReminder,
  currentUserName,
  onTimelineCategoryChange,
}: PlanListBoardProps) {
  const availableOwners = useMemo(
    () => extractPlanOwners(planState.records, locale),
    [planState.records, locale]
  );
  const errorDetailMessage = planErrorDetail
    ? translate('planError', { error: planErrorDetail })
    : null;
  const emptyHint = useMemo(
    () => <Text type="secondary">{translate('planEmptyFiltered')}</Text>,
    [translate]
  );

  const paginationLabel = useMemo(() => {
    if (planState.pagination.total <= 0) {
      return null;
    }
    return translate('planPaginationTotal', { total: planState.pagination.total });
  }, [planState.pagination.total, translate]);

  return (
    <Card
      title={translate('planSectionTitle')}
      bordered={false}
      className="card-block"
      extra={
        <Space size="middle" className="plan-card-extra">
          {planState.origin === 'cache' && (
            <Tag color="gold" className="cache-indicator">
              {translate('planCacheHit')}
            </Tag>
          )}
          {lastUpdatedLabel && (
            <Text type="secondary" className="plan-last-updated">
              {lastUpdatedLabel}
            </Text>
          )}
          <Button
            type="link"
            onClick={() => {
              onRefreshList();
            }}
            disabled={isRefreshDisabled}
            loading={planState.status === 'loading'}
          >
            {translate('planRefresh')}
          </Button>
        </Space>
      }
    >
      {!sessionActive && <Empty description={translate('planLoginRequired')} />}
      {sessionActive && (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <PlanFilters
            filters={planState.filters}
            translate={translate}
            owners={availableOwners}
            onApply={(filters) => {
              onApplyFilters(filters);
            }}
            onReset={() => {
              onResetFilters();
            }}
          />
          <RemoteState
            status={planState.status}
            error={planState.error}
            translate={translate}
            empty={planState.records.length === 0}
            onRetry={onRefreshList}
            errorDetail={errorDetailMessage}
            emptyHint={emptyHint}
          >
            <Table<PlanSummary>
              rowKey="id"
              dataSource={planState.records}
              columns={planColumns}
              pagination={false}
              rowClassName={(record: PlanSummary) =>
                ['plan-table-row', selectedPlanId === record.id ? 'plan-table-row-active' : '']
                  .filter(Boolean)
                  .join(' ')
              }
              onRow={(record: PlanSummary) => ({
                onClick: () => {
                  onSelectPlan(record.id);
                },
              })}
              loading={{
                spinning: planState.status === 'loading',
                tip: translate('planLoading'),
              }}
              locale={{ emptyText: translate('planEmpty') }}
              scroll={{ x: true }}
            />
            {planState.pagination.total > 0 && (
              <div className="plan-pagination">
                {paginationLabel && <Text type="secondary">{paginationLabel}</Text>}
                <Pagination
                  current={planState.pagination.page + 1}
                  pageSize={planState.pagination.pageSize}
                  total={planState.pagination.total}
                  showSizeChanger
                  pageSizeOptions={['10', '20', '50']}
                  onChange={(page) => {
                    onChangePage(page - 1);
                  }}
                  onShowSizeChange={(_, size) => {
                    onChangePageSize(size);
                  }}
                />
              </div>
            )}
            {planState.records.length > 0 && (
              <PlanPreview
                plan={previewPlan}
                translate={translate}
                locale={locale}
                onClose={onClosePreview}
                detailState={planDetailState}
                onRefreshDetail={onRefreshDetail}
                detailErrorDetail={planDetailErrorDetail}
                onExecuteNodeAction={onExecuteNodeAction}
                onUpdateReminder={onUpdateReminder}
                currentUserName={currentUserName}
                onTimelineCategoryChange={onTimelineCategoryChange}
              />
            )}
          </RemoteState>
        </Space>
      )}
    </Card>
  );
}
