export type Locale = 'ja' | 'zh';

export type MessageKey =
  | 'appTitle'
  | 'appDescription'
  | 'backendStatus'
  | 'backendSuccess'
  | 'backendError'
  | 'backendPending'
  | 'localeLabel';

const translations: Record<Locale, Record<MessageKey, string>> = {
  ja: {
    appTitle: 'BOB MTA Maintain Assistants',
    appDescription: '最小構成のフロントエンドとバックエンド接続を確認するページです。',
    backendStatus: 'バックエンド連携状況',
    backendSuccess: 'バックエンド応答：{status}',
    backendError: 'リクエスト失敗：{error}',
    backendPending: 'バックエンド接続を確認しています...',
    localeLabel: '表示言語',
  },
  zh: {
    appTitle: 'BOB MTA Maintain Assistants',
    appDescription: '这是用于验证前后端最小连通性的页面。',
    backendStatus: '后端连通性',
    backendSuccess: '后端响应：{status}',
    backendError: '请求失败：{error}',
    backendPending: '正在检查后端连接...',
    localeLabel: '显示语言',
  },
};

export const defaultLocale: Locale = 'ja';

export function formatMessage(
  locale: Locale,
  key: MessageKey,
  values?: Record<string, string | number>
): string {
  const template = translations[locale][key];
  if (!values) {
    return template;
  }
  return Object.entries(values).reduce((acc, [k, v]) => acc.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v)), template);
}

export function availableLocales(): Locale[] {
  return Object.keys(translations) as Locale[];
}
