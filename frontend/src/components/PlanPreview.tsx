// @ts-nocheck
import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from '../../vendor/react/index.js';
import {
  Alert,
  Button,
  Card,
  Select,
  Space,
  Tag,
  Typography,
} from '../../vendor/antd/index.js';
import type {
  PlanDetail,
  PlanNode,
  PlanReminderSummary,
  PlanStatus,
  PlanSummary,
  PlanTimelineEntry,
} from '../api/types';
import type { Locale, UiMessageKey } from '../i18n/localization';
import type { LocalizationState } from '../i18n/useLocalization';
import { PLAN_STATUS_COLOR, PLAN_STATUS_LABEL } from '../constants/planStatus';
import {
  PLAN_MODE_LABEL,
  PLAN_STATUS_MODE,
  type PlanViewMode,
} from '../constants/planMode';
import { PLAN_REMINDER_CHANNEL_COLOR, PLAN_REMINDER_CHANNEL_LABEL } from '../constants/planReminder';
import { formatDateTime, formatPlanWindow } from '../utils/planFormatting';
import type { PlanDetailState } from '../state/planDetail';
import { PlanNodeTree } from './PlanNodeTree';
import { PlanDetailSection } from './PlanDetailSection';
import { PlanNodeActions, type PlanNodeActionIntent } from './PlanNodeActions';
import { PlanReminderBoard } from './PlanReminderBoard';
import { PlanTimelineBoard } from './PlanTimelineBoard';
import { DesignModePanel } from './DesignModePanel';
import { ExecutionModePanel } from './ExecutionModePanel';
import {
  collectCompletedPlanNodeIds,
  flattenPlanNodes,
  getActionablePlanNodes,
  type PlanNodeWithPath,
  type PlanNodeActionType,
} from '../utils/planNodes';
import type {
  PlanDetailMutationState,
  PlanNodeActionInput,
  PlanReminderUpdateInput,
} from '../state/planDetail';
import { formatApiErrorMessage } from '../utils/apiErrors';

const { Text, Paragraph } = Typography;

type PlanPreviewModeSectionDefinition = {
  title: UiMessageKey;
  showModePanel: boolean;
};

type PlanPreviewNodeSectionDefinition = PlanPreviewModeSectionDefinition & {
  allowEdit: boolean;
};

type PlanPreviewReminderSectionDefinition = PlanPreviewModeSectionDefinition & {
  allowEdit: boolean;
};

type PlanPreviewModeDefinition = {
  tagColor: string;
  sections: {
    nodes: PlanPreviewNodeSectionDefinition;
    actions: PlanPreviewModeSectionDefinition;
    reminders: PlanPreviewReminderSectionDefinition;
  };
};
const PLAN_PREVIEW_MODE_DEFINITIONS: Record<PlanViewMode, PlanPreviewModeDefinition> = {
  design: {
    tagColor: 'purple',
    sections: {
      nodes: {
        title: 'planDetailNodesTitleDesign',
        showModePanel: true,
        allowEdit: true,
      },
      actions: {
        title: 'planDetailActionsTitleDesign',
        showModePanel: true,
      },
      reminders: {
        title: 'planDetailRemindersTitle',
        showModePanel: false,
        allowEdit: true,
      },
    },
  },
  execution: {
    tagColor: 'geekblue',
    sections: {
      nodes: {
        title: 'planDetailNodesTitleExecution',
        showModePanel: true,
        allowEdit: false,
      },
      actions: {
        title: 'planDetailActionsTitleExecution',
        showModePanel: true,
      },
      reminders: {
        title: 'planDetailRemindersTitle',
        showModePanel: false,
        allowEdit: false,
      },
    },
  },
};

type PlanPreviewProps = {
  plan: PlanSummary | null;
  translate: LocalizationState['translate'];
  locale: Locale;
  onClose: () => void;
  detailState: PlanDetailState;
  onRefreshDetail: () => void;
  detailErrorDetail: string | null;
  onExecuteNodeAction: (input: PlanNodeActionInput) => Promise<void>;
  onUpdateReminder: (input: PlanReminderUpdateInput) => Promise<void>;
  currentUserName: string | null;
  onTimelineCategoryChange: (category: string | null) => void;
};

