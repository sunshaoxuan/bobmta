import React, { useCallback, useEffect, useMemo, useState } from '../vendor/react/index.js';
import './App.css';
import {
  Alert,
  Button,
  Card,
  ConfigProvider,
  Empty,
  Input,
  Layout,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from '../vendor/antd/index.js';
import type { TableColumnsType } from '../vendor/antd/index.js';
import {
  DEFAULT_LOCALE,
  fetchLocalization,
  formatMessage,
  getCachedLocalization,
  getSupportedLocales,
  type Locale,
  type LocalizationBundle,
  type UiMessageKey,
} from './i18n/localization';

const { Header, Content } = Layout;
const { Title, Paragraph, Text } = Typography;

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T | null;
};

type PingResponse = {
  status: string;
};

type LoginResponse = {
  token: string;
  expiresAt: string;
  displayName: string;
  roles: string[];
};

type PageResponse<T> = {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
};

type PlanStatus = 'DESIGN' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

type PlanSummary = {
  id: string;
  title: string;
  owner: string;
  status: PlanStatus;
  plannedStartTime?: string | null;
  plannedEndTime?: string | null;
  participants: string[];
  progress: number;
};

type RemoteError =
  | { type: 'status'; status: number }
  | { type: 'network' };

const STATUS_LABEL: Record<PlanStatus, UiMessageKey> = {
  DESIGN: 'planStatusDesign',
  SCHEDULED: 'planStatusScheduled',
  IN_PROGRESS: 'planStatusInProgress',
  COMPLETED: 'planStatusCompleted',
  CANCELLED: 'planStatusCancelled',
};

const PLAN_STATUS_COLOR: Record<
  PlanStatus,
  'default' | 'processing' | 'success' | 'error' | 'warning'
