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

const { Header } = Layout;
const { Title, Paragraph, Text } = Typography;

type HeaderNavMenuItem = {
  key: string;
  label: string;
  roles?: string[];
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
  onMenuClick: MenuProps['onClick'];
  navigationErrorLabel: string | null;
  isForbidden: boolean;
  isAuthenticated: boolean;
  userInitial: string;
  userDisplayName: string;
  userRoles: string[];
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
  isForbidden,
  isAuthenticated,
  userInitial,
  userDisplayName,
  userRoles,
  userMenuItems,
  onUserMenuClick,
  onLoginClick,
}: HeaderNavProps) {
  const normalizedUserRoles = useMemo(
    () =>
      Array.from(
        new Set(
          (userRoles ?? [])
            .map((role) => role.trim().toUpperCase())
            .filter((role) => role.length > 0)
        )
      ),
    [userRoles]
  );

  const visibleMenuItems = useMemo(() => {
    if (!menuItems || menuItems.length === 0) {
      return [] as HeaderNavMenuItem[];
    }
    if (normalizedUserRoles.length === 0) {
      return menuItems.filter((item) => !item.roles || item.roles.length === 0);
    }
    const allowedRoles = new Set(normalizedUserRoles);
    return menuItems.filter((item) => {
      if (!item.roles || item.roles.length === 0) {
        return true;
      }
      return item.roles.some((role) => allowedRoles.has(role.toUpperCase()));
    });
  }, [menuItems, normalizedUserRoles]);

  const antMenuItems = useMemo(
    () =>
      visibleMenuItems.map((item) => ({
        key: item.key,
        label: item.label,
      })),
    [visibleMenuItems]
  ) as NonNullable<MenuProps['items']>;

  const selectedMenuKeys = useMemo<MenuProps['selectedKeys']>(
    () => {
      if (!menuSelectedKeys || menuSelectedKeys.length === 0) {
        return [];
      }
      const visibleKeys = new Set(visibleMenuItems.map((item) => item.key));
      return menuSelectedKeys.filter((key) => visibleKeys.has(key));
    },
    [menuSelectedKeys, visibleMenuItems]
  );

  const showMenu = isAuthenticated && antMenuItems.length > 0;
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

  const statusTag = useMemo(() => {
    if (!navigationErrorLabel) {
      return null;
    }
    const classNames = ['nav-status-badge'];
    if (isForbidden) {
      classNames.push('nav-status-forbidden');
    } else {
      classNames.push('nav-status-error');
    }
    return (
      <Tag
        color={isForbidden ? 'gold' : 'volcano'}
        className={classNames.join(' ')}
        role="status"
        aria-live="polite"
      >
        {navigationErrorLabel}
      </Tag>
    );
  }, [isForbidden, navigationErrorLabel]);

  return (
    <Header className="app-header">
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
            className="app-header-menu"
            items={antMenuItems}
            selectedKeys={selectedMenuKeys}
            onClick={onMenuClick}
          />
        )}
        {isAuthenticated && statusTag}
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
          <Dropdown menu={{ items: userMenuItems, onClick: onUserMenuClick }} placement="bottomRight">
            <button type="button" className="user-dropdown-trigger">
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
          <Button type="primary" className="header-login-button" onClick={onLoginClick}>
            {loginLabel}
          </Button>
        )}
      </div>
    </Header>
  );
}