export function PlanPreview({
  plan,
  translate,
  locale,
  onClose,
  detailState,
  onRefreshDetail,
  detailErrorDetail,
  onExecuteNodeAction,
  onUpdateReminder,
  currentUserName,
  onTimelineCategoryChange,
}: PlanPreviewProps) {
  const isActiveDetail = Boolean(plan && detailState.activePlanId === plan.id);
  const detail: PlanDetail | null = isActiveDetail ? detailState.detail : null;
  const timeline: PlanTimelineEntry[] = isActiveDetail ? detailState.timeline : [];
  const reminders: PlanReminderSummary[] = isActiveDetail ? detailState.reminders : [];
  const nodes = detail?.nodes ?? [];
  const detailStatus = isActiveDetail ? detailState.status : 'idle';
  const detailError = isActiveDetail ? detailState.error : null;
  const detailOrigin = isActiveDetail ? detailState.origin : null;
  const detailContext = isActiveDetail ? detailState.context : null;
  const previewStatus: PlanStatus | null =
    detailContext?.planStatus ?? detail?.status ?? plan?.status ?? null;
  const statusMode: PlanViewMode = previewStatus ? PLAN_STATUS_MODE[previewStatus] : 'design';
  const mode: PlanViewMode = detailContext ? detailContext.mode : statusMode;
  const modeDefinition = PLAN_PREVIEW_MODE_DEFINITIONS[mode];
  const nodesSection = modeDefinition.sections.nodes;
  const actionsSection = modeDefinition.sections.actions;
  const remindersSection = modeDefinition.sections.reminders;
  const showActionList =
    mode === 'execution' && previewStatus ? ['SCHEDULED', 'IN_PROGRESS'].includes(previewStatus) : false;
  const currentNodeId = detailContext ? detailContext.currentNodeId : null;
  const [editingNodeId, setEditingNodeId] = useState<string | null>(null);
  const lastUpdatedLabel =
    isActiveDetail && detailState.lastUpdated
      ? translate('planDetailLastUpdated', {
          time: formatDateTime(detailState.lastUpdated, locale) ?? detailState.lastUpdated,
        })
      : null;

  const summary = detail ?? plan;
  const summaryStatus = summary?.status ?? plan?.status ?? 'DESIGN';
  const description = detail?.description ?? translate('planDetailDescriptionFallback');
  const tags = detail?.tags ?? [];
  const participantNames = detail
    ? detail.participants.map((participant) => participant.name)
    : plan?.participants ?? [];
  const [actionDialog, setActionDialog] = useState<NodeActionDialogState | null>(null);
  const [reminderEditor, setReminderEditor] = useState<ReminderEditorState | null>(null);
  const [selectedReminderId, setSelectedReminderId] = useState<string | null>(null);
  const [highlightedTimelineEntryId, setHighlightedTimelineEntryId] = useState<string | null>(null);

  useEffect(() => {
    setActionDialog(null);
    setReminderEditor(null);
    setSelectedReminderId(null);
    setHighlightedTimelineEntryId(null);
    setEditingNodeId(null);
  }, [detail?.id, plan?.id]);

  const timelineCategoryFilter = detailState.filters.timeline.activeCategory;

  const actionableNodes: PlanNodeWithPath[] = useMemo(
    () => (showActionList ? getActionablePlanNodes(nodes) : []),
    [nodes, showActionList]
  );
  const nodeLookup = useMemo(() => {
    const map = new Map<string, PlanNodeWithPath>();
    for (const entry of flattenPlanNodes(nodes)) {
      map.set(entry.node.id, entry);
    }
    return map;
  }, [nodes]);
  const completedNodeIds = useMemo(() => collectCompletedPlanNodeIds(nodes), [nodes]);
  const currentNodeName = currentNodeId ? nodeLookup.get(currentNodeId)?.node.name ?? null : null;
  const totalNodeCount = nodeLookup.size;

  useEffect(() => {
    if (!nodesSection.allowEdit) {
      setEditingNodeId(null);
    }
  }, [nodesSection.allowEdit]);

  useEffect(() => {
    if (!remindersSection.allowEdit) {
      setReminderEditor(null);
      setSelectedReminderId(null);
    }
  }, [remindersSection.allowEdit]);

  const mutation = detailState.mutation;
  const nodeMutationContext = mutation.context?.type === 'node' ? mutation.context : null;
  const reminderMutationContext = mutation.context?.type === 'reminder' ? mutation.context : null;

  useEffect(() => {
    if (mutation.status !== 'success' || !mutation.completedAt) {
      return;
    }
    if (!detail || timeline.length === 0) {
      return;
    }
    const newestEntryId = timeline[0]?.id ?? null;
    if (!newestEntryId) {
      return;
    }
    setHighlightedTimelineEntryId(newestEntryId);
    if (typeof window === 'undefined') {
      return;
    }
    const timer = window.setTimeout(() => {
      setHighlightedTimelineEntryId((current) => (current === newestEntryId ? null : current));
    }, 8000);
    return () => {
      window.clearTimeout(timer);
    };
  }, [detail, mutation.completedAt, mutation.status, timeline]);

  const pendingNodeAction =
    nodeMutationContext && mutation.status === 'loading'
      ? { nodeId: nodeMutationContext.nodeId, action: nodeMutationContext.action }
      : null;
  const pendingNodeStatus =
    nodeMutationContext && mutation.status === 'loading' ? 'loading' : 'idle';

  const pendingReminderId = reminderMutationContext ? reminderMutationContext.reminderId : null;
  const pendingReminderStatus =
    reminderMutationContext && mutation.status === 'loading' ? 'loading' : 'idle';

  const handleNodeAction = (intent: PlanNodeActionIntent) => {
    if (!detail) {
      return;
    }
    const nodeEntry = nodeLookup.get(intent.nodeId);
    const defaultOperator =
      getDefaultOperatorId({
        detail,
        node: nodeEntry?.node ?? null,
        currentUserName,
      }) ?? '';
    const defaultAssignee =
      intent.action === 'handover'
        ? getDefaultAssigneeId({ detail, node: nodeEntry?.node ?? null }) ?? ''
        : nodeEntry?.node.assignee?.id ?? '';
    setActionDialog({
      intent,
      operatorId: defaultOperator,
      assigneeId: defaultAssignee,
      resultSummary: nodeEntry?.node.resultSummary ?? '',
      comment: '',
    });
  };

  const handleNodeEdit = (node: PlanNode) => {
    if (mode !== 'design') {
      return;
    }
    setEditingNodeId((current) => (current === node.id ? null : node.id));
  };

  const handleReminderToggle = (reminder: PlanReminderSummary) => {
    if (!detail || !remindersSection.allowEdit) {
      return;
    }
    setSelectedReminderId(reminder.id);
    void onUpdateReminder({
      planId: detail.id,
      reminderId: reminder.id,
      active: !reminder.active,
    });
  };

  const handleReminderEdit = (reminder: PlanReminderSummary) => {
    if (!detail || !remindersSection.allowEdit) {
      return;
    }
    setSelectedReminderId(reminder.id);
    setReminderEditor({
      reminder,
      active: reminder.active,
      offsetMinutes: reminder.offsetMinutes,
    });
  };

  const handleRetryNodeMutation = useCallback(() => {
    const context = mutation.context;
    if (!context || context.type !== 'node') {
      return;
    }
    void onExecuteNodeAction(context.input);
  }, [mutation, onExecuteNodeAction]);

  const handleEditNodeMutation = useCallback(() => {
    const context = mutation.context;
    if (!detail || !context || context.type !== 'node') {
      return;
    }
    const nodeEntry = nodeLookup.get(context.nodeId);
    if (!nodeEntry) {
      return;
    }
    const dialog: NodeActionDialogState = {
      intent: {
        nodeId: context.nodeId,
        nodeName: nodeEntry.node.name,
        path: nodeEntry.path,
        action: context.action,
      },
      operatorId: context.input.operatorId,
      assigneeId: nodeEntry?.node.assignee?.id ?? '',
      resultSummary: nodeEntry?.node.resultSummary ?? '',
      comment: '',
    };
    switch (context.input.type) {
      case 'complete':
        dialog.resultSummary = context.input.resultSummary ?? dialog.resultSummary;
        break;
      case 'handover':
        dialog.assigneeId = context.input.assigneeId;
        dialog.comment = context.input.comment ?? '';
        break;
      default:
        break;
    }
    setActionDialog(dialog);
  }, [detail, mutation, nodeLookup]);

  const handleRetryReminderMutation = useCallback(() => {
    const context = mutation.context;
    if (!context || context.type !== 'reminder') {
      return;
    }
    void onUpdateReminder(context.input);
  }, [mutation, onUpdateReminder]);

  const handleEditReminderMutation = useCallback(() => {
    const context = mutation.context;
    if (!context || context.type !== 'reminder') {
      return;
    }
    const reminder = reminders.find((item) => item.id === context.reminderId);
    if (!detail || !reminder) {
      return;
    }
    setSelectedReminderId(reminder.id);
    setReminderEditor({
      reminder,
      active: context.input.active,
      offsetMinutes:
        typeof context.input.offsetMinutes === 'number'
          ? context.input.offsetMinutes
          : reminder.offsetMinutes,
    });
  }, [detail, mutation, reminders]);

  const nodeActionHelper = renderNodeMutationHelper({
    mutation,
    translate,
    nodeLookup,
    onRetry: handleRetryNodeMutation,
    onEdit: handleEditNodeMutation,
  });

  const reminderHelper = renderReminderMutationHelper({
    mutation,
    translate,
    reminders,
    onRetry: handleRetryReminderMutation,
    onEdit: handleEditReminderMutation,
  });

  const updateActionDialog = (patch: Partial<NodeActionDialogState>) => {
    setActionDialog((current) => (current ? { ...current, ...patch } : current));
  };

  const updateReminderEditor = (patch: Partial<ReminderEditorState>) => {
    setReminderEditor((current) => (current ? { ...current, ...patch } : current));
  };

  const handleNodeActionSubmit = async () => {
    if (!detail || !actionDialog) {
      return;
    }
    const operatorId = actionDialog.operatorId.trim();
    if (!operatorId) {
      return;
    }
    let payload: PlanNodeActionInput | null = null;
    switch (actionDialog.intent.action) {
      case 'start':
        payload = {
          planId: detail.id,
          nodeId: actionDialog.intent.nodeId,
          type: 'start',
          operatorId,
        };
        break;
      case 'complete': {
        const resultSummary = actionDialog.resultSummary.trim();
        payload = {
          planId: detail.id,
          nodeId: actionDialog.intent.nodeId,
          type: 'complete',
          operatorId,
          resultSummary: resultSummary ? resultSummary : null,
        };
        break;
      }
      case 'handover': {
        const assigneeId = actionDialog.assigneeId;
        if (!assigneeId) {
          return;
        }
        const comment = actionDialog.comment.trim();
        payload = {
          planId: detail.id,
          nodeId: actionDialog.intent.nodeId,
          type: 'handover',
          operatorId,
          assigneeId,
          comment: comment ? comment : null,
        };
        break;
      }
    }
    if (!payload) {
      return;
    }
    try {
      await onExecuteNodeAction(payload);
    } catch (error) {
      // Mutation feedback handled separately
    }
  };

  const handleReminderEditorSubmit = async () => {
    if (!detail || !reminderEditor) {
      return;
    }
    try {
      await onUpdateReminder({
        planId: detail.id,
        reminderId: reminderEditor.reminder.id,
        active: reminderEditor.active,
        offsetMinutes: reminderEditor.offsetMinutes,
      });
    } catch (error) {
      // Mutation feedback handled separately
    }
  };

  useEffect(() => {
    if (
      actionDialog &&
      detail &&
      mutation.status === 'success' &&
      mutation.context?.type === 'node' &&
      mutation.context.nodeId === actionDialog.intent.nodeId
    ) {
      setActionDialog(null);
    }
  }, [actionDialog, detail, mutation]);

  useEffect(() => {
    if (
      reminderEditor &&
      detail &&
      mutation.status === 'success' &&
      mutation.context?.type === 'reminder' &&
      mutation.context.reminderId === reminderEditor.reminder.id
    ) {
      setReminderEditor(null);
    }
  }, [detail, mutation, reminderEditor]);

  return (
    <Card
      title={translate('planPreviewHeader')}
      className="plan-preview"
      extra={
        plan ? (
          <Button type="text" className="plan-preview-close" onClick={onClose}>
            {translate('planPreviewClose')}
          </Button>
        ) : null
      }
    >
      {!plan ? (
        <Alert
          type="info"
          showIcon
          message={translate('planPreviewEmptyTitle')}
          description={<Paragraph className="plan-preview-empty-text">{translate('planPreviewEmptyDescription')}</Paragraph>}
        />
      ) : (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div className="plan-preview-header">
            <Text className="plan-preview-title">{summary?.title ?? plan.title}</Text>
            <Space size="small">
              <Tag color={PLAN_STATUS_COLOR[summaryStatus]}>
                {translate(PLAN_STATUS_LABEL[summaryStatus])}
              </Tag>
              <Tag color={modeDefinition.tagColor}>
                {translate(PLAN_MODE_LABEL[mode])}
              </Tag>
            </Space>
          </div>
          <Paragraph className="plan-preview-description">{description}</Paragraph>
          <div className="plan-preview-meta">
            {lastUpdatedLabel ? (
              <Text type="secondary" className="plan-preview-meta-item">
                {lastUpdatedLabel}
              </Text>
            ) : null}
            {detailOrigin ? (
              <Tag color={detailOrigin === 'cache' ? 'gold' : 'geekblue'}>
                {translate(
                  detailOrigin === 'cache' ? 'planDetailOriginCache' : 'planDetailOriginNetwork'
                )}
              </Tag>
            ) : null}
            <Button
              type="link"
              size="small"
              onClick={onRefreshDetail}
              loading={detailStatus === 'loading'}
            >
              {translate('planDetailRefresh')}
            </Button>
          </div>
          <div className="plan-preview-grid">
            <PreviewField label={translate('planPreviewOwnerLabel')}>
              <Tag color="blue">{summary?.owner ?? plan.owner}</Tag>
            </PreviewField>
            <PreviewField label={translate('planDetailCustomerLabel')}>
              <Text>{detail?.customer?.name ?? translate('planPreviewEmptyValue')}</Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewWindowLabel')}>
              <Text>{formatPlanWindow(summary ?? plan, locale, translate)}</Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewParticipantsLabel')}>
              <Space size="small" wrap>
                {participantNames.length > 0 ? (
                  participantNames.map((participant) => (
                    <Tag key={participant} color="purple">
                      {participant}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">{translate('planPreviewParticipantsEmpty')}</Text>
                )}
              </Space>
            </PreviewField>
            <PreviewField label={translate('planDetailTagsLabel')}>
              <Space size="small" wrap>
                {tags.length > 0 ? (
                  tags.map((tag) => (
                    <Tag key={tag} color="blue">
                      {tag}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">{translate('planPreviewParticipantsEmpty')}</Text>
                )}
              </Space>
            </PreviewField>
            <PreviewField label={translate('planPreviewStartLabel')}>
              <Text>
                {formatDateTime(summary?.plannedStartTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewEndLabel')}>
              <Text>
                {formatDateTime(summary?.plannedEndTime ?? null, locale) ||
                  translate('planPreviewEmptyValue')}
              </Text>
            </PreviewField>
            <PreviewField label={translate('planPreviewProgressLabel')}>
              <Text strong>{normalizeProgress(summary?.progress ?? plan.progress)}%</Text>
            </PreviewField>
          </div>
          {detailError ? (
            <Alert
              type="error"
              showIcon
              message={translate('planError', {
                error: detailErrorDetail ?? translate('commonStateErrorDescription'),
              })}
            />
          ) : null}
          <div className="plan-preview-sections">
            <PlanDetailSection
              title={translate(nodesSection.title)}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={detailStatus === 'success' && nodes.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailNodesEmpty')}
              className="plan-preview-nodes-section"
              helper={
                renderModeAwareHelper({
                  mode,
                  helper: null,
                  translate,
                  currentNodeName,
                  completedCount: completedNodeIds.size,
                  totalCount: totalNodeCount,
                  showModePanel: nodesSection.showModePanel,
                })
              }
            >
              <PlanNodeTree
                nodes={nodes}
                translate={translate}
                locale={locale}
                mode={mode}
                currentNodeId={currentNodeId}
                completedNodeIds={completedNodeIds}
                onEditNode={nodesSection.allowEdit ? handleNodeEdit : undefined}
                editingNodeId={editingNodeId}
              />
            </PlanDetailSection>
            <PlanDetailSection
              title={translate(actionsSection.title)}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={showActionList ? actionableNodes.length === 0 : false}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailActionsEmpty')}
              helper={
                renderModeAwareHelper({
                  mode,
                  helper: nodeActionHelper,
                  translate,
                  currentNodeName,
                  completedCount: completedNodeIds.size,
                  totalCount: totalNodeCount,
                  showModePanel: actionsSection.showModePanel,
                })
              }
            >
              {showActionList ? (
                <PlanNodeActions
                  mode={mode}
                  candidates={actionableNodes}
                  translate={translate}
                  onAction={handleNodeAction}
                  pendingAction={pendingNodeAction}
                  pendingStatus={pendingNodeStatus}
                />
              ) : null}
              {actionDialog ? (
                <NodeActionForm
                  dialog={actionDialog}
                  detail={detail}
                  translate={translate}
                  onUpdate={updateActionDialog}
                  onCancel={() => {
                    setActionDialog(null);
                  }}
                  onSubmit={() => {
                    void handleNodeActionSubmit();
                  }}
                  submitting={Boolean(
                    actionDialog &&
                      nodeMutationContext &&
                      mutation.status === 'loading' &&
                      nodeMutationContext.nodeId === actionDialog.intent.nodeId
                  )}
                />
              ) : null}
            </PlanDetailSection>
            <PlanTimelineBoard
              entries={timeline}
              status={detailStatus}
              error={detailError}
              translate={translate}
              locale={locale}
              activeCategory={timelineCategoryFilter}
              highlightedEntryId={highlightedTimelineEntryId}
              onCategoryChange={onTimelineCategoryChange}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
            />
            <PlanDetailSection
              title={translate(remindersSection.title)}
              status={detailStatus}
              error={detailError}
              translate={translate}
              empty={reminders.length === 0}
              onRetry={onRefreshDetail}
              errorDetail={detailErrorDetail}
              emptyMessage={translate('planDetailRemindersEmpty')}
              helper={
                renderModeAwareHelper({
                  mode,
                  helper: reminderHelper,
                  translate,
                  currentNodeName,
                  completedCount: completedNodeIds.size,
                  totalCount: totalNodeCount,
                  showModePanel: remindersSection.showModePanel,
                })
              }
            >
              <PlanReminderBoard
                reminders={reminders}
                translate={translate}
                mode={mode}
                allowEdit={remindersSection.allowEdit}
                onEdit={handleReminderEdit}
                onToggle={handleReminderToggle}
                selectedReminderId={selectedReminderId}
                pendingReminderId={pendingReminderId}
                pendingStatus={pendingReminderStatus}
              />
              {reminderEditor ? (
                <ReminderEditorForm
                  editor={reminderEditor}
                  translate={translate}
                  onUpdate={updateReminderEditor}
                  onCancel={() => {
                    setReminderEditor(null);
                    setSelectedReminderId(null);
                  }}
                  onSubmit={() => {
                    void handleReminderEditorSubmit();
                  }}
                  submitting={Boolean(
                    reminderEditor &&
                      reminderMutationContext &&
                      mutation.status === 'loading' &&
                      reminderMutationContext.reminderId === reminderEditor.reminder.id
                  )}
                />
              ) : null}
            </PlanDetailSection>
          </div>
        </Space>
      )}
    </Card>
  );
}

type PreviewFieldProps = {
  label: string;
  children: ReactNode;
};

function PreviewField({ label, children }: PreviewFieldProps) {
  return (
    <div className="plan-preview-field">
      <Text type="secondary" className="plan-preview-field-label">
        {label}
      </Text>
      <div className="plan-preview-field-value">{children}</div>
    </div>
  );
}

function normalizeProgress(value: number | undefined): number {
  if (!Number.isFinite(value ?? Number.NaN)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(value ?? 0)));
}

const ACTION_LABEL_KEY: Record<PlanNodeActionType, UiMessageKey> = {
  start: 'planDetailActionStart',
  complete: 'planDetailActionComplete',
  handover: 'planDetailActionHandover',
};

type NodeActionDialogState = {
  intent: PlanNodeActionIntent;
  operatorId: string;
  assigneeId: string;
  resultSummary: string;
  comment: string;
};

type ReminderEditorState = {
  reminder: PlanReminderSummary;
  active: boolean;
  offsetMinutes: number;
};

type ModeAwareHelperOptions = {
  mode: PlanViewMode;
  translate: LocalizationState['translate'];
  helper: ReactNode | null;
  currentNodeName: string | null;
  completedCount: number;
  totalCount: number;
  showModePanel?: boolean;
};

function renderModeAwareHelper({
  mode,
  translate,
  helper,
  currentNodeName,
  completedCount,
  totalCount,
  showModePanel = false,
}: ModeAwareHelperOptions): ReactNode {
  if (!showModePanel) {
    return helper;
  }

  if (mode === 'design') {
    return <DesignModePanel translate={translate}>{helper}</DesignModePanel>;
  }

  return (
    <ExecutionModePanel
      translate={translate}
      currentNodeName={currentNodeName}
      completedCount={completedCount}
      totalCount={totalCount}
    >
      {helper}
    </ExecutionModePanel>
  );
}

type NodeActionFormProps = {
  dialog: NodeActionDialogState;
  detail: PlanDetail | null;
  translate: LocalizationState['translate'];
  onUpdate: (patch: Partial<NodeActionDialogState>) => void;
  onCancel: () => void;
  onSubmit: () => void;
  submitting: boolean;
};

function NodeActionForm({
  dialog,
  detail,
  translate,
  onUpdate,
  onCancel,
  onSubmit,
  submitting,
}: NodeActionFormProps) {
  const participants = detail?.participants ?? [];
  const operatorOptions = [
    { value: '', label: translate('planDetailActionDialogOperatorPlaceholder') },
    ...participants.map((participant) => ({ value: participant.id, label: participant.name })),
  ];
  const assigneeOptions = [
    { value: '', label: translate('planDetailActionDialogAssigneePlaceholder') },
    ...participants.map((participant) => ({ value: participant.id, label: participant.name })),
  ];

  return (
    <div className="plan-node-action-form">
      <div className="plan-node-action-field">
        <Text type="secondary" className="plan-node-action-label">
          {translate('planDetailActionDialogOperatorLabel')}
        </Text>
        <Select
          options={operatorOptions}
          value={dialog.operatorId ?? ''}
          onChange={(value: string) => onUpdate({ operatorId: value })}
        />
      </div>
      {dialog.intent.action === 'handover' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogAssigneeLabel')}
          </Text>
          <Select
            options={assigneeOptions}
            value={dialog.assigneeId ?? ''}
            onChange={(value: string) => onUpdate({ assigneeId: value })}
          />
        </div>
      ) : null}
      {dialog.intent.action === 'complete' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogResultLabel')}
          </Text>
          <textarea
            rows={3}
            value={dialog.resultSummary}
            onChange={(event: { currentTarget: { value: string } }) =>
              onUpdate({ resultSummary: event.currentTarget.value })
            }
          />
        </div>
      ) : null}
      {dialog.intent.action === 'handover' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogCommentLabel')}
          </Text>
          <textarea
            rows={3}
            value={dialog.comment}
            onChange={(event: { currentTarget: { value: string } }) =>
              onUpdate({ comment: event.currentTarget.value })
            }
          />
        </div>
      ) : null}
      <Space size="small">
        <Button
          type="primary"
          size="small"
          onClick={onSubmit}
          loading={submitting}
          disabled={!dialog.operatorId || (dialog.intent.action === 'handover' && !dialog.assigneeId)}
        >
          {translate('planDetailActionDialogConfirm')}
        </Button>
        <Button type="default" size="small" onClick={onCancel} disabled={submitting}>
          {translate('planDetailActionDialogCancel')}
        </Button>
      </Space>
    </div>
  );
}

type ReminderEditorFormProps = {
  editor: ReminderEditorState;
  translate: LocalizationState['translate'];
  onUpdate: (patch: Partial<ReminderEditorState>) => void;
  onCancel: () => void;
  onSubmit: () => void;
  submitting: boolean;
};

function ReminderEditorForm({
  editor,
  translate,
  onUpdate,
  onCancel,
  onSubmit,
  submitting,
}: ReminderEditorFormProps) {
  return (
    <div className="plan-reminder-editor">
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditActiveLabel')}
        </Text>
        <input
          type="checkbox"
          checked={editor.active}
          onChange={(event: { currentTarget: { checked: boolean } }) =>
            onUpdate({ active: event.currentTarget.checked })
          }
        />
      </div>
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditOffsetLabel')}
        </Text>
        <input
          type="number"
          value={editor.offsetMinutes}
          onChange={(event: { currentTarget: { value: string } }) => {
            const next = Number(event.currentTarget.value);
            onUpdate({ offsetMinutes: Number.isNaN(next) ? 0 : next });
          }}
        />
      </div>
      <Space size="small">
        <Button type="primary" size="small" onClick={onSubmit} loading={submitting}>
          {translate('planDetailReminderEditConfirm')}
        </Button>
        <Button type="default" size="small" onClick={onCancel} disabled={submitting}>
          {translate('planDetailReminderEditCancel')}
        </Button>
      </Space>
    </div>
  );
}

type NodeMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  nodeLookup: Map<string, PlanNodeWithPath>;
  onRetry?: () => void;
  onEdit?: () => void;
};

type ReminderMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  reminders: PlanReminderSummary[];
  onRetry?: () => void;
  onEdit?: () => void;
};

type OperatorSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
  currentUserName: string | null;
};

type AssigneeSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
};

function renderNodeMutationHelper({
  mutation,
  translate,
  nodeLookup,
  onRetry,
  onEdit,
}: NodeMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'node') {
    return null;
  }
  const entry = nodeLookup.get(context.nodeId);
  const nodeName = entry?.node.name ?? context.nodeId;
  const actionLabel = translate(ACTION_LABEL_KEY[context.action]);
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailActionProcessing', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailActionSuccess', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'error':
      const nodeActions: ReactNode[] = [];
      if (onRetry) {
        nodeActions.push(
          <Button key="retry" type="primary" size="small" onClick={onRetry}>
            {translate('planDetailActionRetry')}
          </Button>
        );
      }
      if (onEdit) {
        nodeActions.push(
          <Button key="edit" size="small" onClick={onEdit}>
            {translate('planDetailActionRetryEdit')}
          </Button>
        );
      }
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailActionError', {
            action: actionLabel,
            node: nodeName,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
          action={
            nodeActions.length > 0 ? <Space size="small">{nodeActions}</Space> : undefined
          }
        />
      );
    default:
      return null;
  }
}

function renderReminderMutationHelper({
  mutation,
  translate,
  reminders,
  onRetry,
  onEdit,
}: ReminderMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'reminder') {
    return null;
  }
  const reminder = reminders.find((item) => item.id === context.reminderId) ?? null;
  const channelLabel = reminder
    ? translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])
    : context.reminderId;
  const actionLabel = translate(
    context.action === 'edit'
      ? 'planDetailReminderActionEdit'
      : 'planDetailReminderActionToggle'
  );
  const offsetLabel = reminder
    ? translate('planDetailReminderOffsetMinutes', { minutes: reminder.offsetMinutes })
    : '';
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailReminderProcessing', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailReminderSuccess', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'error':
      const reminderActions: ReactNode[] = [];
      if (onRetry) {
        reminderActions.push(
          <Button key="retry" type="primary" size="small" onClick={onRetry}>
            {translate('planDetailReminderRetry')}
          </Button>
        );
      }
      if (onEdit && context.action === 'edit') {
        reminderActions.push(
          <Button key="edit" size="small" onClick={onEdit}>
            {translate('planDetailReminderRetryEdit')}
          </Button>
        );
      }
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailReminderError', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
          action={
            reminderActions.length > 0 ? (
              <Space size="small">{reminderActions}</Space>
            ) : undefined
          }
        />
      );
    default:
      return null;
  }
}

