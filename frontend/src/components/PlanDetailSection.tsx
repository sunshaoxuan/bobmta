import React, { type ReactNode } from '../../vendor/react/index.js';
import { Alert, Button, Space, Typography } from '../../vendor/antd/index.js';
import type { ApiError } from '../api/client';
import type { LocalizationState } from '../i18n/useLocalization';

const { Text } = Typography;

type PlanDetailSectionProps = {
  title: string;
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  translate: LocalizationState['translate'];
  empty: boolean;
  onRetry: () => void;
  children: ReactNode;
  errorDetail: string | null;
  emptyMessage: string;
  helper?: ReactNode;
  actions?: ReactNode;
  className?: string;
};

export function PlanDetailSection({
  title,
  status,
  error,
  translate,
  empty,
  onRetry,
  children,
  errorDetail,
  emptyMessage,
  helper,
  actions,
  className,
}: PlanDetailSectionProps) {
  return (
    <section className={['plan-preview-section', className].filter(Boolean).join(' ')}>
      <div className="plan-preview-section-header">
        <Text strong>{title}</Text>
        {actions ? <div className="plan-preview-section-actions">{actions}</div> : null}
      </div>
      {helper ? <div className="plan-preview-section-helper">{helper}</div> : null}
      {renderContent({ status, error, translate, empty, onRetry, children, errorDetail, emptyMessage })}
    </section>
  );
}

type RenderContentOptions = {
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  translate: LocalizationState['translate'];
  empty: boolean;
  onRetry: () => void;
  children: ReactNode;
  errorDetail: string | null;
  emptyMessage: string;
};

function renderContent({
  status,
  error,
  translate,
  empty,
  onRetry,
  children,
  errorDetail,
  emptyMessage,
}: RenderContentOptions): ReactNode {
  if (status === 'loading') {
    return (
      <Alert
        type="info"
        showIcon
        message={translate('commonStateLoadingTitle')}
        description={translate('commonStateLoadingDescription')}
      />
    );
  }

  if (error) {
    return (
      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        <Alert
          type="error"
          showIcon
          message={translate('commonStateErrorTitle')}
          description={errorDetail ?? translate('commonStateErrorDescription')}
        />
        <div>
          <Button type="primary" size="small" onClick={onRetry}>
            {translate('commonStateRetry')}
          </Button>
        </div>
      </Space>
    );
  }

  if (empty) {
    return <Alert type="warning" showIcon message={emptyMessage} />;
  }

  return <>{children}</>;
}
