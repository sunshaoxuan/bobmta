import React, { useCallback, useMemo } from 'react';
import { Button, Dropdown, Layout, Menu, Select, Space, Spin, Tag, Typography } from 'antd';
import type { MenuProps } from 'antd';
import type { SessionPermissionsState } from '../state/session';

const { Header } = Layout;
const { Title, Paragraph, Text } = Typography;

export type HeaderNavMenuItem = {
  key: string;
  label: string;
  roles?: string[];
  disabled?: boolean;
  children?: HeaderNavMenuItem[];
};

export type HeaderNavProps = {
  title: string;
  subtitle: string;
  localeLabel: string;
  loginLabel: string;
  brandHref?: string;
  onBrandClick?: () => void;
  localeValue: string;
  localeOptions: Array<{ label: string; value: string }>;
  localeLoading: boolean;
  onLocaleChange: (value: string) => void;
  menuItems: HeaderNavMenuItem[];
  menuSelectedKeys?: MenuProps['selectedKeys'];
  onMenuClick?: MenuProps['onClick'];
  navigationErrorLabel: string | null;
  navigationLoading: boolean;
  isAuthenticated: boolean;
  permissions: SessionPermissionsState;
  isForbidden: boolean;
  isUnauthorized: boolean;
  forbiddenLabel: string;
  unauthorizedLabel: string;
  guestNoticeLabel: string;
  userInitial?: string | null;
  userDisplayName?: string | null;
  userMenuItems: MenuProps['items'];
  onUserMenuClick: MenuProps['onClick'];
  onLoginClick: () => void;
};

const normalizeRoles = (roles: string[] = []): string[] =>
  roles
    .map((role) => role.trim().toUpperCase())
    .filter((role) => role.length > 0);

const mapMenuItems = (
  items: HeaderNavMenuItem[],
  allowedRoles: Set<string>
): HeaderNavMenuItem[] => {
  const visit = (item: HeaderNavMenuItem): HeaderNavMenuItem | null => {
    const normalizedRoles = normalizeRoles(item.roles ?? []);
    const children = item.children ? mapMenuItems(item.children, allowedRoles) : [];
    const isAllowed =
      normalizedRoles.length === 0 || normalizedRoles.some((role) => allowedRoles.has(role));

    if (!isAllowed && children.length === 0) {
      return null;
    }

    const shouldDisable = !isAllowed && children.length > 0;

    return {
      ...item,
      disabled: shouldDisable ? true : item.disabled,
      children: children.length > 0 ? children : undefined,
    };
  };

  return items
    .map((item) => visit(item))
    .filter((item): item is HeaderNavMenuItem => Boolean(item));
};

const toAntdMenuItems = (items: HeaderNavMenuItem[]): NonNullable<MenuProps['items']> =>
  items.map((item) => ({
    key: item.key,
    label: item.label,
    disabled: item.disabled,
    children: item.children ? toAntdMenuItems(item.children) : undefined,
  }));

const collectVisibleKeys = (items: HeaderNavMenuItem[], bucket: Set<string>): Set<string> => {
  items.forEach((item) => {
    bucket.add(item.key);
    if (item.children) {
      collectVisibleKeys(item.children, bucket);
    }
  });
  return bucket;
};

