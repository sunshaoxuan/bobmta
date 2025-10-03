import * as React from 'react';

export interface ConfigProviderProps {
  children?: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
  theme?: Record<string, unknown>;
}
export const ConfigProvider: React.FC<ConfigProviderProps>;

export interface LayoutProps extends React.HTMLAttributes<HTMLDivElement> {}
export interface LayoutHeaderProps extends React.HTMLAttributes<HTMLElement> {}
export interface LayoutContentProps extends React.HTMLAttributes<HTMLElement> {}
export const Layout: React.FC<LayoutProps> & {
  Header: React.FC<LayoutHeaderProps>;
  Content: React.FC<LayoutContentProps>;
};

export interface TypographyTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {
  level?: 1 | 2 | 3 | 4 | 5 | 6;
}
export interface TypographyParagraphProps extends React.HTMLAttributes<HTMLParagraphElement> {}
export interface TypographyTextProps extends React.HTMLAttributes<HTMLSpanElement> {
  type?: 'secondary' | 'warning' | 'danger' | 'success' | 'info';
  strong?: boolean;
  code?: boolean;
}
export const Typography: {
  Title: React.FC<TypographyTitleProps>;
  Paragraph: React.FC<TypographyParagraphProps>;
  Text: React.FC<TypographyTextProps>;
};

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  title?: React.ReactNode;
  extra?: React.ReactNode;
  bordered?: boolean;
}
export const Card: React.FC<CardProps>;

export interface ResultProps extends React.HTMLAttributes<HTMLDivElement> {
  status?: string | number;
  title?: React.ReactNode;
  subTitle?: React.ReactNode;
  extra?: React.ReactNode;
  icon?: React.ReactNode;
}
export const Result: React.FC<ResultProps>;

export interface SpaceProps extends React.HTMLAttributes<HTMLDivElement> {
  direction?: 'horizontal' | 'vertical';
  size?: 'small' | 'middle' | 'large' | number;
  align?: 'start' | 'center' | 'end' | 'baseline' | 'stretch';
  wrap?: boolean;
}
export const Space: React.FC<SpaceProps>;

export interface SpinProps extends React.HTMLAttributes<HTMLSpanElement> {
  size?: 'small' | 'middle' | 'large' | number;
}
export const Spin: React.FC<SpinProps>;

export type MenuDividerItem = { type: 'divider'; key?: string };
export type MenuOptionItem = {
  key: string;
  label: React.ReactNode;
  disabled?: boolean;
  onClick?: (event: React.MouseEvent<HTMLButtonElement>) => void;
};
export type MenuItem = MenuDividerItem | MenuOptionItem;
export interface MenuProps extends React.HTMLAttributes<HTMLUListElement> {
  items?: MenuItem[];
  selectedKeys?: string[];
  onClick?: (info: { key: string }) => void;
  mode?: 'horizontal' | 'vertical';
}
export const Menu: React.FC<MenuProps>;

export interface DropdownMenuProps {
  items?: MenuItem[];
  onClick?: (info: { key: string }) => void;
}
export interface DropdownProps extends React.HTMLAttributes<HTMLDivElement> {
  menu?: DropdownMenuProps;
  placement?: 'bottomLeft' | 'bottomRight';
  children: React.ReactElement;
}
export const Dropdown: React.FC<DropdownProps>;

export interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  type?: 'success' | 'info' | 'warning' | 'error';
  message?: React.ReactNode;
  showIcon?: boolean;
}
export const Alert: React.FC<AlertProps>;

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  type?: 'default' | 'primary' | 'link' | 'text';
  size?: 'small' | 'middle' | 'large';
  block?: boolean;
  loading?: boolean;
}
export const Button: React.FC<ButtonProps>;

export type SegmentedOption =
  | string
  | number
  | {
      label?: React.ReactNode;
      value: string | number;
      disabled?: boolean;
    };
export interface SegmentedProps extends React.HTMLAttributes<HTMLDivElement> {
  options?: SegmentedOption[];
  value?: string | number | null;
  defaultValue?: string | number | null;
  onChange?: (value: string | number | null) => void;
  size?: 'small' | 'middle' | 'large';
  block?: boolean;
}
export const Segmented: React.FC<SegmentedProps>;

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  size?: 'small' | 'middle' | 'large';
  block?: boolean;
  onPressEnter?: React.KeyboardEventHandler<HTMLInputElement>;
}
export const Input: React.FC<InputProps> & {
  Password: React.FC<InputProps>;
};

export interface DatePickerProps extends React.InputHTMLAttributes<HTMLInputElement> {
  value?: string | null;
}
export interface RangePickerProps {
  value?: [string | null, string | null];
  onChange?: (
    value: [string | null, string | null] | null,
    formatted: [string, string]
  ) => void;
  className?: string;
  style?: React.CSSProperties;
  placeholder?: [string, string];
}
export interface DatePickerComponent extends React.FC<DatePickerProps> {
  RangePicker: React.FC<RangePickerProps>;
}
export const DatePicker: DatePickerComponent;

