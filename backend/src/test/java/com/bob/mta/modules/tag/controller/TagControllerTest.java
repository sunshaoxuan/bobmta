package com.bob.mta.modules.tag.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.customer.service.impl.InMemoryCustomerService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.plan.repository.InMemoryPlanRepository;
import com.bob.mta.modules.plan.service.impl.InMemoryPlanService;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.dto.AssignTagRequest;
import com.bob.mta.modules.tag.dto.CreateTagRequest;
import com.bob.mta.modules.tag.dto.TagResponse;
import com.bob.mta.modules.tag.service.impl.InMemoryTagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TagControllerTest {

    private TagController controller;
    private InMemoryPlanService planService;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        InMemoryTagService tagService = new InMemoryTagService();
        InMemoryCustomerService customerService = new InMemoryCustomerService();
        messageResolver = TestMessageResolverFactory.create();
        planService = new InMemoryPlanService(new InMemoryFileService(), new InMemoryPlanRepository(), messageResolver);
        AuditRecorder recorder = new AuditRecorder(new InMemoryAuditService(), new ObjectMapper());
        controller = new TagController(tagService, customerService, planService, recorder, messageResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldCreateTag() {
        CreateTagRequest request = new CreateTagRequest();
        request.setName("urgent");
        request.setColor("#F5222D");
        request.setIcon("AlertOutlined");
        request.setScope(com.bob.mta.modules.tag.domain.TagScope.CUSTOMER);
        request.setEnabled(true);

        ApiResponse<TagResponse> response = controller.create(request);
        assertThat(response.getData().getName()).isEqualTo("urgent");
    }

    @Test
    void shouldAssignTagToPlan() {
        var created = controller.create(buildRequest("plan", com.bob.mta.modules.tag.domain.TagScope.PLAN));
        AssignTagRequest assign = new AssignTagRequest();
        assign.setEntityType(TagEntityType.PLAN);
        assign.setEntityId(planService.listPlans(null, null, null, null, null, null).get(0).getId());

        controller.assign(created.getData().getId(), assign);

        assertThat(controller.listAssignments(created.getData().getId()).getData()).hasSize(1);
    }

    private CreateTagRequest buildRequest(String name, com.bob.mta.modules.tag.domain.TagScope scope) {
        CreateTagRequest request = new CreateTagRequest();
        request.setName(name);
        request.setColor("#000000");
        request.setIcon("TagOutlined");
        request.setScope(scope);
        request.setEnabled(true);
        return request;
    }
}
