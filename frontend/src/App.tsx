import { useEffect, useState } from 'react';
import './App.css';
import { availableLocales, defaultLocale, formatMessage, type Locale, type MessageKey } from './i18n/messages';

type PingResponse = {
  status: string;
};

function App() {
  const [ping, setPing] = useState<PingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [locale, setLocale] = useState<Locale>(defaultLocale);

  const t = (key: MessageKey, values?: Record<string, string | number>) =>
    formatMessage(locale, key, values);

  useEffect(() => {
    setPing(null);
    setError(null);
    const acceptLanguage = locale === 'ja' ? 'ja-JP' : 'zh-CN';
    fetch('/api/ping', {
      headers: {
        'Accept-Language': acceptLanguage,
      },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const body = (await response.json()) as PingResponse;
        setPing(body);
      })
      .catch((err) => {
        setError(err.message);
      });
  }, [locale]);

  return (
    <div className="app">
      <div className="locale-switcher">
        <label htmlFor="locale-select">{t('localeLabel')}:</label>
        <select
          id="locale-select"
          value={locale}
          onChange={(event) => setLocale(event.target.value as Locale)}
        >
          {availableLocales().map((option) => (
            <option key={option} value={option}>
              {option.toUpperCase()}
            </option>
          ))}
        </select>
      </div>
      <h1>{t('appTitle')}</h1>
      <p>{t('appDescription')}</p>
      <section className="status-panel">
        <h2>{t('backendStatus')}</h2>
        {ping && <p className="success">{t('backendSuccess', { status: ping.status })}</p>}
        {error && <p className="error">{t('backendError', { error })}</p>}
        {!ping && !error && <p>{t('backendPending')}</p>}
      </section>
    </div>
  );
}

export default App;
