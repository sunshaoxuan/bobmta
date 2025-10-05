package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.notification.ApiNotificationAdapter;
import com.bob.mta.modules.notification.EmailNotificationAdapter;
import com.bob.mta.modules.notification.InstantMessageNotificationAdapter;
import com.bob.mta.modules.plan.persistence.PlanAggregateMapper;
import com.bob.mta.modules.plan.repository.PlanActionHistoryRepository;
import com.bob.mta.modules.plan.repository.PlanAggregateRepository;
import com.bob.mta.modules.plan.repository.PlanAnalyticsRepository;
import com.bob.mta.modules.template.service.TemplateService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Service
@Primary
@ConditionalOnBean(PlanAggregateMapper.class)
@Transactional(readOnly = true)
public class PersistencePlanService extends InMemoryPlanService {

    public PersistencePlanService(FileService fileService,
                                  PlanAggregateRepository planRepository,
                                  PlanAnalyticsRepository planAnalyticsRepository,
                                  PlanActionHistoryRepository actionHistoryRepository,
                                  TemplateService templateService,
                                  EmailNotificationAdapter emailNotificationAdapter,
                                  InstantMessageNotificationAdapter instantMessageNotificationAdapter,
                                  ApiNotificationAdapter apiNotificationAdapter,
                                  MessageResolver messageResolver) {
        super(fileService, planRepository, planAnalyticsRepository, actionHistoryRepository,
                templateService, emailNotificationAdapter, instantMessageNotificationAdapter,
                apiNotificationAdapter, messageResolver);
    }
}
