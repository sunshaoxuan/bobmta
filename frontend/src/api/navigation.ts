import type { ApiClient } from './client';
import type { UiMessageKey } from '../i18n/localization';

type NavigationMenuRecord = {
  key: string;
  path: string;
  labelKey: UiMessageKey;
  roles?: string[];
  children?: NavigationMenuRecord[];
};

export type NavigationMenuResponse = NavigationMenuRecord[];

export async function fetchNavigationMenu(
  client: ApiClient,
  options: { signal?: AbortSignal; authToken?: string | null } = {}
): Promise<NavigationMenuResponse> {
  return client.get<NavigationMenuResponse>('/api/v1/navigation/menu', {
    signal: options.signal,
    authToken: options.authToken ?? null,
  });
}

export type NavigationMenuPayload = NavigationMenuRecord;
