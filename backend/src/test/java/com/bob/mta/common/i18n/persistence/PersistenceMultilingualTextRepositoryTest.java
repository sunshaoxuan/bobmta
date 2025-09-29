package com.bob.mta.common.i18n.persistence;

import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextRecord;
import com.bob.mta.common.i18n.MultilingualTextScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceMultilingualTextRepositoryTest {

    @Mock
    private MultilingualTextMapper mapper;

    private PersistenceMultilingualTextRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PersistenceMultilingualTextRepository(mapper);
    }

    @Test
    void saveShouldInsertWhenUpdateReturnsZero() {
        MultilingualText text = MultilingualText.of("ja-JP", Map.of(
                "ja-JP", "value-ja",
                "zh-CN", "value-zh"
        ));
        MultilingualTextRecord record = new MultilingualTextRecord(
                MultilingualTextScope.TAG_DEFINITION,
                "tag-1",
                "name",
                text
        );
        when(mapper.update(any(MultilingualTextEntity.class))).thenReturn(0);

        repository.save(record);

        ArgumentCaptor<MultilingualTextEntity> captor = ArgumentCaptor.forClass(MultilingualTextEntity.class);
        verify(mapper).update(captor.capture());
        verify(mapper).insert(captor.getValue());
        MultilingualTextEntity entity = captor.getValue();
        assertThat(entity.scope()).isEqualTo(MultilingualTextScope.TAG_DEFINITION);
        assertThat(entity.entityId()).isEqualTo("tag-1");
        assertThat(entity.field()).isEqualTo("name");
        assertThat(entity.defaultLocale()).isEqualTo("ja-jp");
        assertThat(entity.translations()).containsEntry("ja-jp", "value-ja")
                .containsEntry("zh-cn", "value-zh");
        assertThat(entity.updatedAt()).isNotNull();
    }

    @Test
    void saveShouldOnlyUpdateWhenRowExists() {
        MultilingualText text = MultilingualText.of("ja-JP", Map.of("ja-JP", "value-ja"));
        MultilingualTextRecord record = new MultilingualTextRecord(
                MultilingualTextScope.TEMPLATE_DEFINITION,
                "template-1",
                "subject",
                text
        );
        when(mapper.update(any(MultilingualTextEntity.class))).thenReturn(1);

        repository.save(record);

        verify(mapper).update(any(MultilingualTextEntity.class));
        verify(mapper, never()).insert(any(MultilingualTextEntity.class));
    }

    @Test
    void findShouldReturnEmptyWhenMissing() {
        when(mapper.find(MultilingualTextScope.TEMPLATE_DEFINITION, "missing", "name")).thenReturn(null);

        Optional<MultilingualTextRecord> result = repository.find(
                MultilingualTextScope.TEMPLATE_DEFINITION, "missing", "name");

        assertThat(result).isEmpty();
    }

    @Test
    void findShouldReturnRecordWhenPresent() {
        MultilingualTextEntity entity = new MultilingualTextEntity(
                MultilingualTextScope.TEMPLATE_DEFINITION,
                "template-2",
                "content",
                "ja-jp",
                Map.of("ja-jp", "value-ja", "zh-cn", "value-zh"),
                OffsetDateTime.now()
        );
        when(mapper.find(MultilingualTextScope.TEMPLATE_DEFINITION, "template-2", "content"))
                .thenReturn(entity);

        Optional<MultilingualTextRecord> result = repository.find(
                MultilingualTextScope.TEMPLATE_DEFINITION, "template-2", "content");

        assertThat(result).isPresent();
        MultilingualTextRecord record = result.orElseThrow();
        assertThat(record.getScope()).isEqualTo(MultilingualTextScope.TEMPLATE_DEFINITION);
        assertThat(record.getEntityId()).isEqualTo("template-2");
        assertThat(record.getField()).isEqualTo("content");
        assertThat(record.getText().getDefaultLocale()).isEqualTo("ja-jp");
        assertThat(record.getText().getTranslations()).containsEntry("zh-cn", "value-zh");
    }
}
