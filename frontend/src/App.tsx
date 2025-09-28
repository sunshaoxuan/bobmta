import { useEffect, useMemo, useState } from 'react';
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

type PingResponse = {
  status: string;
};

function App() {
  const [locale, setLocale] = useState<Locale>(DEFAULT_LOCALE);
  const [bundle, setBundle] = useState<LocalizationBundle | null>(null);
  const [loadingBundle, setLoadingBundle] = useState<boolean>(false);
  const [ping, setPing] = useState<PingResponse | null>(null);
  const [error, setError] = useState<{
    key: UiMessageKey;
    values?: Record<string, string | number>;
  } | null>(null);

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
    setError(null);
    fetch('/api/ping', {
      headers: {
        'Accept-Language': locale,
      },
    })
      .then(async (response) => {
        if (!response.ok) {
          setError({ key: 'backendErrorStatus', values: { status: response.status } });
          return;
        }
        const body = (await response.json()) as PingResponse;
        setPing(body);
      })
      .catch(() => {
        setError({ key: 'backendErrorNetwork' });
      });
  }, [locale]);

  const t = (key: UiMessageKey, values?: Record<string, string | number>) => {
    if (!bundle) {
      return '…';
    }
    return formatMessage(bundle, key, values);
  };

  const availableLocales = useMemo(() => getSupportedLocales(bundle), [bundle]);

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
        {error && <p className="error">{t('backendError', { error: t(error.key, error.values) })}</p>}
        {!ping && !error && <p>{t('backendPending')}</p>}
      </section>
    </div>
  );
}

export default App;
