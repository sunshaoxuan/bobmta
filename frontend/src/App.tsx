import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from '../vendor/react/index.js';
import './App.css';
import {
  Alert,
  Button,
  Card,
  ConfigProvider,
  Input,
  Layout,
  Progress,
  Result,
  Segmented,
  Space,
  Tag,
  Typography,
} from '../vendor/antd/index.js';
import type { MenuProps, TableColumnsType } from '../vendor/antd/index.js';
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
  arePlanListFiltersEqual,
  type PlanListViewMode,
  DEFAULT_PLAN_LIST_VIEW_MODE,
} from './state/planList';
import {
  usePlanDetailController,
  type PlanDetailController,
} from './state/planDetail';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from './constants/planStatus';
import { PlanListBoard } from './components/PlanListBoard';
import { PlanByCustomerView } from './components/PlanByCustomerView';
import { PlanCalendarView } from './components/PlanCalendarView';
import { HeaderNav } from './components/HeaderNav';
import {
  useSessionController,
  type SessionController,
  type SessionNavigationItem,
} from './state/session';
import { formatDateTime, formatPlanWindow } from './utils/planFormatting';
import { formatApiErrorMessage } from './utils/apiErrors';
import {
  buildPlanDetailSearch,
  parsePlanDetailUrlState,
  type PlanDetailUrlState,
} from './utils/planDetailUrl';
import {
  buildPlanListSearch,
  parsePlanListUrlState,
} from './utils/planListUrl';
import { useHistoryRouter, type HistoryRouter } from './router/router';
import { buildPlanDetailPath, parsePlanRoute } from './router/planRoutes';

const { Content } = Layout;
const { Text } = Typography;

type FlattenedNavigationItem = { key: string; path: string };

const flattenNavigationItems = (items: SessionNavigationItem[]): FlattenedNavigationItem[] => {
  if (items.length === 0) {
    return [];
  }
  const flat: FlattenedNavigationItem[] = [];
  const visit = (list: SessionNavigationItem[]) => {
    list.forEach((item) => {
      flat.push({ key: item.key, path: item.path });
      if (item.children && item.children.length > 0) {
        visit(item.children);
      }
    });
  };
  visit(items);
  return flat;
};

type CredentialsState = {
  username: string;
  password: string;
};

type AppViewProps = {
  client: ApiClient;
  localization: LocalizationState;
  session: SessionController;
  planList: PlanListController;
  planDetail: PlanDetailController;
  router: HistoryRouter;
};

