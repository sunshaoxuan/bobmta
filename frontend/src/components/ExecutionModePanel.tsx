import React, { type ReactNode } from '../../vendor/react/index.js';
import { Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_MODE_LABEL } from '../constants/planMode';

const { Paragraph, Text } = Typography;

type ExecutionModePanelProps = {
  translate: LocalizationState['translate'];
  currentNodeName: string | null;
  completedCount: number;
  totalCount: number;
  children?: ReactNode;
};

export function ExecutionModePanel({
  translate,
  currentNodeName,
  completedCount,
  totalCount,
  children,
}: ExecutionModePanelProps) {
  const hasNodes = totalCount > 0;

  return (
    <Space
      direction="vertical"
      size={4}
      className="plan-node-action-helper plan-mode-panel plan-mode-panel-execution"
      style={{ width: '100%' }}
    >
      <Text strong className="plan-mode-panel-title">
        <Tag color="geekblue">{translate(PLAN_MODE_LABEL.execution)}</Tag>
        <span className="plan-mode-panel-value">{translate('planDetailModeExecution')}</span>
      </Text>
      <Paragraph type="secondary" className="plan-mode-panel-description">
        {translate('planDetailModeExecutionHint')}
      </Paragraph>
      {hasNodes ? (
        <Space size={8} wrap>
          {currentNodeName ? (
            <Tag color="volcano">
              {translate('planDetailNodeCurrentTag')}
              <span className="plan-mode-panel-value"> {currentNodeName}</span>
            </Tag>
          ) : null}
          <Tag color="default">
            {translate('planPreviewProgressLabel')}
            <span className="plan-mode-panel-value">
              {` ${completedCount}/${totalCount}`}
            </span>
          </Tag>
        </Space>
      ) : null}
      {children ? <Space direction="vertical" size="small">{children}</Space> : null}
    </Space>
  );
}
