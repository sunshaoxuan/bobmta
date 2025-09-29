export type Locale = 'ja-JP' | 'zh-CN';

export const DEFAULT_LOCALE: Locale = 'ja-JP';
export const DEFAULT_SUPPORTED_LOCALES: Locale[] = ['ja-JP', 'zh-CN'];

const STORAGE_PREFIX = 'bobmta.localization.bundle';
const CACHE_TTL_MS = 6 * 60 * 60 * 1000; // 6 hours

export const UI_MESSAGE_KEYS = {
  appTitle: 'frontend.app.title',
  appDescription: 'frontend.app.description',
  backendStatus: 'frontend.app.backend.status',
  backendSuccess: 'frontend.app.backend.success',
  backendError: 'frontend.app.backend.error',
  backendErrorStatus: 'frontend.app.backend.error.status',
  backendErrorNetwork: 'frontend.app.backend.error.network',
  backendPending: 'frontend.app.backend.pending',
  localeLabel: 'frontend.app.locale.label',
  authSectionTitle: 'frontend.auth.section.title',
  authUsernameLabel: 'frontend.auth.username.label',
  authPasswordLabel: 'frontend.auth.password.label',
  authSubmit: 'frontend.auth.submit',
  authLoggingIn: 'frontend.auth.loggingIn',
  authLogout: 'frontend.auth.logout',
  authWelcome: 'frontend.auth.welcome',
  authError: 'frontend.auth.error',
  authTokenExpiry: 'frontend.auth.token.expiry',
  planSectionTitle: 'frontend.plan.section.title',
  planRefresh: 'frontend.plan.refresh',
  planEmptyFiltered: 'frontend.plan.table.empty.filtered',
  planTableHeaderId: 'frontend.plan.table.header.id',
  planTableHeaderTitle: 'frontend.plan.table.header.title',
  planTableHeaderOwner: 'frontend.plan.table.header.owner',
  planTableHeaderStatus: 'frontend.plan.table.header.status',
  planTableHeaderWindow: 'frontend.plan.table.header.window',
  planTableHeaderProgress: 'frontend.plan.table.header.progress',
  planTableHeaderParticipants: 'frontend.plan.table.header.participants',
  planWindowRange: 'frontend.plan.table.window.range',
  planWindowMissing: 'frontend.plan.table.window.missing',
  planLoading: 'frontend.plan.table.loading',
  planEmpty: 'frontend.plan.table.empty',
  planLoginRequired: 'frontend.plan.loginRequired',
  planError: 'frontend.plan.table.error',
  planStatusDesign: 'frontend.plan.status.design',
  planStatusScheduled: 'frontend.plan.status.scheduled',
  planStatusInProgress: 'frontend.plan.status.inProgress',
  planStatusCompleted: 'frontend.plan.status.completed',
  planStatusCancelled: 'frontend.plan.status.cancelled',
  planFilterOwnerLabel: 'frontend.plan.filter.owner.label',
  planFilterOwnerAll: 'frontend.plan.filter.owner.all',
  planFilterStatusLabel: 'frontend.plan.filter.status.label',
  planFilterStatusAll: 'frontend.plan.filter.status.all',
  planFilterKeywordLabel: 'frontend.plan.filter.keyword.label',
  planFilterKeywordPlaceholder: 'frontend.plan.filter.keyword.placeholder',
  planFilterApply: 'frontend.plan.filter.apply',
  planFilterReset: 'frontend.plan.filter.reset',
  planFilterWindowLabel: 'frontend.plan.filter.window.label',
  planFilterWindowPlaceholderStart: 'frontend.plan.filter.window.placeholder.start',
  planFilterWindowPlaceholderEnd: 'frontend.plan.filter.window.placeholder.end',
  planPaginationTotal: 'frontend.plan.pagination.total',
  planLastUpdated: 'frontend.plan.lastUpdated',
  planCacheHit: 'frontend.plan.cache.hit',
  planPreviewHeader: 'frontend.plan.preview.header',
  planPreviewClose: 'frontend.plan.preview.close',
  planPreviewEmptyTitle: 'frontend.plan.preview.empty.title',
  planPreviewEmptyDescription: 'frontend.plan.preview.empty.description',
  planPreviewDescription: 'frontend.plan.preview.description',
  planPreviewOwnerLabel: 'frontend.plan.preview.owner',
  planPreviewWindowLabel: 'frontend.plan.preview.window',
  planPreviewProgressLabel: 'frontend.plan.preview.progress',
  planPreviewParticipantsLabel: 'frontend.plan.preview.participants',
  planPreviewParticipantsEmpty: 'frontend.plan.preview.participants.empty',
  planPreviewStartLabel: 'frontend.plan.preview.start',
  planPreviewEndLabel: 'frontend.plan.preview.end',
  planPreviewEmptyValue: 'frontend.plan.preview.emptyValue',
  planPreviewComingTitle: 'frontend.plan.preview.coming.title',
  planPreviewComingDescription: 'frontend.plan.preview.coming.description',
  planDetailRefresh: 'frontend.plan.detail.refresh',
  planDetailLastUpdated: 'frontend.plan.detail.lastUpdated',
  planDetailOriginCache: 'frontend.plan.detail.origin.cache',
  planDetailOriginNetwork: 'frontend.plan.detail.origin.network',
  planDetailCustomerLabel: 'frontend.plan.detail.customer',
  planDetailTagsLabel: 'frontend.plan.detail.tags',
  planDetailDescriptionFallback: 'frontend.plan.detail.descriptionFallback',
  planDetailTimelineTitle: 'frontend.plan.detail.timeline.title',
  planDetailTimelineEmpty: 'frontend.plan.detail.timeline.empty',
  planDetailRemindersTitle: 'frontend.plan.detail.reminders.title',
  planDetailRemindersEmpty: 'frontend.plan.detail.reminders.empty',
  planDetailReminderOffsetMinutes: 'frontend.plan.detail.reminders.offsetMinutes',
  planDetailReminderInactive: 'frontend.plan.detail.reminders.inactive',
  planDetailReminderActionEdit: 'frontend.plan.detail.reminders.action.edit',
  planDetailReminderActionToggle: 'frontend.plan.detail.reminders.action.toggle',
  planDetailReminderActionPending: 'frontend.plan.detail.reminders.action.pending',
  planDetailReminderSelectionHint: 'frontend.plan.detail.reminders.selectionHint',
  planDetailNodesTitle: 'frontend.plan.detail.nodes.title',
  planDetailNodesEmpty: 'frontend.plan.detail.nodes.empty',
  planDetailActionsTitle: 'frontend.plan.detail.actions.title',
  planDetailActionsEmpty: 'frontend.plan.detail.actions.empty',
  planDetailActionStart: 'frontend.plan.detail.actions.start',
  planDetailActionComplete: 'frontend.plan.detail.actions.complete',
  planDetailActionHandover: 'frontend.plan.detail.actions.handover',
  planDetailActionUnavailable: 'frontend.plan.detail.actions.unavailable',
  planDetailActionPending: 'frontend.plan.detail.actions.pending',
  planDetailActionAssigneeMissing: 'frontend.plan.detail.actions.assigneeMissing',
  planDetailActionPermissionHint: 'frontend.plan.detail.actions.permissionHint',
  planDetailNodeStatusPending: 'frontend.plan.detail.nodes.status.pending',
  planDetailNodeStatusInProgress: 'frontend.plan.detail.nodes.status.inProgress',
  planDetailNodeStatusDone: 'frontend.plan.detail.nodes.status.done',
  planDetailNodeStatusCancelled: 'frontend.plan.detail.nodes.status.cancelled',
  planDetailNodeStatusSkipped: 'frontend.plan.detail.nodes.status.skipped',
  planDetailNodeOrderTag: 'frontend.plan.detail.nodes.tag.order',
  planDetailNodeActionTag: 'frontend.plan.detail.nodes.tag.action',
  planDetailNodeAssigneeTag: 'frontend.plan.detail.nodes.tag.assignee',
  planDetailNodeDurationPlannedTag: 'frontend.plan.detail.nodes.tag.durationPlanned',
  planDetailNodeActualStartTag: 'frontend.plan.detail.nodes.tag.actualStart',
  planDetailNodeActualEndTag: 'frontend.plan.detail.nodes.tag.actualEnd',
  planDetailNodeResultTag: 'frontend.plan.detail.nodes.tag.result',
  planReminderChannelEmail: 'frontend.plan.reminder.channel.email',
  planReminderChannelSms: 'frontend.plan.reminder.channel.sms',
  planReminderChannelIm: 'frontend.plan.reminder.channel.im',
  planReminderChannelWebhook: 'frontend.plan.reminder.channel.webhook',
  commonStateLoadingTitle: 'frontend.common.state.loading.title',
  commonStateLoadingDescription: 'frontend.common.state.loading.description',
  commonStateErrorTitle: 'frontend.common.state.error.title',
  commonStateErrorDescription: 'frontend.common.state.error.description',
  commonStateEmptyDescription: 'frontend.common.state.empty.description',
  commonStateRetry: 'frontend.common.state.retry',
} as const;

