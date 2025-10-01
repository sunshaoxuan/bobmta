-- -----------------------------------------------------------------------------
-- BOB MTA Maintenance Assistants - 示例数据
-- 用于本地开发或测试环境快速演示计划聚合数据结构。
-- 如需恢复空库，请先执行 schema.sql，再按需导入本文件。
-- -----------------------------------------------------------------------------

INSERT INTO mt_plan (plan_id, tenant_id, customer_id, owner_id, title, description, status,
                     planned_start_time, planned_end_time, timezone, created_at, updated_at)
VALUES
    ('PLAN-00000001', 'tenant-demo', 'customer-a', 'owner-alpha', '预防性巡检-东京数据中心',
     '检查冷却系统与配电柜，确认巡检清单。', 'SCHEDULED',
     NOW() + INTERVAL '1 day', NOW() + INTERVAL '2 days', 'Asia/Tokyo', NOW(), NOW()),
    ('PLAN-00000002', 'tenant-demo', 'customer-b', 'owner-beta', '应急演练-灾备切换',
     '模拟核心服务故障并在 30 分钟内完成灾备切换演练。', 'IN_PROGRESS',
     NOW() - INTERVAL '1 hour', NOW() + INTERVAL '5 hours', 'Asia/Shanghai', NOW(), NOW()),
    ('PLAN-00000003', 'tenant-demo', 'customer-a', 'owner-gamma', '巡检总结与复盘',
     '收集巡检日志并输出问题清单，准备向客户汇报。', 'COMPLETED',
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 'Asia/Shanghai', NOW(), NOW()),
    ('PLAN-00000004', 'tenant-demo', 'customer-c', 'owner-delta', '多团队协作演练',
     '验证跨团队协作和多渠道提醒。', 'SCHEDULED',
     NOW() + INTERVAL '3 days', NOW() + INTERVAL '4 days', 'Asia/Shanghai', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_participant (plan_id, participant_id) VALUES
    ('PLAN-00000001', 'user-ops-001'),
    ('PLAN-00000001', 'user-ops-002'),
    ('PLAN-00000002', 'user-ops-003'),
    ('PLAN-00000002', 'user-ops-004'),
    ('PLAN-00000003', 'user-ops-001'),
    ('PLAN-00000004', 'user-ops-005'),
    ('PLAN-00000004', 'user-ops-006')
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_node (plan_id, node_id, parent_node_id, name, type, assignee, order_index,
                           expected_duration_minutes, action_type, completion_threshold, action_ref, description)
VALUES
    ('PLAN-00000001', 'NODE-00000001', NULL, '冷却系统巡检', 'TASK', 'user-ops-001', 0, 60, 'MANUAL', 100, 'checklist-cooling', '按照表单巡检温控。'),
    ('PLAN-00000001', 'NODE-00000002', 'NODE-00000001', '配电柜巡检', 'TASK', 'user-ops-002', 1, 45, 'MANUAL', 100, 'checklist-power', '确认备用电源及警报。'),
    ('PLAN-00000002', 'NODE-00000003', NULL, '演练启动', 'TASK', 'user-ops-003', 0, 30, 'MANUAL', 100, 'drill-start', '宣布演练目标与角色分配。'),
    ('PLAN-00000002', 'NODE-00000004', 'NODE-00000003', '灾备切换', 'TASK', 'user-ops-004', 1, 45, 'API_CALL', 80, 'drill-switch', '执行灾备切换流程。'),
    ('PLAN-00000003', 'NODE-00000005', NULL, '巡检材料收集', 'TASK', 'user-ops-001', 0, 40, 'MANUAL', 100, 'review-collect', '整理巡检记录与附件。'),
    ('PLAN-00000004', 'NODE-00000006', NULL, '跨团队协调会', 'TASK', 'user-ops-005', 0, 50, 'MANUAL', 100, 'coordination-call', '协调内外部参与方。'),
    ('PLAN-00000004', 'NODE-00000007', 'NODE-00000006', '渠道联调', 'TASK', 'user-ops-006', 1, 70, 'API_CALL', 90, 'channel-sync', '校验跨渠道通知链路。')
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_node_execution (plan_id, node_id, status, start_time, end_time, operator_id, result_summary, execution_log)
VALUES
    ('PLAN-00000002', 'NODE-00000003', 'DONE', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '90 minutes', 'user-ops-003', '演练已启动', 'log-start'),
    ('PLAN-00000002', 'NODE-00000004', 'IN_PROGRESS', NOW() - INTERVAL '60 minutes', NULL, 'user-ops-004', '灾备切换进行中', 'log-switch'),
    ('PLAN-00000003', 'NODE-00000005', 'DONE', NOW() - INTERVAL '3 days', NOW() - INTERVAL '70 hours', 'user-ops-001', '巡检总结完成', 'log-review'),
    ('PLAN-00000004', 'NODE-00000006', 'PENDING', NULL, NULL, NULL, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_node_attachment (plan_id, node_id, file_id) VALUES
    ('PLAN-00000001', 'NODE-00000001', 'file-cooling-report'),
    ('PLAN-00000001', 'NODE-00000002', 'file-power-report'),
    ('PLAN-00000002', 'NODE-00000003', 'file-drill-briefing'),
    ('PLAN-00000004', 'NODE-00000006', 'file-coordination-minutes'),
    ('PLAN-00000004', 'NODE-00000007', 'file-channel-report')
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_activity (plan_id, activity_id, activity_type, occurred_at, actor_id, message_key, reference_id, attributes)
VALUES
    ('PLAN-00000002', 'ACT-00000001', 'PLAN_STARTED', NOW() - INTERVAL '2 hours', 'user-ops-003', 'plan.activity.planStarted', 'NODE-00000003', '{"shift":"A"}'),
    ('PLAN-00000002', 'ACT-00000002', 'NODE_COMPLETED', NOW() - INTERVAL '90 minutes', 'user-ops-003', 'plan.activity.nodeCompleted', 'NODE-00000003', '{"result":"ok"}'),
    ('PLAN-00000003', 'ACT-00000003', 'PLAN_COMPLETED', NOW() - INTERVAL '2 days', 'user-ops-001', 'plan.activity.planCompleted', NULL, '{"report":"delivered"}'),
    ('PLAN-00000004', 'ACT-00000004', 'PLAN_CREATED', NOW() - INTERVAL '1 day', 'owner-delta', 'plan.activity.planCreated', NULL, '{"createdBy":"owner-delta"}')
ON CONFLICT DO NOTHING;

INSERT INTO mt_plan_reminder_rule (plan_id, rule_id, trigger, offset_minutes, channels, template_id, recipients, description, active)
VALUES
    ('PLAN-00000001', 'REM-00000001', 'BEFORE_PLAN_START', 60, '["EMAIL"]', 'tmpl-plan-reminder', '["owner-alpha","user-ops-001"]', '开工前一小时提醒执行人', TRUE),
    ('PLAN-00000002', 'REM-00000002', 'BEFORE_PLAN_END', 15, '["EMAIL","IM"]', 'tmpl-node-reminder', '["owner-beta"]', '节点完成后通知负责人', TRUE),
    ('PLAN-00000004', 'REM-00000003', 'BEFORE_PLAN_START', 120, '["EMAIL","SMS"]', 'tmpl-plan-precheck', '["owner-delta","user-ops-006"]', '计划开始前两小时提醒全员', TRUE)
ON CONFLICT DO NOTHING;
