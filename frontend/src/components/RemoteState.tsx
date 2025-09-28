import type { ReactNode } from '../../vendor/react/index.js';
import React, { useMemo } from '../../vendor/react/index.js';
import { Alert, Button, Empty, Space } from '../../vendor/antd/index.js';
import type { ApiError } from '../api/client';
import type { LocalizationState } from '../i18n/useLocalization';

export type RemoteStateProps = {
  status: 'idle' | 'loading' | 'success';
  error: ApiError | null;
  translate: LocalizationState['translate'];
  empty: boolean;
  onRetry?: () => void;
  children: ReactNode;
  emptyHint?: ReactNode;
  errorDetail?: string | null;
};

export function RemoteState({
  status,
  error,
  translate,
  empty,
  onRetry,
  children,
  emptyHint,
  errorDetail,
}: RemoteStateProps) {
  const retryLabel = useMemo(() => translate('commonStateRetry'), [translate]);

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
          description={
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <span>{translate('commonStateErrorDescription')}</span>
              {errorDetail ? (
                <span className="state-error-detail">{errorDetail}</span>
              ) : null}
            </Space>
          }
        />
        {onRetry && (
          <div>
            <Button type="primary" size="small" onClick={onRetry}>
              {retryLabel}
            </Button>
          </div>
        )}
      </Space>
    );
  }

  if (empty) {
    return (
      <Empty
        description={translate('commonStateEmptyDescription')}
        imageStyle={{ height: 80 }}
      >
        {emptyHint ?? null}
      </Empty>
    );
  }

  return <>{children}</>;
}
