import React, { useEffect, useMemo, useState } from '../../vendor/react/index.js';
import { Button, DatePicker, Input, Select, Space } from '../../vendor/antd/index.js';
import type { PlanStatus } from '../api/types';
import type { LocalizationState } from '../i18n/useLocalization';
import type { PlanListFilters } from '../state/planList';
import { PLAN_STATUS_LABEL, PLAN_STATUS_ORDER } from '../constants/planStatus';

type PlanFiltersProps = {
  filters: PlanListFilters;
  translate: LocalizationState['translate'];
  owners: string[];
  onApply: (filters: Partial<PlanListFilters>) => void;
  onReset: () => void;
};

export function PlanFilters({ filters, translate, owners, onApply, onReset }: PlanFiltersProps) {
  const [formValues, setFormValues] = useState<PlanListFilters>(filters);

  useEffect(() => {
    setFormValues(filters);
  }, [filters]);

  const ownerOptions = useMemo(
    () => [
      { value: '', label: translate('planFilterOwnerAll') },
      ...owners.map((owner) => ({ value: owner, label: owner })),
    ],
    [owners, translate]
  );

  const statusOptions = useMemo(
    () =>
      [
        { value: '', label: translate('planFilterStatusAll') },
        ...PLAN_STATUS_ORDER.map((status) => ({
          value: status,
          label: translate(PLAN_STATUS_LABEL[status]),
        })),
      ],
    [translate]
  );

  const handleApply = () => {
    onApply(formValues);
  };

  const handleReset = () => {
    setFormValues({ owner: '', keyword: '', status: '', from: '', to: '' });
    onReset();
  };

  return (
    <div className="plan-filters">
      <Space size="large" wrap>
        <div className="filter-field">
          <div className="filter-label">{translate('planFilterOwnerLabel')}</div>
          <Select
            className="filter-select"
            value={formValues.owner}
            options={ownerOptions}
            onChange={(value: string) =>
              setFormValues((current) => ({ ...current, owner: value ?? '' }))
            }
          />
        </div>
        <div className="filter-field">
          <div className="filter-label">{translate('planFilterStatusLabel')}</div>
          <Select
            className="filter-select"
            value={formValues.status}
            options={statusOptions}
            onChange={(value: string) =>
              setFormValues((current) => ({ ...current, status: (value as PlanStatus) ?? '' }))
            }
          />
        </div>
        <div className="filter-field range-field">
          <div className="filter-label">{translate('planFilterWindowLabel')}</div>
          <DatePicker.RangePicker
            value={[formValues.from || null, formValues.to || null]}
            placeholder={[
              translate('planFilterWindowPlaceholderStart'),
              translate('planFilterWindowPlaceholderEnd'),
            ]}
            onChange={(value) => {
              const [from, to] = value ?? [null, null];
              setFormValues((current) => ({
                ...current,
                from: from ?? '',
                to: to ?? '',
              }));
            }}
          />
        </div>
        <div className="filter-field keyword-field">
          <div className="filter-label">{translate('planFilterKeywordLabel')}</div>
          <Input
            value={formValues.keyword}
            placeholder={translate('planFilterKeywordPlaceholder')}
            onChange={(event: Event & { target: HTMLInputElement }) =>
              setFormValues((current) => ({ ...current, keyword: event.target.value }))
            }
            onPressEnter={handleApply}
          />
        </div>
        <Space size="middle" align="end">
          <Button type="primary" onClick={handleApply}>
            {translate('planFilterApply')}
          </Button>
          <Button type="text" onClick={handleReset}>
            {translate('planFilterReset')}
          </Button>
        </Space>
      </Space>
    </div>
  );
}
