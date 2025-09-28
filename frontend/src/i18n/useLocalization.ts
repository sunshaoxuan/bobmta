import { useEffect, useState, useCallback, useMemo } from '../../vendor/react/index.js';
import {
  DEFAULT_LOCALE,
  fetchLocalization,
  formatMessage,
  getCachedLocalization,
  getSupportedLocales,
  type Locale,
  type LocalizationBundle,
  type UiMessageKey,
} from './localization';

export type LocalizationState = {
  locale: Locale;
  bundle: LocalizationBundle | null;
  loading: boolean;
  setLocale: (locale: Locale) => void;
  translate: (key: UiMessageKey, values?: Record<string, string | number>) => string;
  availableLocales: Locale[];
};

export function useLocalizationState(initialLocale: Locale = DEFAULT_LOCALE): LocalizationState {
  const [locale, setLocale] = useState<Locale>(initialLocale);
  const [bundle, setBundle] = useState<LocalizationBundle | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    const cached = getCachedLocalization(locale);
    if (cached) {
      setBundle(cached.bundle);
    } else {
      setBundle(null);
    }

    let cancelled = false;
    if (!cached || cached.stale) {
      setLoading(true);
      fetchLocalization(locale)
        .then((fresh) => {
          if (!cancelled) {
            setBundle(fresh);
          }
        })
        .catch(() => {
          // retain existing bundle on failure
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });
    } else {
      setLoading(false);
    }

    return () => {
      cancelled = true;
    };
  }, [locale]);

  const translate = useCallback(
    (key: UiMessageKey, values?: Record<string, string | number>) => {
      if (!bundle) {
        return '…';
      }
      return formatMessage(bundle, key, values);
    },
    [bundle]
  );

  const availableLocales = useMemo(() => getSupportedLocales(bundle), [bundle]);

  return {
    locale,
    bundle,
    loading,
    setLocale,
    translate,
    availableLocales,
  };
}