export type UiMessageKey = keyof typeof UI_MESSAGE_KEYS;

export interface LocalizationBundle {
  locale: Locale;
  defaultLocale: Locale;
  version: string;
  supportedLocales: Locale[];
  messages: Record<string, string>;
  defaultMessages: Record<string, string>;
}

type CacheRecord = LocalizationBundle & { fetchedAt: number };

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type LocalizationBundlePayload = {
  locale: string;
  defaultLocale: string;
  version?: string;
  supportedLocales?: string[];
  messages?: Record<string, string>;
  defaultMessages?: Record<string, string>;
};

export function getCachedLocalization(
  locale: Locale
): { bundle: LocalizationBundle; stale: boolean } | null {
  const record = readCache(locale);
  if (!record) {
    return null;
  }
  return { bundle: toBundle(record), stale: isStale(record) };
}

export async function fetchLocalization(locale: Locale): Promise<LocalizationBundle> {
  const record = await requestBundle(locale);
  persistCache(locale, record);
  return toBundle(record);
}

export function getSupportedLocales(bundle?: LocalizationBundle | null): Locale[] {
  if (bundle && bundle.supportedLocales.length > 0) {
    return bundle.supportedLocales;
  }
  return DEFAULT_SUPPORTED_LOCALES;
}

export function formatMessage(
  bundle: LocalizationBundle | null,
  key: UiMessageKey,
  values?: Record<string, string | number>
): string {
  const resourceKey = UI_MESSAGE_KEYS[key];
  const template =
    bundle?.messages[resourceKey] ??
    bundle?.defaultMessages[resourceKey] ??
    resourceKey;
  return applyTemplate(template, values);
}

