import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import './App.css';
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

  const t = (key: UiMessageKey, values?: Record<string, string | number>) => {
    if (!bundle) {
      return '…';
    }
    return formatMessage(bundle, key, values);
  };

  const availableLocales = useMemo(() => getSupportedLocales(bundle), [bundle]);

  const describeRemoteError = (error: RemoteError | null) => {
    if (!error) {
      return null;
    }
    if (error.type === 'status') {
      return t('backendErrorStatus', { status: error.status });
    }
    return t('backendErrorNetwork');
  };

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

  const handleLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (authLoading) {
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
        username: credentials.username.trim(),
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
  };

  const handleLogout = () => {
    setSession(null);
    setPlans([]);
    setPlansError(null);
  };

  const authErrorDetail = describeRemoteError(authError);
  const planErrorDetail = describeRemoteError(plansError);

  return (
    <div className="app">
      <div className="locale-switcher">
        <label htmlFor="locale-select">{t('localeLabel')}:</label>
        <select
          id="locale-select"
          value={locale}
          onChange={(event) => setLocale(event.target.value as Locale)}
          disabled={loadingBundle}
        >
          {availableLocales.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </div>
      <h1>{t('appTitle')}</h1>
      <p>{t('appDescription')}</p>
      <section className="status-panel">
        <h2>{t('backendStatus')}</h2>
        {ping && <p className="success">{t('backendSuccess', { status: ping.status })}</p>}
        {pingError && <p className="error">{t('backendError', { error: t(pingError.key, pingError.values) })}</p>}
        {!ping && !pingError && <p>{t('backendPending')}</p>}
      </section>
      <section className="auth-panel">
        <h2>{t('authSectionTitle')}</h2>
        {session ? (
          <div className="auth-summary">
            <p>{t('authWelcome', { name: session.displayName })}</p>
            <p>{t('authTokenExpiry', { time: formatDateTime(session.expiresAt) })}</p>
            <button type="button" onClick={handleLogout} className="link-button">
              {t('authLogout')}
            </button>
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleLogin}>
            <label className="field">
              <span>{t('authUsernameLabel')}</span>
              <input
                type="text"
                name="username"
                autoComplete="username"
                value={credentials.username}
                onChange={(event) =>
                  setCredentials((current) => ({ ...current, username: event.target.value }))
                }
                disabled={authLoading}
              />
            </label>
            <label className="field">
              <span>{t('authPasswordLabel')}</span>
              <input
                type="password"
                name="password"
                autoComplete="current-password"
                value={credentials.password}
                onChange={(event) =>
                  setCredentials((current) => ({ ...current, password: event.target.value }))
                }
                disabled={authLoading}
              />
            </label>
            <button
              type="submit"
              className="primary-button"
              disabled={
                authLoading ||
                credentials.username.trim().length === 0 ||
                credentials.password.length === 0
              }
            >
              {authLoading ? t('authLoggingIn') : t('authSubmit')}
            </button>
          </form>
        )}
        {authErrorDetail && <p className="error">{t('authError', { error: authErrorDetail })}</p>}
      </section>
      <section className="plan-panel">
        <div className="plan-header">
          <h2>{t('planSectionTitle')}</h2>
          <button
            type="button"
            className="link-button"
            onClick={() => loadPlans()}
            disabled={!session || plansLoading}
          >
            {t('planRefresh')}
          </button>
        </div>
        {!session && <p>{t('planLoginRequired')}</p>}
        {session && plansLoading && <p>{t('planLoading')}</p>}
        {session && planErrorDetail && (
          <p className="error">{t('planError', { error: planErrorDetail })}</p>
        )}
        {session && !plansLoading && !planErrorDetail && plans.length === 0 && (
          <p>{t('planEmpty')}</p>
        )}
        {session && !plansLoading && !planErrorDetail && plans.length > 0 && (
          <table className="plan-table">
            <thead>
              <tr>
                <th>{t('planTableHeaderId')}</th>
                <th>{t('planTableHeaderTitle')}</th>
                <th>{t('planTableHeaderOwner')}</th>
                <th>{t('planTableHeaderStatus')}</th>
                <th>{t('planTableHeaderWindow')}</th>
                <th>{t('planTableHeaderProgress')}</th>
                <th>{t('planTableHeaderParticipants')}</th>
              </tr>
            </thead>
            <tbody>
              {plans.map((plan) => (
                <tr key={plan.id}>
                  <td>{plan.id}</td>
                  <td>{plan.title}</td>
                  <td>{plan.owner}</td>
                  <td>{t(STATUS_LABEL[plan.status])}</td>
                  <td>{formatPlanWindow(plan)}</td>
                  <td>{`${plan.progress}%`}</td>
                  <td>{plan.participants.length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

export default App;
