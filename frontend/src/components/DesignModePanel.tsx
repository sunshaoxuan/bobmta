import React from '../../vendor/react/index.js';
import { Space, Tag, Typography } from '../../vendor/antd/index.js';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_MODE_LABEL } from '../constants/planMode';

const { Paragraph, Text } = Typography;

type DesignModePanelProps = {
  translate: LocalizationState['translate'];
};

export function DesignModePanel({ translate }: DesignModePanelProps) {
  return (
    <Space
      direction="vertical"
      size={4}
      className="plan-node-action-helper plan-mode-panel plan-mode-panel-design"
    >
      <Text strong className="plan-mode-panel-title">
        <Tag color="purple">{translate(PLAN_MODE_LABEL.design)}</Tag>
        <span className="plan-mode-panel-value">{translate('planDetailModeDesign')}</span>
      </Text>
      <Paragraph type="secondary" style={{ marginBottom: 0 }}>
        {translate('planDetailModeDesignHint')}
      </Paragraph>
      <Text type="secondary" className="plan-mode-panel-label">
        {translate('planDetailNodeEdit')}
      </Text>
    </Space>
  );
}