function applyTemplate(template: string, values?: Record<string, string | number>): string {
  if (!values) {
    return template;
  }
  return Object.entries(values).reduce((acc, [token, value]) => {
    const pattern = new RegExp(`\\{${token}\\}`, 'g');
    return acc.replace(pattern, String(value));
  }, template);
}

function requestCacheKey(locale: Locale): string {
  return `${STORAGE_PREFIX}:${locale}`;
}

function readCache(locale: Locale): CacheRecord | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const raw = window.localStorage.getItem(requestCacheKey(locale));
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as CacheRecord;
    parsed.supportedLocales = normalizeLocales(parsed.supportedLocales);
    parsed.locale = normalizeLocale(parsed.locale);
    parsed.defaultLocale = normalizeLocale(parsed.defaultLocale);
    if (typeof parsed.fetchedAt !== 'number') {
      parsed.fetchedAt = 0;
    }
    return parsed;
  } catch {
    return null;
  }
}

function persistCache(locale: Locale, record: CacheRecord): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    const serialized = JSON.stringify(record);
    window.localStorage.setItem(requestCacheKey(locale), serialized);
  } catch {
    // ignore persistence errors to keep the app resilient when storage is unavailable
  }
}

function isStale(record: CacheRecord): boolean {
  return Date.now() - record.fetchedAt > CACHE_TTL_MS;
}

async function requestBundle(locale: Locale): Promise<CacheRecord> {
  const response = await fetch('/api/v1/i18n/messages', {
    headers: {
      'Accept-Language': locale,
    },
  });
  if (!response.ok) {
    throw new Error(`Failed to load localization bundle: ${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<LocalizationBundlePayload>;
  if (!body.data) {
    throw new Error('Localization bundle response is empty');
  }
  const payload = body.data;
  return {
    locale: normalizeLocale(payload.locale),
    defaultLocale: normalizeLocale(payload.defaultLocale),
    version: payload.version ?? '1',
    supportedLocales: normalizeLocales(payload.supportedLocales),
    messages: payload.messages ?? {},
    defaultMessages: payload.defaultMessages ?? {},
    fetchedAt: Date.now(),
  };
}

function toBundle(record: CacheRecord): LocalizationBundle {
  return {
    locale: normalizeLocale(record.locale),
    defaultLocale: normalizeLocale(record.defaultLocale),
    version: record.version,
    supportedLocales: normalizeLocales(record.supportedLocales),
    messages: record.messages,
    defaultMessages: record.defaultMessages,
  };
}

function normalizeLocales(locales?: Iterable<string>): Locale[] {
  if (!locales) {
    return DEFAULT_SUPPORTED_LOCALES;
  }
  const normalized: Locale[] = [];
  for (const locale of locales) {
    const value = normalizeLocale(locale);
    if (!normalized.includes(value)) {
      normalized.push(value);
    }
  }
  return normalized.length > 0 ? normalized : DEFAULT_SUPPORTED_LOCALES;
}

function normalizeLocale(value?: string): Locale {
  if (!value) {
    return DEFAULT_LOCALE;
  }
  const normalized = value.replace('_', '-');
  const match = DEFAULT_SUPPORTED_LOCALES.find(
    (candidate) => candidate.toLowerCase() === normalized.toLowerCase()
  );
  if (match) {
    return match;
  }
  const language = normalized.split('-')[0]?.toLowerCase();
  const byLanguage = DEFAULT_SUPPORTED_LOCALES.find(
    (candidate) => candidate.split('-')[0].toLowerCase() === language
  );
  return byLanguage ?? DEFAULT_LOCALE;
}
