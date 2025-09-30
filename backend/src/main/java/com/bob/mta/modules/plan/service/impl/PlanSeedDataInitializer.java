package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.plan.domain.PlanNodeActionType;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.repository.PlanRepository;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.plan.service.command.CreatePlanCommand;
import com.bob.mta.modules.plan.service.command.PlanNodeCommand;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(InMemoryPlanRepository.class)
public class PlanSeedDataInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final PlanService planService;

    public PlanSeedDataInitializer(PlanRepository planRepository, PlanService planService) {
        this.planRepository = planRepository;
        this.planService = planService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!planRepository.findAll().isEmpty()) {
            return;
        }
        planService.createPlan(primaryPlan());
        planService.createPlan(secondaryPlan());
    }

    private CreatePlanCommand primaryPlan() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);
        OffsetDateTime end = start.plusHours(4);
        return new CreatePlanCommand(
                "tenant-001",
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_PRIMARY_DESCRIPTION),
                "cust-001",
                "admin",
                start,
                end,
                "Asia/Tokyo",
                List.of("admin", "operator"),
                List.of(
                        new PlanNodeCommand(
                                null,
                                Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_TITLE),
                                "REMOTE",
                                "admin",
                                1,
                                60,
                                PlanNodeActionType.REMOTE,
                                100,
                                "remote-template-1",
                                Localization.text(LocalizationKeys.Seeds.PLAN_NODE_BACKUP_DESCRIPTION),
                                List.of()
                        ),
                        new PlanNodeCommand(
                                null,
                                Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_TITLE),
                                "EMAIL",
                                "operator",
                                2,
                                15,
                                PlanNodeActionType.EMAIL,
                                100,
                                "email-template-1",
                                Localization.text(LocalizationKeys.Seeds.PLAN_NODE_NOTIFY_DESCRIPTION),
                                List.of()
                        )
                )
        );
    }

    private CreatePlanCommand secondaryPlan() {
        OffsetDateTime start = OffsetDateTime.now().plusWeeks(1);
        OffsetDateTime end = start.plusHours(6);
        return new CreatePlanCommand(
                "tenant-001",
                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_TITLE),
                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_DESCRIPTION),
                "cust-002",
                "operator",
                start,
                end,
                "Asia/Tokyo",
                List.of("operator"),
                List.of(
                        new PlanNodeCommand(
                                null,
                                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_NODE_TITLE),
                                "CHECKLIST",
                                "operator",
                                1,
                                180,
                                PlanNodeActionType.NONE,
                                100,
                                null,
                                Localization.text(LocalizationKeys.Seeds.PLAN_SECONDARY_NODE_DESCRIPTION),
                                List.of()
                        )
                )
        );
    }
}