function getDefaultOperatorId({
  detail,
  node,
  currentUserName,
}: OperatorSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  if (currentUserName) {
    const currentUser = participants.find((participant) => participant.name === currentUserName);
    if (currentUser) {
      return currentUser.id;
    }
  }
  if (node?.assignee?.id) {
    return node.assignee.id;
  }
  if (detail) {
    const ownerMatch = participants.find((participant) => participant.name === detail.owner);
    if (ownerMatch) {
      return ownerMatch.id;
    }
  }
  return participants[0]?.id ?? null;
}

function getDefaultAssigneeId({ detail, node }: AssigneeSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  const currentAssignee = node?.assignee?.id ?? null;
  const fallback = participants.find((participant) => participant.id !== currentAssignee);
  return fallback?.id ?? currentAssignee ?? participants[0]?.id ?? null;
}

const ACTION_LABEL_KEY: Record<PlanNodeActionType, UiMessageKey> = {
  start: 'planDetailActionStart',
  complete: 'planDetailActionComplete',
  handover: 'planDetailActionHandover',
};

type NodeActionDialogState = {
  intent: PlanNodeActionIntent;
  operatorId: string;
  assigneeId: string;
  resultSummary: string;
  comment: string;
};

type ReminderEditorState = {
  reminder: PlanReminderSummary;
  active: boolean;
  offsetMinutes: number;
};