export function HeaderNav({
  title,
  subtitle,
  localeLabel,
  loginLabel,
  brandHref = '/',
  onBrandClick,
  localeValue,
  localeOptions,
  localeLoading,
  onLocaleChange,
  menuItems,
  menuSelectedKeys,
  onMenuClick,
  navigationErrorLabel,
  navigationLoading,
  isAuthenticated,
  permissions,
  isForbidden,
  isUnauthorized,
  forbiddenLabel,
  unauthorizedLabel,
  guestNoticeLabel,
  userInitial,
  userDisplayName,
  userMenuItems,
  onUserMenuClick,
  onLoginClick,
}: HeaderNavProps) {
  const allowedRoles = useMemo(
    () => new Set(permissions.normalizedRoles ?? []),
    [permissions.normalizedRoles]
  );

  const filteredMenuItems = useMemo(() => {
    if (!isAuthenticated) {
      return [];
    }
    if (!menuItems || menuItems.length === 0) {
      return [];
    }
    return mapMenuItems(menuItems, allowedRoles);
  }, [allowedRoles, isAuthenticated, menuItems]);

  const visibleKeySet = useMemo(
    () => collectVisibleKeys(filteredMenuItems, new Set<string>()),
    [filteredMenuItems]
  );

  const antMenuItems = useMemo<MenuProps['items']>(() => {
    if (filteredMenuItems.length === 0) {
      return [];
    }
    return toAntdMenuItems(filteredMenuItems);
  }, [filteredMenuItems]);

  const selectedKeys = useMemo<MenuProps['selectedKeys']>(() => {
    if (!menuSelectedKeys || menuSelectedKeys.length === 0) {
      return [];
    }
    return menuSelectedKeys.filter((key) => visibleKeySet.has(key));
  }, [menuSelectedKeys, visibleKeySet]);

  const showMenu = isAuthenticated && filteredMenuItems.length > 0;
  const normalizedUserRoles = permissions.normalizedRoles ?? [];
  const primaryRole = normalizedUserRoles.length > 0 ? normalizedUserRoles[0] : null;
  const extraRoleCount = primaryRole ? Math.max(normalizedUserRoles.length - 1, 0) : 0;
  const roleBadgeLabel = primaryRole
    ? `${primaryRole}${extraRoleCount > 0 ? ` +${extraRoleCount}` : ''}`
    : null;
  const safeUserInitial = (userInitial ?? '').trim() || '•';
  const safeUserDisplayName = (userDisplayName ?? '').trim() || loginLabel;

  const handleBrandClick = useCallback(
    (event: Event) => {
      if (!onBrandClick) {
        return;
      }
      event.preventDefault();
      onBrandClick();
    },
    [onBrandClick]
  );

  const handleMenuClick = useCallback<NonNullable<MenuProps['onClick']>>(
    (info) => {
      if (navigationLoading) {
        return;
      }
      onMenuClick?.(info);
    },
    [navigationLoading, onMenuClick]
  );

  const menuClassName = useMemo(() => {
    const classes = ['app-header-menu'];
    if (navigationLoading) {
      classes.push('app-header-menu-disabled');
    }
    return classes.join(' ');
  }, [navigationLoading]);

  const showGuestNotice = !isAuthenticated && guestNoticeLabel.trim().length > 0;
  const showForbiddenNotice = isForbidden && forbiddenLabel.trim().length > 0;
  const showUnauthorizedNotice = isUnauthorized && unauthorizedLabel.trim().length > 0;
  const showNavigationError = Boolean(navigationErrorLabel) && isAuthenticated;

  return (
    <Header className="app-header" role="banner">
      <div className="app-header-left">
        <a className="app-brand" href={brandHref} aria-label={title} onClick={handleBrandClick}>
          <div className="app-brand-mark" aria-hidden="true">
            <span className="app-brand-logo">BOB</span>
          </div>
          <div className="app-brand-text">
            <Title level={3} className="app-title">
              {title}
            </Title>
            <Paragraph className="app-subtitle">{subtitle}</Paragraph>
          </div>
        </a>
        {showMenu && (
          <Menu
            mode="horizontal"
            className={menuClassName}
            items={antMenuItems}
            selectedKeys={selectedKeys}
            onClick={handleMenuClick}
          />
        )}
        <div className="app-header-status" aria-live="polite">
          {navigationLoading && isAuthenticated && (
            <Spin size="small" className="nav-status-spinner" aria-hidden="true" />
          )}
          {showForbiddenNotice && (
            <Tag color="volcano" className="nav-status-tag nav-status-forbidden">
              {forbiddenLabel}
            </Tag>
          )}
          {!showForbiddenNotice && showUnauthorizedNotice && (
            <Tag color="gold" className="nav-status-tag nav-status-unauthorized">
              {unauthorizedLabel}
            </Tag>
          )}
          {!showMenu && !showForbiddenNotice && showGuestNotice && (
            <Tag color="geekblue" className="nav-status-tag nav-status-guest">
              {guestNoticeLabel}
            </Tag>
          )}
          {showNavigationError && (
            <Tag color="volcano" className="nav-error-badge">
              {navigationErrorLabel}
            </Tag>
          )}
        </div>
      </div>
      <div className="app-header-right">
        <Space align="center" size="middle" className="locale-switcher">
          <Text strong>{localeLabel}</Text>
          <Select
            className="locale-select"
            value={localeValue}
            onChange={(value: string) => onLocaleChange(value)}
            loading={localeLoading}
            options={localeOptions}
          />
        </Space>
        {isAuthenticated ? (
          <Dropdown
            menu={{ items: userMenuItems, onClick: onUserMenuClick }}
            placement="bottomRight"
            trigger={['click']}
          >
            <button
              type="button"
              className="user-dropdown-trigger"
              aria-haspopup="menu"
              aria-expanded="false"
            >
              <span className="user-avatar" aria-hidden="true">
                {safeUserInitial}
              </span>
              <span className="user-bio">
                <span className="user-name">{safeUserDisplayName}</span>
                {roleBadgeLabel && <span className="user-role-badge">{roleBadgeLabel}</span>}
              </span>
            </button>
          </Dropdown>
        ) : (
          <Button
            type="primary"
            className="header-login-button"
            onClick={onLoginClick}
            aria-label={loginLabel}
          >
            {loginLabel}
          </Button>
        )}
      </div>
    </Header>
  );
}
