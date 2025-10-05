# Plan Persistence Roadmap (Phase IV)

## Current State Snapshot
- Domain services����Ҫ���� InMemoryPlanService���־û��ִ���PlanAggregateMapper��PlanPersistenceAnalyticsRepository �ȣ�����ͳ�ƺͲ�ѯ�㱻ʹ�á�
- Flyway V7 introduces `mt_plan_action_history` plus the MyBatis mapper/repository pair so action automation audits persist alongside existing plan aggregates.
- PostgreSQL schema ��ͨ�� Flyway V1�CV6 ������Plan ģ��� CRUD / Reminder / Timeline ��ṹ�߱�������������
- Testcontainers ���ɲ��Ը��� PlanAggregateMapper ��ɸѡ���ڵ㡢���ѡ���������Ⱥ��� SQL����ҵ�����δ�л����ִ�ʵ�֡�

## Gap Assessment
1. **����ʵ��ȱʧ**����Ҫʵ�� PersistencePlanService���Բִ�Ϊ����Դ��ͬʱ�������������߼���������ȡ�����ڵ�ִ�С���Ƶȣ���
2. **����߽�����**���־û�ʵ����Ը��Ӳ�����������ִ�С����Ѹ��£�����һ�µ�����ģ�壬ȡ���ڴ�̬��ԭ�Ӳ������ɸ��� Spring @Transactional + MyBatis ӳ�䡣
3. **�����֪ͨ����**��InMemory ������д����ơ�����֪ͨ��Ǩ�ƺ��豣֤ PersistencePlanService ���ڳ־û������ɹ��������ͬ����Ƽ�¼��֪ͨ��������
4. **�����벢��**��Ŀǰ�Ļ�������� InMemory ����ά����Ǩ��ʱ�������ƻ���д�����Ƿ�������ڻ������ݿ���ͼ/Redis �����
5. **�ع����**����Ҫ���� API �� / �������ϲ��ԣ���֤�־û���Ϊ��ԭ���ڴ�ʵ�ֵ�һ���ԣ�״̬��������Ԥ����ʱ���ߵȣ���

## Implementation Steps
1. **��ȡ�����߼�**
   - �� InMemoryPlanService �е�����ת�������Ѽ��㡢ʱ�������ɵȴ�������ȡ���ɸ��õ� helper������ PlanDomainSupport����
   - Ϊ֪ͨ��������Ƽ�¼�����ӿڻ���ԣ�ʹ�ڴ���־û�ʵ�ֹ��á�
2. **���� PersistencePlanService**
   - �� PlanRepository��PlanAnalyticsRepository��TagRepository��CustomerRepository ����Ϊ������ͨ�� Spring @ConditionalOnBean(PlanAggregateMapper.class) ���
   - ʵ�� listPlans/getPlan ʹ�� PlanAggregateMapper �ۺϽ����createPlan/updatePlan/publish/cancel ��д�������� mapper �� insert/update/delete��
   - ά���ڵ㡢���ѡ������ĳ־û����������� mapper ���� insertNodes, insertReminderRules �ȣ���
3. **���������**
   - ��д����������ʹ�� @Transactional��ȷ�� Plan��Node��Reminder��Audit �Ŀ�����Ҫôȫ���ɹ�Ҫô�ع���
   - ���д���������� AuditRecorder���ڳ־û������ɹ����¼���ա�
4. **֪ͨ����**
   - ��֪ͨ�����߼���ȡΪ NotificationGateway �ӿڣ�InMemoryPlanService ���µ� PersistencePlanService ����ʵ�֣��ڲ��������õ��� ApiNotificationAdapter/EmailNotificationAdapter �ȡ�
5. **Ǩ��/ѡ���߼�**
   - �� Spring �����и��� PlanAggregateMapper Bean �Ƿ���ھ������� PersistencePlanService �� InMemoryPlanService������������Ϊ���������ݿ�ĵ�Ԫ���ԣ���
   - README �벿���ĵ�ͬ�����£�˵��������ó־û�ʵ�֡�
6. **�ع����**
   - �������񼶱𼯳ɲ��ԣ�ʹ�� Testcontainers + Mock ֪ͨ/��ƣ���֤�ؼ�����������/����/ȡ�����ڵ�ִ�С����Ѹ��¡�PlanBoard/Analytics��
   - API �㣨MockMvc����� /plans ϵ�нӿڲ���־û���ϲ��ԡ�

## Exit Criteria
- ���� PlanService API ���ɳ־û�ʵ��֧�ţ�InMemory �汾����������/�����ݿ�ģʽ��
- Testcontainers �׼����ǣ��ƻ� CRUD + �ڵ�ִ�� + Reminders + Analytics + Board��
- README / Runbook ���£���¼���ó־û������ݿ�Ǩ�ơ����˲��ԡ�
- CI Pipeline ִ�� mvn verify ʱĬ���߳־û�ʵ�֣������� PostgreSQL Testcontainers����
