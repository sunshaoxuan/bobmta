import React, { forwardRef } from 'react';

const classNames = (...tokens) => tokens.filter(Boolean).join(' ');

export const ConfigProvider = ({ children, className = '', style }) => (
  <div className={classNames('antd-config-provider', className)} style={style}>
    {children}
  </div>
);

const LayoutRoot = ({ children, className = '', style }) => (
  <div className={classNames('antd-layout', className)} style={style}>
    {children}
  </div>
);

const LayoutHeader = ({ children, className = '', style }) => (
  <header className={classNames('antd-layout-header', className)} style={style}>
    {children}
  </header>
);

const LayoutContent = ({ children, className = '', style }) => (
  <main className={classNames('antd-layout-content', className)} style={style}>
    {children}
  </main>
);

export const Layout = Object.assign(LayoutRoot, {
  Header: LayoutHeader,
  Content: LayoutContent,
});

const Title = ({ level = 1, children, className = '', style }) => {
  const Component = `h${Math.min(6, Math.max(1, level))}`;
  return (
    <Component className={classNames('antd-typography-title', className)} style={style}>
      {children}
    </Component>
  );
};

const Paragraph = ({ children, className = '', style }) => (
  <p className={classNames('antd-typography-paragraph', className)} style={style}>
    {children}
  </p>
);

const Text = ({
  children,
  className = '',
  style,
  type,
  strong,
  code,
}) => (
  <span
    className={classNames(
      'antd-typography-text',
      type ? `antd-typography-text-${type}` : '',
      strong ? 'antd-typography-text-strong' : '',
      code ? 'antd-typography-text-code' : '',
      className,
    )}
    style={style}
  >
    {children}
  </span>
);

export const Typography = { Title, Paragraph, Text };

export const Card = ({
  children,
  className = '',
  style,
  title,
  extra,
  bordered = true,
}) => (
  <div
    className={classNames(
      'antd-card',
      bordered ? 'antd-card-bordered' : 'antd-card-borderless',
      className,
    )}
    style={style}
  >
    {(title || extra) && (
      <div className="antd-card-head">
        <div className="antd-card-title">{title}</div>
        {extra && <div className="antd-card-extra">{extra}</div>}
      </div>
    )}
    <div className="antd-card-body">{children}</div>
  </div>
);

const sizeMap = {
  small: 8,
  middle: 16,
  large: 24,
};

const alignMap = {
  start: 'flex-start',
  center: 'center',
  end: 'flex-end',
  baseline: 'baseline',
  stretch: 'stretch',
};

export const Space = ({
  children,
  className = '',
  direction = 'horizontal',
  size = 'small',
  align = 'stretch',
  wrap = false,
  style,
}) => {
  const gap = typeof size === 'number' ? size : sizeMap[size] ?? sizeMap.small;
  const baseStyle = {
    display: 'flex',
    flexDirection: direction === 'vertical' ? 'column' : 'row',
    alignItems: alignMap[align] ?? alignMap.stretch,
    gap,
    flexWrap: wrap ? 'wrap' : 'nowrap',
  };
  return (
    <div
      className={classNames('antd-space', className)}
      style={{ ...baseStyle, ...(style ?? {}) }}
    >
      {children}
    </div>
  );
};

const alertTypeClass = {
  success: 'antd-alert-success',
  info: 'antd-alert-info',
  warning: 'antd-alert-warning',
  error: 'antd-alert-error',
};

export const Alert = ({
  type = 'info',
  showIcon = false,
  message,
  className = '',
  style,
}) => (
  <div className={classNames('antd-alert', alertTypeClass[type], className)} style={style}>
    {showIcon && <span className="antd-alert-icon" aria-hidden="true">●</span>}
    <span className="antd-alert-message">{message}</span>
  </div>
);

export const Button = ({
  children,
  type = 'default',
  size = 'middle',
  block = false,
  loading = false,
  className = '',
  style,
  disabled,
  ...rest
}) => {
  const finalDisabled = Boolean(disabled || loading);
  return (
    <button
      {...rest}
      className={classNames(
        'antd-btn',
        `antd-btn-${type}`,
        `antd-btn-${size}`,
        block ? 'antd-btn-block' : '',
        loading ? 'antd-btn-loading' : '',
        className,
      )}
      style={style}
      disabled={finalDisabled}
    >
      {loading ? <span className="antd-btn-spinner" aria-hidden="true" /> : null}
      <span className="antd-btn-label">{children}</span>
    </button>
  );
};

