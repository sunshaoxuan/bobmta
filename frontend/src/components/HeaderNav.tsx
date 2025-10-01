import React from '../../vendor/react/index.js';
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

type HeaderNavProps = {
  title: string;
  subtitle: string;
  localeLabel: string;
  loginLabel: string;
  localeValue: string;
  localeOptions: Array<{ label: string; value: string }>;
  localeLoading: boolean;
  onLocaleChange: (value: string) => void;
  menuItems: MenuProps['items'];
  menuSelectedKeys: MenuProps['selectedKeys'];
  onMenuClick: MenuProps['onClick'];
  navigationErrorLabel: string | null;
  isAuthenticated: boolean;
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
  localeValue,
  localeOptions,
  localeLoading,
  onLocaleChange,
  menuItems,
  menuSelectedKeys,
  onMenuClick,
  navigationErrorLabel,
  isAuthenticated,
  userInitial,
  userDisplayName,
  userMenuItems,
  onUserMenuClick,
  onLoginClick,
}: HeaderNavProps) {
  const showMenu = isAuthenticated && Boolean(menuItems && menuItems.length > 0);

  return (
    <Header className="app-header">
      <div className="app-header-left">
        <div className="app-brand">
          <Title level={3} className="app-title">
            {title}
          </Title>
          <Paragraph className="app-subtitle">{subtitle}</Paragraph>
        </div>
        {showMenu && (
          <Menu
            mode="horizontal"
            className="app-header-menu"
            items={menuItems}
            selectedKeys={menuSelectedKeys}
            onClick={onMenuClick}
          />
        )}
        {isAuthenticated && navigationErrorLabel && (
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
          <Dropdown menu={{ items: userMenuItems, onClick: onUserMenuClick }} placement="bottomRight">
            <button type="button" className="user-dropdown-trigger">
              <span className="user-avatar">{userInitial}</span>
              <span className="user-name">{userDisplayName}</span>
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