export interface SelectOption {
  value: string;
  label: React.ReactNode;
}
export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  options?: SelectOption[];
  loading?: boolean;
  value?: string;
  onChange?: (value: string) => void;
}
export const Select: React.FC<SelectProps>;

export interface TagProps extends React.HTMLAttributes<HTMLSpanElement> {
  color?: string;
}
export const Tag: React.FC<TagProps>;

export interface ProgressProps extends React.HTMLAttributes<HTMLDivElement> {
  percent?: number;
  size?: 'small' | 'default';
}
export const Progress: React.FC<ProgressProps>;

export interface EmptyProps extends React.HTMLAttributes<HTMLDivElement> {
  description?: React.ReactNode;
}
export const Empty: React.FC<EmptyProps>;

export interface ListProps<T> extends React.HTMLAttributes<HTMLDivElement> {
  dataSource?: T[];
  renderItem?: (item: T, index: number) => React.ReactNode;
  header?: React.ReactNode;
  itemLayout?: 'horizontal' | 'vertical';
}
export interface ListItemProps extends React.HTMLAttributes<HTMLDivElement> {}
export interface ListComponent {
  <T>(props: ListProps<T>): React.ReactElement;
  Item: React.FC<ListItemProps>;
}
export const List: ListComponent;

export interface TabsItem {
  key: string;
  label: React.ReactNode;
  children?: React.ReactNode;
}
export interface TabsProps extends React.HTMLAttributes<HTMLDivElement> {
  items?: TabsItem[];
  activeKey?: string;
  onChange?: (key: string) => void;
}
export const Tabs: React.FC<TabsProps>;

export interface TreeNode {
  key: string;
  title: React.ReactNode;
  children?: TreeNode[];
}
export interface TreeProps extends React.HTMLAttributes<HTMLDivElement> {
  treeData?: TreeNode[];
  defaultExpandAll?: boolean;
  showLine?: boolean;
  selectable?: boolean;
}
export const Tree: React.FC<TreeProps>;

export type CalendarMode = 'month' | 'year';
export interface CalendarValue {
  format: (pattern: string) => string;
  toDate: () => Date;
}
export interface CalendarCellInfo {
  type: 'date' | 'month';
  originNode: React.ReactNode;
}
export interface CalendarProps extends React.HTMLAttributes<HTMLDivElement> {
  mode?: CalendarMode;
  fullscreen?: boolean;
  onPanelChange?: (value: CalendarValue, mode: CalendarMode) => void;
  cellRender?: (value: CalendarValue, info: CalendarCellInfo) => React.ReactNode;
}
export const Calendar: React.FC<CalendarProps>;

export interface TableColumnType<T> {
  title?: React.ReactNode;
  dataIndex?: keyof T | string;
  key?: string;
  width?: number;
  ellipsis?: boolean;
  align?: 'left' | 'center' | 'right';
  render?: (value: any, record: T, index: number) => React.ReactNode;
}
export type TableColumnsType<T> = TableColumnType<T>[];

export interface TableLocale {
  emptyText?: React.ReactNode;
}
export interface TableScrollOptions {
  x?: boolean | string | number;
}
export interface TableLoadingConfig {
  spinning?: boolean;
  tip?: React.ReactNode;
}
export interface TableProps<T> {
  columns?: TableColumnsType<T>;
  dataSource?: T[];
  rowKey?: keyof T | ((record: T, index: number) => React.Key);
  className?: string;
  style?: React.CSSProperties;
  loading?: boolean | TableLoadingConfig;
  locale?: TableLocale;
  scroll?: TableScrollOptions;
  pagination?: false | Record<string, unknown>;
  rowClassName?: string | ((record: T, index: number) => string);
  onRow?: (record: T, index: number) => Partial<React.HTMLAttributes<HTMLTableRowElement>>;
}
export function Table<T>(props: TableProps<T>): React.ReactElement;

export interface PaginationProps {
  current?: number;
  pageSize?: number;
  total?: number;
  showSizeChanger?: boolean;
  pageSizeOptions?: string[];
  onChange?: (page: number, pageSize: number) => void;
  onShowSizeChange?: (page: number, pageSize: number) => void;
  className?: string;
  style?: React.CSSProperties;
}
export const Pagination: React.FC<PaginationProps>;

export default {
  ConfigProvider,
  Layout,
  Typography,
  Card,
  Result,
  Space,
  Alert,
  Button,
  Segmented,
  Input,
  Menu,
  Dropdown,
  DatePicker,
  Select,
  Table,
  Tag,
  Progress,
  Empty,
  Pagination,
  List,
  Tabs,
  Tree,
  Calendar,
};
