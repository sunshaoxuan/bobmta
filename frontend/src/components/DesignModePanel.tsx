import React from '../../vendor/react/index.js';
import { Typography } from '../../vendor/antd/index.js';
import type { LocalizationState } from '../i18n/useLocalization';

const { Paragraph } = Typography;

type DesignModePanelProps = {
  translate: LocalizationState['translate'];
};

export function DesignModePanel({ translate }: DesignModePanelProps) {
  return (
    <Paragraph
      type="secondary"
      className="plan-node-action-helper plan-mode-panel plan-mode-panel-design"
    >
      {translate('planDetailModeDesignHint')}
    </Paragraph>
  );
}
