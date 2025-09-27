package com.bob.mta.modules.tag.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTagServiceTest {

    private final InMemoryTagService tagService = new InMemoryTagService();

    @Test
    void shouldCreateAndRetrieveTag() {
        var created = tagService.create("巡检", "#52C41A", "CheckCircleOutlined", TagScope.BOTH, null, true);
        var fetched = tagService.getById(created.getId());
        assertThat(fetched.getName()).isEqualTo("巡检");
    }

    @Test
    void shouldAssignTagToCustomer() {
        var created = tagService.create("重点", "#FA8C16", "FireOutlined", TagScope.CUSTOMER, null, true);
        TagAssignment assignment = tagService.assign(created.getId(), TagEntityType.CUSTOMER, "cust-100");
        assertThat(tagService.listAssignments(created.getId())).contains(assignment);
        List<?> tags = tagService.findByEntity(TagEntityType.CUSTOMER, "cust-100");
        assertThat(tags).hasSize(1);
    }

    @Test
    void shouldRejectUnsupportedScope() {
        var created = tagService.create("仅计划", "#1890FF", "CalendarOutlined", TagScope.PLAN, null, true);
        assertThatThrownBy(() -> tagService.assign(created.getId(), TagEntityType.CUSTOMER, "cust"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
