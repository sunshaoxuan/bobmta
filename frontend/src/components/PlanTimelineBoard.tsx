import React, { useMemo, type ReactNode } from '../../vendor/react/index.js';
import { Alert, Button, Select, Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { ApiError } from '../api/client';
import type { PlanTimelineEntry } from '../api/types';
import type { Locale } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PlanDetailSection } from './PlanDetailSection';
import { formatDateTime } from '../utils/planFormatting';
import {
  deriveTimelineFilter,
  isTimelineHighlightVisible,
  type TimelineFilterResult,
} from '../utils/planTimeline.js';

const { Text } = Typography;

type PlanTimelineBoardProps = {
  entries: readonly PlanTimelineEntry[];
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  translate: LocalizationState['translate'];
  locale: Locale;
  activeCategory: string | null;
  highlightedEntryId: string | null;
  onCategoryChange: (category: string | null) => void;
  onRetry: () => void;
  errorDetail: string | null;
};

export function PlanTimelineBoard({
  entries,
  status,
  error,
  translate,
  locale,
  activeCategory,
  highlightedEntryId,
  onCategoryChange,
  onRetry,
  errorDetail,
}: PlanTimelineBoardProps) {
  const filterState = useMemo<TimelineFilterResult>(
    () => deriveTimelineFilter(entries, activeCategory),
    [entries, activeCategory]
  );

  const isHighlightVisible = useMemo(
    () =>
      isTimelineHighlightVisible(
        entries,
        filterState.activeCategories,
        highlightedEntryId
      ),
    [entries, filterState.activeCategories, highlightedEntryId]
  );

  const helper = useMemo(() => {
    const helperMessages: ReactNode[] = [];
    if (filterState.isFilterActive && filterState.isFilteredEmpty) {
      helperMessages.push(
        <Alert
          key="filter-empty"
          type="warning"
          showIcon
          message={translate('planDetailTimelineFilterNoMatch')}
        />
      );
    }
    if (highlightedEntryId && !isHighlightVisible) {
      helperMessages.push(
        <Alert
          key="highlight-hidden"
          type="info"
          showIcon
          message={translate('planDetailTimelineHighlightHidden')}
        />
      );
    }
    if (helperMessages.length === 0) {
      return null;
    }
    return (
      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        {helperMessages}
      </Space>
    );
  }, [
    filterState.isFilterActive,
    filterState.isFilteredEmpty,
    highlightedEntryId,
    isHighlightVisible,
    translate,
  ]);

  const actions = useMemo(() => {
    if (filterState.categories.length === 0) {
      return null;
    }
    return (
      <Space size="small" align="center">
        <Text type="secondary">{translate('planDetailTimelineFilterLabel')}</Text>
        <Select
          size="small"
          className="plan-preview-timeline-filter"
          value={activeCategory ?? '__ALL__'}
          onChange={(value: string) => {
            onCategoryChange(value === '__ALL__' ? null : value);
          }}
          options={[
            {
              value: '__ALL__',
              label: translate('planDetailTimelineFilterAll'),
            },
            ...filterState.categories.map((category) => ({
              value: category,
              label: translate('planDetailTimelineFilterOption', { category }),
            })),
          ]}
        />
        {activeCategory !== null ? (
          <Button
            type="link"
            size="small"
            onClick={() => {
              onCategoryChange(null);
            }}
          >
            {translate('planDetailTimelineFilterReset')}
          </Button>
        ) : null}
      </Space>
    );
  }, [filterState.categories, activeCategory, onCategoryChange, translate]);

  return (
    <PlanDetailSection
      title={translate('planDetailTimelineTitle')}
      status={status}
      error={error}
      translate={translate}
      empty={entries.length === 0}
      onRetry={onRetry}
      errorDetail={errorDetail}
      emptyMessage={translate('planDetailTimelineEmpty')}
      actions={actions}
      helper={helper}
    >
      {filterState.filteredEntries.length === 0 ? (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailTimelineFilterEmpty')}
        />
      ) : (
        <ul className="plan-preview-timeline">
          {filterState.filteredEntries.map((entry) => (
            <li
              key={entry.id}
              className={[
                'plan-preview-timeline-item',
                highlightedEntryId === entry.id
                  ? 'plan-preview-timeline-item-highlight'
                  : '',
              ]
                .filter(Boolean)
                .join(' ')}
            >
              <div className="plan-preview-timeline-time">
                {formatDateTime(entry.occurredAt, locale) ?? entry.occurredAt}
              </div>
              <div className="plan-preview-timeline-body">
                <Text>{entry.message}</Text>
                {entry.actor ? (
                  <Tag color="cyan" className="plan-preview-timeline-actor">
                    {entry.actor.name}
                  </Tag>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </PlanDetailSection>
  );
}
