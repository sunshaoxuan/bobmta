package com.bob.mta.modules.tag.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.InMemoryMultilingualTextRepository;
import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTagServiceTest {

    private final InMemoryTagService tagService = new InMemoryTagService(new MultilingualTextService(new InMemoryMultilingualTextRepository()));

    @Test
    void shouldCreateAndRetrieveTag() {
        var created = tagService.create(text("inspection"), "#52C41A", "CheckCircleOutlined", TagScope.BOTH, null, true);
        var fetched = tagService.getById(created.getId(), Locale.JAPAN);
        assertThat(fetched.getName().getValueOrDefault("ja-JP")).isEqualTo("inspection");
    }

    @Test
    void shouldAssignTagToCustomer() {
        var created = tagService.create(text("critical"), "#FA8C16", "FireOutlined", TagScope.CUSTOMER, null, true);
        TagAssignment assignment = tagService.assign(created.getId(), TagEntityType.CUSTOMER, "cust-100");
        assertThat(tagService.listAssignments(created.getId())).contains(assignment);
        List<?> tags = tagService.findByEntity(TagEntityType.CUSTOMER, "cust-100", Locale.JAPAN);
        assertThat(tags).hasSize(1);
    }

    @Test
    void shouldRejectUnsupportedScope() {
        var created = tagService.create(text("plan-only"), "#1890FF", "CalendarOutlined", TagScope.PLAN, null, true);
        assertThatThrownBy(() -> tagService.assign(created.getId(), TagEntityType.CUSTOMER, "cust"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldFailWhenTagMissing() {
        assertThatThrownBy(() -> tagService.getById(9_999, Locale.JAPAN))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.TAG_NOT_FOUND.getDefaultMessage())
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TAG_NOT_FOUND);
    }

    private MultilingualText text(String value) {
        return MultilingualText.of("ja-JP", Map.of(
                "ja-JP", value,
                "zh-CN", value
        ));
    }
}