type NodeActionFormProps = {
  dialog: NodeActionDialogState;
  detail: PlanDetail | null;
  translate: LocalizationState['translate'];
  onUpdate: (patch: Partial<NodeActionDialogState>) => void;
  onCancel: () => void;
  onSubmit: () => void;
  submitting: boolean;
};

function NodeActionForm({
  dialog,
  detail,
  translate,
  onUpdate,
  onCancel,
  onSubmit,
  submitting,
}: NodeActionFormProps) {
  const participants = detail?.participants ?? [];
  const operatorOptions = [
    { value: '', label: translate('planDetailActionDialogOperatorPlaceholder') },
    ...participants.map((participant) => ({ value: participant.id, label: participant.name })),
  ];
  const assigneeOptions = [
    { value: '', label: translate('planDetailActionDialogAssigneePlaceholder') },
    ...participants.map((participant) => ({ value: participant.id, label: participant.name })),
  ];

  return (
    <div className="plan-node-action-form">
      <div className="plan-node-action-field">
        <Text type="secondary" className="plan-node-action-label">
          {translate('planDetailActionDialogOperatorLabel')}
        </Text>
        <Select
          options={operatorOptions}
          value={dialog.operatorId ?? ''}
          onChange={(value: string) => onUpdate({ operatorId: value })}
        />
      </div>
      {dialog.intent.action === 'handover' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogAssigneeLabel')}
          </Text>
          <Select
            options={assigneeOptions}
            value={dialog.assigneeId ?? ''}
            onChange={(value: string) => onUpdate({ assigneeId: value })}
          />
        </div>
      ) : null}
      {dialog.intent.action === 'complete' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogResultLabel')}
          </Text>
          <textarea
            rows={3}
            value={dialog.resultSummary}
            onChange={(event: { currentTarget: { value: string } }) =>
              onUpdate({ resultSummary: event.currentTarget.value })
            }
          />
        </div>
      ) : null}
      {dialog.intent.action === 'handover' ? (
        <div className="plan-node-action-field">
          <Text type="secondary" className="plan-node-action-label">
            {translate('planDetailActionDialogCommentLabel')}
          </Text>
          <textarea
            rows={3}
            value={dialog.comment}
            onChange={(event: { currentTarget: { value: string } }) =>
              onUpdate({ comment: event.currentTarget.value })
            }
          />
        </div>
      ) : null}
      <Space size="small">
        <Button
          type="primary"
          size="small"
          onClick={onSubmit}
          loading={submitting}
          disabled={!dialog.operatorId || (dialog.intent.action === 'handover' && !dialog.assigneeId)}
        >
          {translate('planDetailActionDialogConfirm')}
        </Button>
        <Button type="default" size="small" onClick={onCancel} disabled={submitting}>
          {translate('planDetailActionDialogCancel')}
        </Button>
      </Space>
    </div>
  );
}

