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
      size={2}
      className="plan-node-action-helper plan-mode-panel plan-mode-panel-design"
    >
      <Paragraph type="secondary" style={{ marginBottom: 0 }}>
        {translate('planDetailModeDesignHint')}
      </Paragraph>
      <Text type="secondary" className="plan-mode-panel-label">
        <Tag color="purple">
          {translate(PLAN_MODE_LABEL.design)}
        </Tag>
        {translate('planDetailNodeEdit')}
      </Text>
    </Space>
  );
}