const InputBase = forwardRef(
  (
    {
      block = false,
      size = 'middle',
      onPressEnter,
      className = '',
      style,
      type = 'text',
      ...rest
    },
    ref,
  ) => {
    const { onKeyDown, ...inputRest } = rest;
    const handleKeyDown = (event) => {
      if (event.key === 'Enter' && onPressEnter) {
        onPressEnter(event);
      }
      if (onKeyDown) {
        onKeyDown(event);
      }
    }
  };
  return (
    <input
      {...inputRest}
      ref={ref}
      type={type}
      className={classNames('antd-input', `antd-input-${size}`, block ? 'antd-input-block' : '', className)}
      style={style}
      onKeyDown={handleKeyDown}
    />
  );
  }
);
InputBase.displayName = 'Input';

const Password = forwardRef((props, ref) => <InputBase {...props} type="password" ref={ref} />);
Password.displayName = 'InputPassword';

export const Input = Object.assign(InputBase, { Password });

export const Select = forwardRef(({ options = [], loading = false, className = '', style, onChange, value, ...rest }, ref) => (
  <div className={classNames('antd-select-wrapper', className)} style={style}>
    <select
      {...rest}
      ref={ref}
      className="antd-select"
      value={value ?? ''}
      onChange={(event) => onChange?.(event.target.value)}
      disabled={loading || rest.disabled}
    >
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
    {loading && <span className="antd-select-spinner" aria-hidden="true" />}
  </div>
));
Select.displayName = 'Select';

export const Tag = ({ children, color = 'default', className = '', style }) => (
  <span className={classNames('antd-tag', className)} data-color={color} style={style}>
    {children}
  </span>
);

export const Progress = ({ percent = 0, size = 'default', className = '', style }) => {
  const clamped = Math.max(0, Math.min(100, percent));
  return (
    <div className={classNames('antd-progress', size === 'small' ? 'antd-progress-small' : '', className)} style={style}>
      <div className="antd-progress-outer">
        <div className="antd-progress-inner">
          <div className="antd-progress-bg" style={{ width: `${clamped}%` }} />
        </div>
      </div>
      <span className="antd-progress-text">{`${clamped}%`}</span>
    </div>
  );
};

export const Empty = ({ description, className = '', style }) => (
  <div className={classNames('antd-empty', className)} style={style}>
    <div className="antd-empty-icon" aria-hidden="true">
      ∅
    </div>
    <div className="antd-empty-description">{description}</div>
  </div>
);

const computeKey = (rowKey, record, index) => {
  if (typeof rowKey === 'function') {
    return rowKey(record, index);
  }
  if (rowKey && record && rowKey in record) {
    return record[rowKey];
  }
  return index;
};

const columnKey = (column, index) => column?.key ?? column?.dataIndex ?? index;

const alignCells = (columns) =>
  columns.map((column) => ({
    ...column,
    align: column.align ?? 'left',
  }));

export const Table = ({
  columns = [],
  dataSource = [],
  rowKey = 'key',
  className = '',
  style,
  loading = false,
  locale,
  scroll,
}) => {
  const normalizedColumns = alignCells(columns);
  const isLoading = typeof loading === 'boolean' ? loading : Boolean(loading?.spinning);
  const tip = typeof loading === 'boolean' ? 'Loading…' : loading?.tip ?? 'Loading…';
  const emptyText = locale?.emptyText ?? 'No data';
  const containerStyle = {
    overflowX: scroll?.x ? 'auto' : undefined,
    ...style,
  };

  if (isLoading) {
    return (
      <div className={classNames('antd-table', className)} style={containerStyle}>
        <div className="antd-table-status">{tip}</div>
      </div>
    );
  }

  if (!dataSource || dataSource.length === 0) {
    return (
      <div className={classNames('antd-table', className)} style={containerStyle}>
        <div className="antd-table-status">{emptyText}</div>
      </div>
    );
  }

  return (
    <div className={classNames('antd-table', className)} style={containerStyle}>
      <table className="antd-table-element">
        <thead>
          <tr>
            {normalizedColumns.map((column, index) => (
              <th key={columnKey(column, index)} style={{ width: column.width }}>
                {column.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {dataSource.map((record, recordIndex) => (
            <tr key={computeKey(rowKey, record, recordIndex)}>
              {normalizedColumns.map((column, columnIndex) => {
                const value = column.dataIndex
                  ? typeof column.dataIndex === 'string'
                    ? record?.[column.dataIndex]
                    : record?.[column.dataIndex]
                  : undefined;
                const content = column.render ? column.render(value, record, recordIndex) : value;
                return (
                  <td key={columnKey(column, columnIndex)} style={{ textAlign: column.align }}>
                    {content}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

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