function AppView({ client, localization, session, planList, planDetail, router }: AppViewProps) {
  const { locale, translate, availableLocales, loading, setLocale } = localization;
  const {
    state: sessionState,
    login,
    logout,
    navigationMenu: sessionNavigationMenu,
    navigationItems: sessionNavigationItems,
    navigationPathMap: sessionNavigationPathMap,
    navigationPaths: sessionNavigationPaths,
    canAccessPath,
  } = session;
  const isAuthenticated = Boolean(sessionState.session);
  const { state: planState, refresh, changePage, changePageSize, restore: restorePlanList } = planList;
  const {
    state: planDetailState,
    selectPlan: selectPlanDetail,
    refresh: refreshPlanDetail,
    retain: retainPlanDetails,
    executeNodeAction,
    updateReminder: updatePlanReminder,
    setTimelineCategoryFilter,
  } = planDetail;
  const { location, navigate } = router;
  const planRoute = useMemo(() => parsePlanRoute(location.pathname), [location.pathname]);
  const authSectionRef = useRef<HTMLDivElement | null>(null);
  const initialUrlStateRef = useRef<PlanDetailUrlState | null>(null);
  if (initialUrlStateRef.current === null) {
    initialUrlStateRef.current = parsePlanDetailUrlState(location.search);
  }
  const initialUrlState = initialUrlStateRef.current;
  const planListUrlState = useMemo(() => parsePlanListUrlState(location.search), [location.search]);
  const previewPlanId = planRoute.type === 'detail' ? planRoute.planId : null;
  const [lastVisitedPlanId, setLastVisitedPlanId] = useState<string | null>(initialUrlState.planId);
  const [viewMode, setViewMode] = useState<PlanListViewMode>(
    planListUrlState.view ?? DEFAULT_PLAN_LIST_VIEW_MODE
  );
  const pendingTimelineCategoryRef = useRef<{ value: string | null; pending: boolean }>({
    value: initialUrlState.timelineCategory,
    pending: initialUrlState.hasTimelineCategory,
  });
  const suppressedAutoOpenRef = useRef(false);
  const planListSearchSyncSuppressedRef = useRef(false);
  const lastPlanListSearchRef = useRef<string | null>(null);
  const planRecordSignature = useMemo(
    () => planState.records.map((record) => record.id).join('|'),
    [planState.records]
  );
  const [credentials, setCredentials] = useState<CredentialsState>({
    username: '',
    password: '',
  });
  const [pingError, setPingError] = useState<ApiError | null>(null);
  const [ping, setPing] = useState<{ status: string } | null>(null);
  const describeRemoteError = useCallback(
    (error: ApiError | null) => formatApiErrorMessage(error, translate),
    [translate]
  );

  const queueTimelineCategory = useCallback((value: string | null, shouldApply: boolean) => {
    pendingTimelineCategoryRef.current = { value, pending: shouldApply };
  }, []);

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
    suppressedAutoOpenRef.current = false;
  }, [planRecordSignature]);

  useEffect(() => {
    if (!sessionState.session) {
      return;
    }
    const { filters: urlFilters, page: urlPage, pageSize: urlPageSize } = planListUrlState;
    if (
      arePlanListFiltersEqual(planState.filters, urlFilters) &&
      planState.pagination.page === urlPage &&
      planState.pagination.pageSize === urlPageSize
    ) {
      return;
    }
    planListSearchSyncSuppressedRef.current = true;
    void restorePlanList({
      filters: urlFilters,
      page: urlPage,
      pageSize: urlPageSize,
    });
  }, [
    sessionState.session,
    planListUrlState,
    restorePlanList,
    planState.filters,
    planState.pagination.page,
    planState.pagination.pageSize,
  ]);

  useEffect(() => {
    if (!sessionState.session) {
      lastPlanListSearchRef.current = location.search;
      return;
    }
    if (planListSearchSyncSuppressedRef.current) {
      planListSearchSyncSuppressedRef.current = false;
      lastPlanListSearchRef.current = location.search;
      return;
    }
    const nextSearch = buildPlanListSearch(location.search, {
      filters: planState.filters,
      page: planState.pagination.page,
      pageSize: planState.pagination.pageSize,
      view: viewMode,
    });
    if (nextSearch === location.search) {
      lastPlanListSearchRef.current = nextSearch;
      return;
    }
    if (lastPlanListSearchRef.current === nextSearch) {
      return;
    }
    lastPlanListSearchRef.current = nextSearch;
    navigate({ search: nextSearch }, { replace: true, preserveHash: true });
  }, [
    sessionState.session,
    planState.filters,
    planState.pagination.page,
    planState.pagination.pageSize,
    location.search,
    viewMode,
    navigate,
  ]);

  useEffect(() => {
    setViewMode((current) => (current === planListUrlState.view ? current : planListUrlState.view));
  }, [planListUrlState.view]);

  useEffect(() => {
    if (sessionState.session) {
      return;
    }
    const nextSearch = buildPlanListSearch(location.search, { view: viewMode });
    if (nextSearch === location.search) {
      return;
    }
    navigate({ search: nextSearch }, { replace: true, preserveHash: true });
  }, [sessionState.session, location.search, viewMode, navigate]);

  useEffect(() => {
    if (!previewPlanId) {
      return;
    }
    if (!planState.recordIndex[previewPlanId]) {
      return;
    }
    setLastVisitedPlanId(previewPlanId);
  }, [previewPlanId, planState.recordIndex]);

  useEffect(() => {
    const urlState = parsePlanDetailUrlState(location.search);
    if (!urlState.hasTimelineCategory) {
      pendingTimelineCategoryRef.current = { value: null, pending: false };
      setTimelineCategoryFilter(null);
      return;
    }
    queueTimelineCategory(urlState.timelineCategory, true);
  }, [location.search, queueTimelineCategory, setTimelineCategoryFilter]);

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
  const planDetailErrorDetail = planDetailState.activePlanId
    ? describeRemoteError(planDetailState.error)
    : null;
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

  useEffect(() => {
    retainPlanDetails(planState.records.map((record) => record.id));
  }, [planState.records, retainPlanDetails]);

  useEffect(() => {
    void selectPlanDetail(previewPlanId);
  }, [previewPlanId, selectPlanDetail]);

  useEffect(() => {
    if (!planDetailState.activePlanId) {
      return;
    }
    if (planDetailState.activePlanId !== previewPlanId) {
      return;
    }
    const snapshot = pendingTimelineCategoryRef.current;
    if (!snapshot || !snapshot.pending) {
      return;
    }
    setTimelineCategoryFilter(snapshot.value);
    pendingTimelineCategoryRef.current = { value: snapshot.value, pending: false };
  }, [planDetailState.activePlanId, previewPlanId, setTimelineCategoryFilter]);

  useEffect(() => {
    const activeTimelineCategory =
      planDetailState.activePlanId && planDetailState.activePlanId === previewPlanId
        ? planDetailState.filters.timeline.activeCategory
        : null;
    const nextSearch = buildPlanDetailSearch(location.search, {
      planId: previewPlanId,
      timelineCategory: activeTimelineCategory,
    });
    if (nextSearch === location.search) {
      return;
    }
    navigate({ search: nextSearch }, { replace: true, preserveHash: true });
  }, [
    planDetailState.activePlanId,
    planDetailState.filters.timeline.activeCategory,
    previewPlanId,
    location.search,
    navigate,
  ]);

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

  const navigationState = sessionState.navigation;
  const navigationLoading = navigationState.loading;
  const navigationError = useMemo(() => {
    if (!sessionState.session) {
      return null;
    }
    return navigationState.error;
  }, [navigationState.error, sessionState.session]);
  const navigationUnauthorized = navigationState.unauthorized;
  const navigationPathCount = sessionNavigationPaths.length;

  const navigationErrorLabel = useMemo(() => {
    if (!navigationError) {
      return null;
    }
    if (navigationError.type === 'network') {
      return translate('backendErrorNetwork');
    }
    if (navigationError.type === 'status') {
      if (navigationError.status === 403 || navigationError.status === 401) {
        return null;
      }
      return translate('backendErrorStatus', { status: navigationError.status });
    }
    return translate('backendErrorNetwork');
  }, [navigationError, translate]);
  const normalizePathname = useCallback((pathname: string) => {
    if (!pathname) {
      return '/';
    }
    return pathname.startsWith('/') ? pathname : `/${pathname}`;
  }, []);

  const navigationPathMap = useMemo(() => {
    const map = new Map<string, string>();
    Object.entries(sessionNavigationPathMap).forEach(([key, path]) => {
      map.set(key, normalizePathname(path));
    });
    return map;
  }, [sessionNavigationPathMap, normalizePathname]);

  const planViewOptions = useMemo(
    () =>
      [
        { value: 'table' as PlanListViewMode, label: translate('planSectionTitle') },
        { value: 'customer' as PlanListViewMode, label: translate('planDetailCustomerLabel') },
        { value: 'calendar' as PlanListViewMode, label: translate('planDetailTimelineTitle') },
      ] satisfies Array<{ value: PlanListViewMode; label: string }>,
    [translate]
  );

  const headerMenuItems = useMemo(() => {
    type HeaderMenuItem = {
      key: string;
      label: string;
      roles: string[];
      disabled?: boolean;
      children?: HeaderMenuItem[];
    };

    const mapMenuItems = (items: typeof sessionNavigationMenu): HeaderMenuItem[] =>
      items.map((item) => ({
        key: item.key,
        label: translate(item.labelKey),
        roles: item.roles,
        children: item.children ? mapMenuItems(item.children) : undefined,
      }));

    return mapMenuItems(sessionNavigationMenu);
  }, [sessionNavigationMenu, translate]);

  const flattenedNavigationItems = useMemo(
    () => flattenNavigationItems(sessionNavigationItems),
    [sessionNavigationItems]
  );

  const sessionUserMenuItems = useMemo<MenuProps['items']>(() => {
    return sessionState.userMenu.items.map((item) => {
      if ('type' in item) {
        return { type: 'divider' as const, key: item.key };
      }
      return {
        key: item.key,
        label: translate(item.labelKey, item.labelValues),
        disabled: item.disabled,
      };
    });
  }, [sessionState.userMenu.items, translate]);

  const activeMenuKey = useMemo(() => {
    if (flattenedNavigationItems.length === 0) {
      return null;
    }
    const currentPath = normalizePathname(location.pathname);
    let matchedKey: string | null = null;
    let matchedLength = -1;
    for (const item of flattenedNavigationItems) {
      const candidate = normalizePathname(item.path);
      if (currentPath === candidate || currentPath.startsWith(`${candidate}/`)) {
        if (candidate.length > matchedLength) {
          matchedKey = item.key;
          matchedLength = candidate.length;
        }
      }
    }
    return matchedKey ?? flattenedNavigationItems[0]?.key ?? null;
  }, [flattenedNavigationItems, location.pathname, normalizePathname]);

  const handleMenuClick = useCallback(
    ({ key }: { key: string }) => {
      const targetPath = navigationPathMap.get(key);
      if (!targetPath) {
        return;
      }
      const normalizedTarget = normalizePathname(targetPath);
      if (normalizePathname(location.pathname) === normalizedTarget) {
        return;
      }
      navigate({ pathname: normalizedTarget });
    },
    [location.pathname, navigate, navigationPathMap, normalizePathname]
  );

  const menuSelectedKeys = activeMenuKey ? [activeMenuKey] : [];

  const isRouteForbidden = useMemo(() => {
    if (!sessionState.session) {
      return false;
    }
    if (navigationLoading) {
      return false;
    }
    if (navigationPathCount === 0) {
      return false;
    }
    return !canAccessPath(location.pathname);
  }, [
    canAccessPath,
    location.pathname,
    navigationLoading,
    navigationPathCount,
    sessionState.session,
  ]);

  useEffect(() => {
    if (!sessionState.session) {
      if (previewPlanId) {
        navigate({ pathname: '/' }, { replace: true, preserveSearch: true, preserveHash: true });
      }
      return;
    }
    if (isRouteForbidden) {
      return;
    }
    if (planState.records.length === 0) {
      if (previewPlanId) {
        navigate({ pathname: '/' }, { replace: true, preserveSearch: true, preserveHash: true });
      }
      return;
    }
    if (previewPlanId && planState.recordIndex[previewPlanId]) {
      return;
    }
    if (suppressedAutoOpenRef.current) {
      return;
    }
    const fromHistory =
      lastVisitedPlanId && planState.recordIndex[lastVisitedPlanId] ? lastVisitedPlanId : null;
    const fallbackPlanId = fromHistory ?? planState.records[0]?.id ?? null;
    if (!fallbackPlanId) {
      return;
    }
    if (previewPlanId === fallbackPlanId) {
      return;
    }
    const shouldReplace = planRoute.type !== 'detail';
    navigate(
      { pathname: buildPlanDetailPath(fallbackPlanId) },
      { replace: shouldReplace, preserveSearch: true, preserveHash: true }
    );
  }, [
    sessionState.session,
    isRouteForbidden,
    planState.records,
    planState.recordIndex,
    previewPlanId,
    lastVisitedPlanId,
    planRoute.type,
    navigate,
  ]);

  const handleBrandNavigateHome = useCallback(() => {
    navigate({ pathname: '/' });
  }, [navigate]);

  const userDisplayName = sessionState.session?.displayName ?? translate('navUserGuest');
  const userInitial = useMemo(() => {
    if (!sessionState.session) {
      return '•';
    }
    const trimmed = sessionState.session.displayName.trim();
    if (!trimmed) {
      return '•';
    }
    return trimmed.charAt(0).toUpperCase();
  }, [sessionState.session]);

  const handleUserMenuClick = useCallback(
    ({ key }: { key: string }) => {
      if (key === 'logout') {
        logout();
      }
    },
    [logout]
  );

  const handleLoginShortcut = useCallback(() => {
    if (authSectionRef.current) {
      authSectionRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const isForbiddenRoute = Boolean(
    sessionState.session && (navigationState.forbidden || isRouteForbidden)
  );
  const showForbiddenResult = isForbiddenRoute;

  const handleForbiddenNavigateHome = useCallback(() => {
    navigate({ pathname: '/' }, { preserveHash: true });
  }, [navigate]);

  return (
    <Layout className="app-layout">
      <HeaderNav
        title={translate('appTitle')}
        subtitle={translate('appDescription')}
        localeLabel={translate('localeLabel')}
        loginLabel={translate('navLogin')}
        localeValue={locale}
        localeOptions={localeOptions}
        localeLoading={loading}
        onLocaleChange={(value) => setLocale(value as Locale)}
        brandHref="/"
        onBrandClick={handleBrandNavigateHome}
        menuItems={headerMenuItems}
        menuSelectedKeys={menuSelectedKeys}
        onMenuClick={handleMenuClick}
        navigationErrorLabel={navigationErrorLabel}
        navigationLoading={navigationLoading}
        isAuthenticated={isAuthenticated}
        permissions={sessionState.permissions}
        isForbidden={isForbiddenRoute}
        isUnauthorized={navigationUnauthorized}
        forbiddenLabel={translate('navAccessDeniedTitle')}
        unauthorizedLabel={translate('planLoginRequired')}
        guestNoticeLabel={translate('navUserGuest')}
        userInitial={userInitial}
        userDisplayName={userDisplayName}
        userMenuItems={sessionUserMenuItems}
        onUserMenuClick={handleUserMenuClick}
        onLoginClick={handleLoginShortcut}
      />
      <Content className="app-content">
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          {showForbiddenResult ? (
            <Result
              className="forbidden-result"
              status="403"
              title={translate('navAccessDeniedTitle')}
              subTitle={translate('navAccessDeniedDescription')}
              extra={
                <Button type="primary" onClick={handleForbiddenNavigateHome}>
                  {translate('navMenuOverview')}
                </Button>
              }
            />
          ) : (
            <>
              <Card
                title={translate('backendStatus')}
                bordered={false}
                className="card-block status-card"
              >
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

              <div ref={authSectionRef} style={{ width: '100%' }}>
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
              </div>

              <PlanListBoard
                translate={translate}
                locale={locale}
                sessionActive={Boolean(sessionState.session)}
                planState={planState}
                planColumns={planColumns}
                planErrorDetail={planErrorDetail}
                lastUpdatedLabel={lastUpdatedLabel}
                onRefreshList={() => {
                  void refresh();
                }}
                isRefreshDisabled={!sessionState.session}
                onApplyFilters={(filters) => {
                  void planList.applyFilters(filters);
                }}
                onResetFilters={() => {
                  void planList.resetFilters();
                }}
                onChangePage={(page) => {
                  void changePage(page);
                }}
                onChangePageSize={(size) => {
                  void changePageSize(size);
                }}
                selectedPlanId={previewPlanId}
                previewPlan={previewPlan}
                planDetailState={planDetailState}
                planDetailErrorDetail={planDetailErrorDetail}
                onRefreshDetail={() => {
                  void refreshPlanDetail();
                }}
                onClosePreview={() => {
                  suppressedAutoOpenRef.current = true;
                  navigate({ pathname: '/' }, { preserveSearch: true, preserveHash: true });
                }}
                onSelectPlan={(planId) => {
                  suppressedAutoOpenRef.current = false;
                  setLastVisitedPlanId(planId);
                  navigate(
                    { pathname: buildPlanDetailPath(planId) },
                    { preserveSearch: true, preserveHash: true }
                  );
                }}
                onExecuteNodeAction={executeNodeAction}
                onUpdateReminder={updatePlanReminder}
                currentUserName={sessionState.session?.displayName ?? null}
                onTimelineCategoryChange={setTimelineCategoryFilter}
                viewMode={viewMode}
                onChangeViewMode={(mode) => {
                  setViewMode(mode);
                }}
              />
            </>
          )}
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
  const planDetail = usePlanDetailController(client, session.state.session);
  const router = useHistoryRouter();

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
        planDetail={planDetail}
        router={router}
      />
    </ConfigProvider>
  );
}
export default App;