> = {
  DESIGN: 'default',
  SCHEDULED: 'warning',
  IN_PROGRESS: 'processing',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

function App() {
  const [locale, setLocale] = useState<Locale>(DEFAULT_LOCALE);
  const [bundle, setBundle] = useState<LocalizationBundle | null>(null);
  const [loadingBundle, setLoadingBundle] = useState<boolean>(false);
  const [ping, setPing] = useState<PingResponse | null>(null);
  const [pingError, setPingError] = useState<{
    key: UiMessageKey;
    values?: Record<string, string | number>;
  } | null>(null);
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [authLoading, setAuthLoading] = useState(false);
  const [session, setSession] = useState<LoginResponse | null>(null);
  const [authError, setAuthError] = useState<RemoteError | null>(null);
  const [plans, setPlans] = useState<PlanSummary[]>([]);
  const [plansLoading, setPlansLoading] = useState(false);
  const [plansError, setPlansError] = useState<RemoteError | null>(null);

  useEffect(() => {
    const cached = getCachedLocalization(locale);
    if (cached) {
      setBundle(cached.bundle);
    } else {
      setBundle(null);
    }

    let cancelled = false;
    if (!cached || cached.stale) {
      setLoadingBundle(true);
      fetchLocalization(locale)
        .then((fresh) => {
          if (!cancelled) {
            setBundle(fresh);
          }
        })
        .catch(() => {
          // keep the cached bundle if fetching fails
        })
        .finally(() => {
          if (!cancelled) {
            setLoadingBundle(false);
          }
        });
    } else {
      setLoadingBundle(false);
    }

    return () => {
      cancelled = true;
    };
  }, [locale]);

  useEffect(() => {
    setPing(null);
    setPingError(null);
    fetch('/api/ping', {
      headers: {
        'Accept-Language': locale,
      },
    })
      .then(async (response) => {
        if (!response.ok) {
          setPingError({ key: 'backendErrorStatus', values: { status: response.status } });
          return;
        }
        const body = (await response.json()) as PingResponse;
        setPing(body);
      })
      .catch(() => {
        setPingError({ key: 'backendErrorNetwork' });
      });
  }, [locale]);

  const t = useCallback(
    (key: UiMessageKey, values?: Record<string, string | number>) => {
      if (!bundle) {
        return '…';
      }
      return formatMessage(bundle, key, values);
    },
    [bundle]
  );

  const availableLocales = useMemo(() => getSupportedLocales(bundle), [bundle]);

  const describeRemoteError = useCallback(
    (error: RemoteError | null) => {
      if (!error) {
        return null;
      }
      if (error.type === 'status') {
        return t('backendErrorStatus', { status: error.status });
      }
      return t('backendErrorNetwork');
    },
    [t]
  );

  const formatDateTime = useCallback(
    (value?: string | null) => {
      if (!value) {
        return '';
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return '';
      }
      return new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date);
    },
    [locale]
  );

  const formatPlanWindow = useCallback(
    (plan: PlanSummary) => {
      const start = formatDateTime(plan.plannedStartTime ?? null);
      const end = formatDateTime(plan.plannedEndTime ?? null);
      if (start && end) {
        return t('planWindowRange', { start, end });
      }
      if (start) {
        return start;
      }
      if (end) {
        return end;
      }
      return t('planWindowMissing');
    },
    [formatDateTime, t]
  );

  const loadPlans = useCallback(
    (signal?: AbortSignal) => {
      if (!session) {
        setPlans([]);
        setPlansError(null);
        setPlansLoading(false);
        return;
      }
      setPlansLoading(true);
      setPlansError(null);
      fetch('/api/v1/plans?page=0&size=20', {
        headers: {
          'Accept-Language': locale,
          Authorization: `Bearer ${session.token}`,
        },
        signal,
      })
        .then(async (response) => {
          if (signal?.aborted) {
            return;
          }
          if (!response.ok) {
            setPlansError({ type: 'status', status: response.status });
            return;
          }
          const body = (await response.json()) as ApiResponse<PageResponse<PlanSummary>>;
          if (!body.data) {
            setPlansError({ type: 'network' });
            return;
          }
          setPlans(body.data.list ?? []);
        })
        .catch((error: unknown) => {
          if (signal?.aborted) {
            return;
          }
          if (error instanceof DOMException && error.name === 'AbortError') {
            return;
          }
          setPlansError({ type: 'network' });
        })
        .finally(() => {
          if (signal?.aborted) {
            return;
          }
          setPlansLoading(false);
        });
    },
    [locale, session]
  );

  useEffect(() => {
    if (!session) {
      setPlans([]);
      setPlansError(null);
      setPlansLoading(false);
      return;
    }
    const controller = new AbortController();
    loadPlans(controller.signal);
    return () => {
      controller.abort();
    };
  }, [session, locale, loadPlans]);

  const handleLogin = useCallback(() => {
    const username = credentials.username.trim();
    if (authLoading || username.length === 0 || credentials.password.length === 0) {
      return;
    }
    setAuthLoading(true);
    setAuthError(null);
    fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': locale,
      },
      body: JSON.stringify({
        username,
        password: credentials.password,
      }),
    })
      .then(async (response) => {
        if (!response.ok) {
          setAuthError({ type: 'status', status: response.status });
          return;
        }
        const body = (await response.json()) as ApiResponse<LoginResponse>;
        if (!body.data) {
          setAuthError({ type: 'network' });
          return;
        }
        setSession(body.data);
        setCredentials({ username: '', password: '' });
        setAuthError(null);
      })
      .catch(() => {
        setAuthError({ type: 'network' });
      })
      .finally(() => {
        setAuthLoading(false);
      });
  }, [authLoading, credentials, locale]);

  const handleLogout = useCallback(() => {
    setSession(null);
    setPlans([]);
    setPlansError(null);
  }, []);

  const authErrorDetail = describeRemoteError(authError);
  const planErrorDetail = describeRemoteError(plansError);

  const planColumns = useMemo<TableColumnsType<PlanSummary>>(
    () => [
      {
        title: t('planTableHeaderId'),
        dataIndex: 'id',
        key: 'id',
        width: 160,
        render: (value: string) => <Text code>{value}</Text>,
      },
      {
        title: t('planTableHeaderTitle'),
        dataIndex: 'title',
        key: 'title',
        ellipsis: true,
        render: (value: string) => <Text strong>{value}</Text>,
      },
      {
        title: t('planTableHeaderOwner'),
        dataIndex: 'owner',
        key: 'owner',
        render: (value: string) => <Tag color="geekblue">{value}</Tag>,
      },
      {
        title: t('planTableHeaderStatus'),
        dataIndex: 'status',
        key: 'status',
        render: (_: PlanStatus, record) => (
          <Tag color={PLAN_STATUS_COLOR[record.status]}>{t(STATUS_LABEL[record.status])}</Tag>
        ),
      },
      {
        title: t('planTableHeaderWindow'),
        dataIndex: 'plannedStartTime',
        key: 'window',
        render: (_: unknown, record) => <Text>{formatPlanWindow(record)}</Text>,
      },
      {
        title: t('planTableHeaderProgress'),
        dataIndex: 'progress',
        key: 'progress',
        width: 200,
        render: (value: number) => (
          <Progress percent={Math.max(0, Math.min(100, Math.round(value ?? 0)))} size="small" />
        ),
      },
      {
        title: t('planTableHeaderParticipants'),
        dataIndex: 'participants',
        key: 'participants',
        width: 160,
        render: (_: string[], record) => <Tag color="purple">{record.participants.length}</Tag>,
      },
    ],
    [formatPlanWindow, t]
  );

  const authButtonDisabled =
    authLoading || credentials.username.trim().length === 0 || credentials.password.length === 0;

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
      <Layout className="app-layout">
        <Header className="app-header">
          <div className="header-main">
            <Title level={3} className="app-title">
              {t('appTitle')}
            </Title>
            <Paragraph className="app-subtitle">{t('appDescription')}</Paragraph>
          </div>
          <Space align="center" size="middle">
            <Text strong>{t('localeLabel')}</Text>
            <Select
              className="locale-select"
              value={locale}
              onChange={(value: string) => setLocale(value as Locale)}
              loading={loadingBundle}
              options={availableLocales.map((option) => ({ value: option, label: option }))}
            />
          </Space>
        </Header>
        <Content className="app-content">
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card title={t('backendStatus')} bordered={false} className="card-block status-card">
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                {ping && (
                  <Alert
                    type="success"
                    showIcon
                    message={t('backendSuccess', { status: ping.status })}
                  />
                )}
                {pingError && (
                  <Alert
                    type="error"
                    showIcon
                    message={t('backendError', { error: t(pingError.key, pingError.values) })}
                  />
                )}
                {!ping && !pingError && <Alert type="info" showIcon message={t('backendPending')} />}
              </Space>
            </Card>

            <Card title={t('authSectionTitle')} bordered={false} className="card-block">
              {session ? (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Alert
                    type="success"
                    showIcon
                    message={t('authWelcome', { name: session.displayName })}
                  />
                  <Text type="secondary">
                    {t('authTokenExpiry', { time: formatDateTime(session.expiresAt) })}
                  </Text>
                  {session.roles.length > 0 && (
                    <Space size="small" wrap>
                      {session.roles.map((role) => (
                        <Tag key={role} color="geekblue">
                          {role}
                        </Tag>
                      ))}
                    </Space>
                  )}
                  <Button type="text" onClick={handleLogout} className="logout-button">
                    {t('authLogout')}
                  </Button>
                </Space>
              ) : (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Input
                    size="large"
                    placeholder={t('authUsernameLabel')}
                    autoComplete="username"
                    value={credentials.username}
                    onChange={(event: Event & { target: HTMLInputElement }) =>
                      setCredentials((current) => ({ ...current, username: event.target.value }))
                    }
                    onPressEnter={() => {
                      if (!authButtonDisabled) {
                        handleLogin();
                      }
                    }}
                  />
                  <Input.Password
                    size="large"
                    placeholder={t('authPasswordLabel')}
                    autoComplete="current-password"
                    value={credentials.password}
                    onChange={(event: Event & { target: HTMLInputElement }) =>
                      setCredentials((current) => ({ ...current, password: event.target.value }))
                    }
                    onPressEnter={() => {
                      if (!authButtonDisabled) {
                        handleLogin();
                      }
                    }}
                  />
                  <Button
                    type="primary"
                    block
                    size="large"
                    loading={authLoading}
                    onClick={handleLogin}
                    disabled={authButtonDisabled}
                  >
                    {authLoading ? t('authLoggingIn') : t('authSubmit')}
                  </Button>
                  {authErrorDetail && (
                    <Alert
                      type="error"
                      showIcon
                      message={t('authError', { error: authErrorDetail })}
                    />
                  )}
                </Space>
              )}
            </Card>

            <Card
              title={t('planSectionTitle')}
              bordered={false}
              className="card-block"
              extra={
                <Button type="link" onClick={() => loadPlans()} disabled={!session} loading={plansLoading}>
                  {t('planRefresh')}
                </Button>
              }
            >
              {!session && <Empty description={t('planLoginRequired')} />}
              {session && planErrorDetail && (
                <Alert
                  type="error"
                  showIcon
                  message={t('planError', { error: planErrorDetail })}
                  style={{ marginBottom: 16 }}
                />
              )}
              {session && !planErrorDetail && (
                <Table<PlanSummary>
                  rowKey="id"
                  dataSource={plans}
                  columns={planColumns}
                  pagination={false}
                  loading={{ spinning: plansLoading, tip: t('planLoading') }}
                  locale={{ emptyText: t('planEmpty') }}
                  scroll={{ x: true }}
                />
              )}
            </Card>
          </Space>
        </Content>
      </Layout>
    </ConfigProvider>
  );
}

export default App;
