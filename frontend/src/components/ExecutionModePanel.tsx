import React from '../../vendor/react/index.js';
import { Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { LocalizationState } from '../i18n/useLocalization';

const { Paragraph } = Typography;

type ExecutionModePanelProps = {
  translate: LocalizationState['translate'];
  currentNodeName: string | null;
  completedCount: number;
  totalCount: number;
};

export function ExecutionModePanel({
  translate,
  currentNodeName,
  completedCount,
  totalCount,
}: ExecutionModePanelProps) {
  const hasNodes = totalCount > 0;

  return (
    <Space
      direction="vertical"
      size={4}
      className="plan-mode-panel plan-mode-panel-execution"
    >
      <Paragraph type="secondary" className="plan-node-action-helper">
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
    </Space>
  );
}