type ReminderEditorFormProps = {
  editor: ReminderEditorState;
  translate: LocalizationState['translate'];
  onUpdate: (patch: Partial<ReminderEditorState>) => void;
  onCancel: () => void;
  onSubmit: () => void;
  submitting: boolean;
};

function ReminderEditorForm({
  editor,
  translate,
  onUpdate,
  onCancel,
  onSubmit,
  submitting,
}: ReminderEditorFormProps) {
  return (
    <div className="plan-reminder-editor">
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditActiveLabel')}
        </Text>
        <input
          type="checkbox"
          checked={editor.active}
          onChange={(event: { currentTarget: { checked: boolean } }) =>
            onUpdate({ active: event.currentTarget.checked })
          }
        />
      </div>
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditOffsetLabel')}
        </Text>
        <input
          type="number"
          value={editor.offsetMinutes}
          onChange={(event: { currentTarget: { value: string } }) => {
            const next = Number(event.currentTarget.value);
            onUpdate({ offsetMinutes: Number.isNaN(next) ? 0 : next });
          }}
        />
      </div>
      <Space size="small">
        <Button type="primary" size="small" onClick={onSubmit} loading={submitting}>
          {translate('planDetailReminderEditConfirm')}
        </Button>
        <Button type="default" size="small" onClick={onCancel} disabled={submitting}>
          {translate('planDetailReminderEditCancel')}
        </Button>
      </Space>
    </div>
  );
}

type NodeMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  nodeLookup: Map<string, PlanNodeWithPath>;
  onRetry?: () => void;
  onEdit?: () => void;
};

type ReminderMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  reminders: PlanReminderSummary[];
  onRetry?: () => void;
  onEdit?: () => void;
};

type OperatorSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
  currentUserName: string | null;
};

type AssigneeSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
};

function renderNodeMutationHelper({
  mutation,
  translate,
  nodeLookup,
  onRetry,
  onEdit,
}: NodeMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'node') {
    return null;
  }
  const entry = nodeLookup.get(context.nodeId);
  const nodeName = entry?.node.name ?? context.nodeId;
  const actionLabel = translate(ACTION_LABEL_KEY[context.action]);
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailActionProcessing', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailActionSuccess', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'error':
      const nodeActions: ReactNode[] = [];
      if (onRetry) {
        nodeActions.push(
          <Button key="retry" type="primary" size="small" onClick={onRetry}>
            {translate('planDetailActionRetry')}
          </Button>
        );
      }
      if (onEdit) {
        nodeActions.push(
          <Button key="edit" size="small" onClick={onEdit}>
            {translate('planDetailActionRetryEdit')}
          </Button>
        );
      }
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailActionError', {
            action: actionLabel,
            node: nodeName,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
          action={
            nodeActions.length > 0 ? <Space size="small">{nodeActions}</Space> : undefined
          }
        />
      );
    default:
      return null;
  }
}

function renderReminderMutationHelper({
  mutation,
  translate,
  reminders,
  onRetry,
  onEdit,
}: ReminderMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'reminder') {
    return null;
  }
  const reminder = reminders.find((item) => item.id === context.reminderId) ?? null;
  const channelLabel = reminder
    ? translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])
    : context.reminderId;
  const actionLabel = translate(
    context.action === 'edit'
      ? 'planDetailReminderActionEdit'
      : 'planDetailReminderActionToggle'
  );
  const offsetLabel = reminder
    ? translate('planDetailReminderOffsetMinutes', { minutes: reminder.offsetMinutes })
    : '';
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailReminderProcessing', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailReminderSuccess', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'error':
      const reminderActions: ReactNode[] = [];
      if (onRetry) {
        reminderActions.push(
          <Button key="retry" type="primary" size="small" onClick={onRetry}>
            {translate('planDetailReminderRetry')}
          </Button>
        );
      }
      if (onEdit && context.action === 'edit') {
        reminderActions.push(
          <Button key="edit" size="small" onClick={onEdit}>
            {translate('planDetailReminderRetryEdit')}
          </Button>
        );
      }
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailReminderError', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
          action={
            reminderActions.length > 0 ? (
              <Space size="small">{reminderActions}</Space>
            ) : undefined
          }
        />
      );
    default:
      return null;
  }
}


function getDefaultOperatorId({
  detail,
  node,
  currentUserName,
}: OperatorSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  if (currentUserName) {
    const currentUser = participants.find((participant) => participant.name === currentUserName);
    if (currentUser) {
      return currentUser.id;
    }
  }
  if (node?.assignee?.id) {
    return node.assignee.id;
  }
  if (detail) {
    const ownerMatch = participants.find((participant) => participant.name === detail.owner);
    if (ownerMatch) {
      return ownerMatch.id;
    }
  }
  return participants[0]?.id ?? null;
}

function getDefaultAssigneeId({ detail, node }: AssigneeSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  const currentAssignee = node?.assignee?.id ?? null;
  const fallback = participants.find((participant) => participant.id !== currentAssignee);
  return fallback?.id ?? currentAssignee ?? participants[0]?.id ?? null;
}

type ReminderEditorFormProps = {
  editor: ReminderEditorState;
  translate: LocalizationState['translate'];
  onUpdate: (patch: Partial<ReminderEditorState>) => void;
  onCancel: () => void;
  onSubmit: () => void;
  submitting: boolean;
};

function ReminderEditorForm({
  editor,
  translate,
  onUpdate,
  onCancel,
  onSubmit,
  submitting,
}: ReminderEditorFormProps) {
  return (
    <div className="plan-reminder-editor">
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditActiveLabel')}
        </Text>
        <input
          type="checkbox"
          checked={editor.active}
          onChange={(event: { currentTarget: { checked: boolean } }) =>
            onUpdate({ active: event.currentTarget.checked })
          }
        />
      </div>
      <div className="plan-reminder-editor-field">
        <Text type="secondary" className="plan-reminder-editor-label">
          {translate('planDetailReminderEditOffsetLabel')}
        </Text>
        <input
          type="number"
          value={editor.offsetMinutes}
          onChange={(event: { currentTarget: { value: string } }) => {
            const next = Number(event.currentTarget.value);
            onUpdate({ offsetMinutes: Number.isNaN(next) ? 0 : next });
          }}
        />
      </div>
      <Space size="small">
        <Button type="primary" size="small" onClick={onSubmit} loading={submitting}>
          {translate('planDetailReminderEditConfirm')}
        </Button>
        <Button type="default" size="small" onClick={onCancel} disabled={submitting}>
          {translate('planDetailReminderEditCancel')}
        </Button>
      </Space>
    </div>
  );
}

type NodeMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  nodeLookup: Map<string, PlanNodeWithPath>;
};

type ReminderMutationHelperOptions = {
  mutation: PlanDetailMutationState;
  translate: LocalizationState['translate'];
  reminders: PlanReminderSummary[];
};

type OperatorSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
  currentUserName: string | null;
};

type AssigneeSelectionOptions = {
  detail: PlanDetail | null;
  node: PlanNode | null;
};

function renderNodeMutationHelper({
  mutation,
  translate,
  nodeLookup,
}: NodeMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'node') {
    return null;
  }
  const entry = nodeLookup.get(context.nodeId);
  const nodeName = entry?.node.name ?? context.nodeId;
  const actionLabel = translate(ACTION_LABEL_KEY[context.action]);
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailActionProcessing', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailActionSuccess', {
            action: actionLabel,
            node: nodeName,
          })}
        />
      );
    case 'error':
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailActionError', {
            action: actionLabel,
            node: nodeName,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
        />
      );
    default:
      return null;
  }
}

function renderReminderMutationHelper({
  mutation,
  translate,
  reminders,
}: ReminderMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'reminder') {
    return null;
  }
  const reminder = reminders.find((item) => item.id === context.reminderId) ?? null;
  const channelLabel = reminder
    ? translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])
    : context.reminderId;
  const actionLabel = translate(
    context.action === 'edit'
      ? 'planDetailReminderActionEdit'
      : 'planDetailReminderActionToggle'
  );
  const offsetLabel = reminder
    ? translate('planDetailReminderOffsetMinutes', { minutes: reminder.offsetMinutes })
    : '';
  const errorDetail = formatApiErrorMessage(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailReminderProcessing', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailReminderSuccess', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'error':
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailReminderError', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
        />
      );
    default:
      return null;
  }
}

function getDefaultOperatorId({
  detail,
  node,
  currentUserName,
}: OperatorSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  if (currentUserName) {
    const currentUser = participants.find((participant) => participant.name === currentUserName);
    if (currentUser) {
      return currentUser.id;
    }
  }
  if (node?.assignee?.id) {
    return node.assignee.id;
  }
  if (detail) {
    const ownerMatch = participants.find((participant) => participant.name === detail.owner);
    if (ownerMatch) {
      return ownerMatch.id;
    }
  }
  return participants[0]?.id ?? null;
}

function getDefaultAssigneeId({ detail, node }: AssigneeSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  const currentAssignee = node?.assignee?.id ?? null;
  const fallback = participants.find((participant) => participant.id !== currentAssignee);
  return fallback?.id ?? currentAssignee ?? participants[0]?.id ?? null;
}

function renderReminderMutationHelper({
  mutation,
  translate,
  reminders,
}: ReminderMutationHelperOptions): ReactNode {
  const context = mutation.context;
  if (!context || context.type !== 'reminder') {
    return null;
  }
  const reminder = reminders.find((item) => item.id === context.reminderId) ?? null;
  const channelLabel = reminder
    ? translate(PLAN_REMINDER_CHANNEL_LABEL[reminder.channel])
    : context.reminderId;
  const actionLabel = translate(
    context.action === 'edit'
      ? 'planDetailReminderActionEdit'
      : 'planDetailReminderActionToggle'
  );
  const offsetLabel = reminder
    ? translate('planDetailReminderOffsetMinutes', { minutes: reminder.offsetMinutes })
    : '';
  const errorDetail = describeApiError(mutation.error, translate);
  switch (mutation.status) {
    case 'loading':
      return (
        <Alert
          type="info"
          showIcon
          message={translate('planDetailReminderProcessing', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'success':
      return (
        <Alert
          type="success"
          showIcon
          message={translate('planDetailReminderSuccess', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
          })}
        />
      );
    case 'error':
      return (
        <Alert
          type="error"
          showIcon
          message={translate('planDetailReminderError', {
            action: actionLabel,
            channel: channelLabel,
            offset: offsetLabel,
            error: errorDetail ?? translate('commonStateErrorDescription'),
          })}
        />
      );
    default:
      return null;
  }
}

function describeApiError(error: ApiError | null, translate: LocalizationState['translate']): string | null {
  if (!error) {
    return null;
  }
  if (error.type === 'status') {
    return translate('backendErrorStatus', { status: error.status });
  }
  return translate('backendErrorNetwork');
}

function getDefaultOperatorId({
  detail,
  node,
  currentUserName,
}: OperatorSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  if (currentUserName) {
    const currentUser = participants.find((participant) => participant.name === currentUserName);
    if (currentUser) {
      return currentUser.id;
    }
  }
  if (node?.assignee?.id) {
    return node.assignee.id;
  }
  if (detail) {
    const ownerMatch = participants.find((participant) => participant.name === detail.owner);
    if (ownerMatch) {
      return ownerMatch.id;
    }
  }
  return participants[0]?.id ?? null;
}

function getDefaultAssigneeId({ detail, node }: AssigneeSelectionOptions): string | null {
  const participants = detail?.participants ?? [];
  const currentAssignee = node?.assignee?.id ?? null;
  const fallback = participants.find((participant) => participant.id !== currentAssignee);
  return fallback?.id ?? currentAssignee ?? participants[0]?.id ?? null;
}

const ACTION_LABEL_KEY: Record<PlanNodeActionType, UiMessageKey> = {
  start: 'planDetailActionStart',
  complete: 'planDetailActionComplete',
  handover: 'planDetailActionHandover',
};

type ReminderActionIntent = {
  reminderId: string;
  action: 'edit' | 'toggle';
  channel: PlanReminderChannel;
  offset: number;
};
