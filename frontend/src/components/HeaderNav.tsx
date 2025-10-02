import React, { useCallback, useMemo } from '../../vendor/react/index.js';
import {
  Button,
  Dropdown,
  Layout,
  Menu,
  Select,
  Space,
  Tag,
  Typography,
  type MenuProps,
} from '../../vendor/antd/index.js';
import type { SessionPermissionsState } from '../state/session';

const { Header } = Layout;
const { Title, Paragraph, Text } = Typography;

type HeaderNavMenuItem = {
  key: string;
  label: string;
  roles?: string[];
  disabled?: boolean;
  children?: HeaderNavMenuItem[];
};

type HeaderNavProps = {
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
  userInitial: string;
  userDisplayName: string;
  userMenuItems: MenuProps['items'];
  onUserMenuClick: MenuProps['onClick'];
  onLoginClick: () => void;
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
  const normalizedUserRoles = permissions.normalizedRoles;

  const filterMenuItemsByRoles = useCallback(
    (items: HeaderNavMenuItem[]): HeaderNavMenuItem[] => {
      if (items.length === 0) {
        return [];
      }
      const allowedRoles = new Set(normalizedUserRoles);
      const mapItem = (item: HeaderNavMenuItem): HeaderNavMenuItem | null => {
        const children = item.children ? filterMenuItemsByRoles(item.children) : [];
        const normalizedRoles = (item.roles ?? []).map((role) => role.trim().toUpperCase());
        const allowed =
          normalizedRoles.length === 0 || normalizedRoles.some((role) => allowedRoles.has(role));
        if (!allowed && children.length === 0) {
          return null;
        }
        const shouldDisable = !allowed && children.length > 0;
        return {
          ...item,
          disabled: shouldDisable ? true : item.disabled,
          children: children.length > 0 ? children : undefined,
        };
      };

      return items
        .map((item) => mapItem(item))
        .filter((item): item is HeaderNavMenuItem => Boolean(item));
    },
    [normalizedUserRoles]
  );

  const visibleMenuItems = useMemo<HeaderNavMenuItem[]>(() => {
    if (!menuItems || menuItems.length === 0) {
      return [];
    }
    return filterMenuItemsByRoles(menuItems);
  }, [filterMenuItemsByRoles, menuItems]);

  const collectVisibleMenuKeys = useCallback((items: HeaderNavMenuItem[], keys: Set<string>) => {
    items.forEach((item) => {
      keys.add(item.key);
      if (item.children) {
        collectVisibleMenuKeys(item.children, keys);
      }
    });
    return keys;
  }, []);

  const visibleMenuKeySet = useMemo(() => collectVisibleMenuKeys(visibleMenuItems, new Set<string>()), [collectVisibleMenuKeys, visibleMenuItems]);

  const antMenuItems = useMemo<MenuProps['items']>(() => {
    const mapMenu = (items: HeaderNavMenuItem[]): NonNullable<MenuProps['items']> =>
      items.map((item) => ({
        key: item.key,
        label: item.label,
        disabled: item.disabled,
        children: item.children ? mapMenu(item.children) : undefined,
      }));
    return mapMenu(visibleMenuItems);
  }, [visibleMenuItems]);

  const selectedMenuKeys = useMemo<MenuProps['selectedKeys']>(
    () => {
      if (!menuSelectedKeys || menuSelectedKeys.length === 0) {
        return [];
      }
      return menuSelectedKeys.filter((key) => visibleMenuKeySet.has(key));
    },
    [menuSelectedKeys, visibleMenuKeySet]
  );

  const showMenu = isAuthenticated && visibleMenuItems.length > 0;
  const primaryRole = isAuthenticated && normalizedUserRoles.length > 0 ? normalizedUserRoles[0] : null;
  const secondaryRoleCount = primaryRole ? Math.max(normalizedUserRoles.length - 1, 0) : 0;
  const roleBadgeLabel = primaryRole
    ? `${primaryRole}${secondaryRoleCount > 0 ? ` +${secondaryRoleCount}` : ''}`
    : null;

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
      if (onMenuClick) {
        onMenuClick(info);
      }
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
            selectedKeys={selectedMenuKeys}
            onClick={handleMenuClick}
          />
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
                {userInitial}
              </span>
              <span className="user-bio">
                <span className="user-name">{userDisplayName}</span>
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
