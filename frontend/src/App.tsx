import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from '../vendor/react/index.js';
import './App.css';
import {
  Alert,
  Button,
  Card,
  ConfigProvider,
  Empty,
  Input,
  Layout,
  Pagination,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from '../vendor/antd/index.js';
import type { TableColumnsType } from '../vendor/antd/index.js';
import { createApiClient, type ApiClient, type ApiError } from './api/client';
import { fetchPing } from './api/system';
import type { PlanStatus, PlanSummary } from './api/types';
import {
  useLocalizationState,
  type LocalizationState,
} from './i18n/useLocalization';
import { type Locale } from './i18n/localization';
import {
  usePlanListController,
  type PlanListController,
} from './state/planList';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from './constants/planStatus';
import { RemoteState } from './components/RemoteState';
import { PlanFilters } from './components/PlanFilters';
import { PlanPreview } from './components/PlanPreview';
import {
  useSessionController,
  type SessionController,
} from './state/session';
import { formatDateTime, formatPlanWindow } from './utils/planFormatting';

const { Header, Content } = Layout;
const { Title, Paragraph, Text } = Typography;

type CredentialsState = {
  username: string;
  password: string;
};

type AppViewProps = {
  client: ApiClient;
  localization: LocalizationState;
  session: SessionController;
  planList: PlanListController;
};

function AppView({ client, localization, session, planList }: AppViewProps) {
  const { locale, translate, availableLocales, loading, setLocale } = localization;
  const { state: sessionState, login, logout } = session;
  const { state: planState, refresh, changePage, changePageSize } = planList;
  const [credentials, setCredentials] = useState<CredentialsState>({
    username: '',
    password: '',
  });
  const [pingError, setPingError] = useState<ApiError | null>(null);
  const [ping, setPing] = useState<{ status: string } | null>(null);
  const [previewPlanId, setPreviewPlanId] = useState<string | null>(null);

  const describeRemoteError = useCallback(
    (error: ApiError | null) => {
      if (!error) {
        return null;
      }
      if (error.type === 'status') {
        return translate('backendErrorStatus', {
          status: error.status,
        });
      }
      return translate('backendErrorNetwork');
    },
    [translate]
  );

  useEffect(() => {
    const controller = new AbortController();
    setPing(null);
    setPingError(null);
    fetchPing(client, controller.signal)
      .then((response) => {
        setPing(response);
      })
      .catch((error) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return;
        }
        const apiError: ApiError =
          (error as ApiError)?.type === 'status' || (error as ApiError)?.type === 'network'
            ? (error as ApiError)
            : ({ type: 'network' } as ApiError);
        setPingError(apiError);
      });

    return () => {
      controller.abort();
    };
  }, [client]);

  useEffect(() => {
    if (sessionState.session) {
      setCredentials({ username: '', password: '' });
    }
  }, [sessionState.session]);

  useEffect(() => {
    if (!sessionState.session) {
      setPreviewPlanId(null);
      return;
    }
    if (planState.records.length === 0) {
      setPreviewPlanId(null);
      return;
    }
    setPreviewPlanId((current) => {
      if (current && planState.recordIndex[current]) {
        return current;
      }
      return planState.records[0]?.id ?? null;
    });
  }, [sessionState.session, planState.records, planState.recordIndex]);

  const planColumns = useMemo<TableColumnsType<PlanSummary>>(
    () => [
      {
        title: translate('planTableHeaderId'),
        dataIndex: 'id',
        key: 'id',
        width: 160,
        render: (value: string): ReactNode => <Text code>{value}</Text>,
      },
      {
        title: translate('planTableHeaderTitle'),
        dataIndex: 'title',
        key: 'title',
        ellipsis: true,
        render: (value: string): ReactNode => <Text strong>{value}</Text>,
      },
      {
        title: translate('planTableHeaderOwner'),
        dataIndex: 'owner',
        key: 'owner',
        render: (value: string): ReactNode => <Tag color="geekblue">{value}</Tag>,
      },
      {
        title: translate('planTableHeaderStatus'),
        dataIndex: 'status',
        key: 'status',
        render: (_: PlanStatus, record): ReactNode => (
          <Tag color={PLAN_STATUS_COLOR[record.status]}>
            {translate(PLAN_STATUS_LABEL[record.status])}
          </Tag>
        ),
      },
      {
        title: translate('planTableHeaderWindow'),
        dataIndex: 'plannedStartTime',
        key: 'window',
        render: (_: unknown, record): ReactNode => (
          <Text>{formatPlanWindow(record, locale, translate)}</Text>
        ),
      },
      {
        title: translate('planTableHeaderProgress'),
        dataIndex: 'progress',
        key: 'progress',
        width: 200,
        render: (value: number): ReactNode => (
          <Progress percent={Math.max(0, Math.min(100, Math.round(value ?? 0)))} size="small" />
        ),
      },
      {
        title: translate('planTableHeaderParticipants'),
        dataIndex: 'participants',
        key: 'participants',
        width: 160,
        render: (_: string[], record): ReactNode => (
          <Tag color="purple">{record.participants.length}</Tag>
        ),
      },
    ],
    [locale, translate]
  );

  const authErrorDetail = describeRemoteError(sessionState.error);
  const planErrorDetail = describeRemoteError(planState.error);
  const pingErrorDetail = describeRemoteError(pingError);

  const lastUpdatedLabel = useMemo(() => {
    if (!planState.lastUpdated) {
      return null;
    }
    const formatted = formatDateTime(planState.lastUpdated, locale);
    if (!formatted) {
      return null;
    }
    return translate('planLastUpdated', { time: formatted });
  }, [planState.lastUpdated, translate, locale]);

  const previewPlan = useMemo(
    () => (previewPlanId ? planList.getCachedPlan(previewPlanId) : null),
    [planList, previewPlanId]
  );

  const availableOwners = useMemo(() => {
    const ownerSet = new Set<string>();
    planState.records.forEach((plan) => {
      if (plan.owner) {
        ownerSet.add(plan.owner);
      }
    });
    return Array.from(ownerSet).sort((a, b) =>
      a.localeCompare(b, locale ?? 'ja-JP', { sensitivity: 'base' })
    );
  }, [planState.records, locale]);

  const authButtonDisabled =
    sessionState.status === 'loading' ||
    credentials.username.trim().length === 0 ||
    credentials.password.length === 0;

  const localeOptions = useMemo(
    () =>
      availableLocales.map((option) => ({
        value: option,
        label: option,
      })),
    [availableLocales]
  );

  return (
    <Layout className="app-layout">
      <Header className="app-header">
        <div className="header-main">
          <Title level={3} className="app-title">
            {translate('appTitle')}
          </Title>
          <Paragraph className="app-subtitle">
            {translate('appDescription')}
          </Paragraph>
        </div>
        <Space align="center" size="middle">
          <Text strong>{translate('localeLabel')}</Text>
          <Select
            className="locale-select"
            value={locale}
            onChange={(value: string) => setLocale(value as Locale)}
            loading={loading}
            options={localeOptions}
          />
        </Space>
      </Header>
      <Content className="app-content">
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Card title={translate('backendStatus')} bordered={false} className="card-block status-card">
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              {ping && (
                <Alert
                  type="success"
                  showIcon
                  message={translate('backendSuccess', { status: ping.status })}
                />
              )}
              {pingErrorDetail && (
                <Alert
                  type="error"
                  showIcon
                  message={translate('backendError', { error: pingErrorDetail })}
                />
              )}
              {!ping && !pingErrorDetail && (
                <Alert type="info" showIcon message={translate('backendPending')} />
              )}
            </Space>
          </Card>

          <Card title={translate('authSectionTitle')} bordered={false} className="card-block">
            {sessionState.session ? (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <Alert
                  type="success"
                  showIcon
                  message={translate('authWelcome', {
                    name: sessionState.session.displayName,
                  })}
                />
                <Text type="secondary">
                  {translate('authTokenExpiry', {
                    time: formatDateTime(sessionState.session.expiresAt, locale),
                  })}
                </Text>
                {sessionState.session.roles.length > 0 && (
                  <Space size="small" wrap>
                    {sessionState.session.roles.map((role) => (
                      <Tag key={role} color="geekblue">
                        {role}
                      </Tag>
                    ))}
                  </Space>
                )}
                <Button type="text" onClick={logout} className="logout-button">
                  {translate('authLogout')}
                </Button>
              </Space>
            ) : (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <Input
                  size="large"
                  placeholder={translate('authUsernameLabel')}
                  autoComplete="username"
                  value={credentials.username}
                  onChange={(event: Event & { target: HTMLInputElement }) =>
                    setCredentials((current) => ({ ...current, username: event.target.value }))
                  }
                  onPressEnter={() => {
                    if (!authButtonDisabled) {
                      login(credentials);
                    }
                  }}
                />
                <Input.Password
                  size="large"
                  placeholder={translate('authPasswordLabel')}
                  autoComplete="current-password"
                  value={credentials.password}
                  onChange={(event: Event & { target: HTMLInputElement }) =>
                    setCredentials((current) => ({ ...current, password: event.target.value }))
                  }
                  onPressEnter={() => {
                    if (!authButtonDisabled) {
                      login(credentials);
                    }
                  }}
                />
                <Button
                  type="primary"
                  block
                  size="large"
                  loading={sessionState.status === 'loading'}
                  onClick={() => login(credentials)}
                  disabled={authButtonDisabled}
                >
                  {sessionState.status === 'loading'
                    ? translate('authLoggingIn')
                    : translate('authSubmit')}
                </Button>
                {authErrorDetail && (
                  <Alert
                    type="error"
                    showIcon
                    message={translate('authError', { error: authErrorDetail })}
                  />
                )}
              </Space>
            )}
          </Card>

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
                  onClick={refresh}
                  disabled={!sessionState.session}
                  loading={planState.status === 'loading'}
                >
                  {translate('planRefresh')}
                </Button>
              </Space>
            }
          >
            {!sessionState.session && <Empty description={translate('planLoginRequired')} />}
            {sessionState.session && (
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <PlanFilters
                  filters={planState.filters}
                  translate={translate}
                  owners={availableOwners}
                  onApply={(filters) => {
                    void planList.applyFilters(filters);
                  }}
                  onReset={() => {
                    void planList.resetFilters();
                  }}
                />
                <RemoteState
                  status={planState.status}
                  error={planState.error}
                  translate={translate}
                  empty={planState.records.length === 0}
                  onRetry={planList.refresh}
                  errorDetail={
                    planErrorDetail
                      ? translate('planError', { error: planErrorDetail })
                      : null
                  }
                  emptyHint={<Text type="secondary">{translate('planEmptyFiltered')}</Text>}
                >
                  <Table<PlanSummary>
                    rowKey="id"
                    dataSource={planState.records}
                    columns={planColumns}
                    pagination={false}
                    rowClassName={(record: PlanSummary) =>
                      ['plan-table-row', previewPlanId === record.id ? 'plan-table-row-active' : '']
                        .filter(Boolean)
                        .join(' ')
                    }
                    onRow={(record: PlanSummary) => ({
                      onClick: () => {
                        setPreviewPlanId(record.id);
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
                      <Text type="secondary">
                        {translate('planPaginationTotal', {
                          total: planState.pagination.total,
                        })}
                      </Text>
                      <Pagination
                        current={planState.pagination.page + 1}
                        pageSize={planState.pagination.pageSize}
                        total={planState.pagination.total}
                        showSizeChanger
                        pageSizeOptions={['10', '20', '50']}
                        onChange={(page) => {
                          void changePage(page - 1);
                        }}
                        onShowSizeChange={(_, size) => {
                          void changePageSize(size);
                        }}
                      />
                    </div>
                  )}
                  {planState.records.length > 0 && (
                    <PlanPreview
                      plan={previewPlan}
                      translate={translate}
                      locale={locale}
                      onClose={() => setPreviewPlanId(null)}
                    />
                  )}
                </RemoteState>
              </Space>
            )}
          </Card>
        </Space>
      </Content>
    </Layout>
  );
}

function App() {
  const localization = useLocalizationState();
  const client = useMemo(
    () =>
      createApiClient({
        getLocale: () => localization.locale,
      }),
    [localization.locale]
  );
  const session = useSessionController(client);
  const planList = usePlanListController(client, session.state.session);

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1677ff',
          borderRadius: 12,
          fontSize: 14,
        },
      }}
    >
      <AppView
        client={client}
        localization={localization}
        session={session}
        planList={planList}
      />
    </ConfigProvider>
  );
}

export default App;
