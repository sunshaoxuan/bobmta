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

export interface SpaceProps extends React.HTMLAttributes<HTMLDivElement> {
  direction?: 'horizontal' | 'vertical';
  size?: 'small' | 'middle' | 'large' | number;
  align?: 'start' | 'center' | 'end' | 'baseline' | 'stretch';
  wrap?: boolean;
}
export const Space: React.FC<SpaceProps>;

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

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  size?: 'small' | 'middle' | 'large';
  block?: boolean;
  onPressEnter?: React.KeyboardEventHandler<HTMLInputElement>;
}
export const Input: React.FC<InputProps> & {
  Password: React.FC<InputProps>;
};

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
}
export function Table<T>(props: TableProps<T>): React.ReactElement;

export default {
  ConfigProvider,
  Layout,
  Typography,
  Card,
  Space,
  Alert,
  Button,
  Input,
  Select,
  Table,
  Tag,
  Progress,
  Empty,
};
