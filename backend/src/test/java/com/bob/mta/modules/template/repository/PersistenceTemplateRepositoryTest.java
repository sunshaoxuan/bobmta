package com.bob.mta.modules.template.repository;

import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.persistence.TemplateEntity;
import com.bob.mta.modules.template.persistence.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PersistenceTemplateRepositoryTest {

    private TemplateMapper mapper;
    private PersistenceTemplateRepository repository;

    @BeforeEach
    void setUp() {
        mapper = mock(TemplateMapper.class);
        repository = new PersistenceTemplateRepository(mapper);
    }

    @Test
    void insertShouldReturnPersistedEntity() {
        TemplateEntity entity = sampleEntity(null);
        TemplateEntity persisted = sampleEntity(2001L);

        doAnswer(invocation -> {
            TemplateEntity argument = invocation.getArgument(0);
            argument.setId(2001L);
            return null;
        }).when(mapper).insert(any(TemplateEntity.class));
        when(mapper.findById(2001L)).thenReturn(persisted);

        TemplateEntity result = repository.insert(entity);

        assertThat(result.getId()).isEqualTo(2001L);
        assertThat(result.getNameDefaultLocale()).isEqualTo("en-US");
        verify(mapper).insert(entity);
        verify(mapper).findById(2001L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void insertShouldFailWhenIdNotReturned() {
        TemplateEntity entity = sampleEntity(null);

        assertThatThrownBy(() -> repository.insert(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Template insert");
        verify(mapper).insert(entity);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void updateShouldReturnLatestEntity() {
        TemplateEntity entity = sampleEntity(3001L);
        TemplateEntity refreshed = sampleEntity(3001L);
        refreshed.setEndpoint("https://updated.example.com");

        when(mapper.update(any(TemplateEntity.class))).thenReturn(1);
        when(mapper.findById(3001L)).thenReturn(refreshed);

        TemplateEntity result = repository.update(entity);

        assertThat(result.getEndpoint()).isEqualTo("https://updated.example.com");
        verify(mapper).update(entity);
        verify(mapper).findById(3001L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void updateShouldFailWhenNoRowsAffected() {
        TemplateEntity entity = sampleEntity(4001L);
        when(mapper.update(any(TemplateEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> repository.update(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Template update");
        verify(mapper).update(entity);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void findAllShouldDelegateToMapper() {
        TemplateEntity entity = sampleEntity(5001L);
        when(mapper.findAll(eq(TemplateType.EMAIL))).thenReturn(List.of(entity));

        List<TemplateEntity> results = repository.findAll(TemplateType.EMAIL);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(5001L);
        verify(mapper).findAll(TemplateType.EMAIL);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void deleteShouldDelegateToMapper() {
        repository.delete(6001L);

        verify(mapper).delete(6001L);
        verifyNoMoreInteractions(mapper);
    }

    private TemplateEntity sampleEntity(Long id) {
        TemplateEntity entity = new TemplateEntity();
        entity.setId(id);
        entity.setType(TemplateType.EMAIL);
        entity.setToRecipients(List.of("ops@example.com"));
        entity.setCcRecipients(List.of("audit@example.com"));
        entity.setEndpoint("https://webhook.example.com");
        entity.setEnabled(true);
        entity.setNameDefaultLocale("en-US");
        entity.setNameTranslations(Map.of("en-US", "Notice", "zh-CN", "通知"));
        entity.setSubjectDefaultLocale("en-US");
        entity.setSubjectTranslations(Map.of("en-US", "Subject", "zh-CN", "主题"));
        entity.setContentDefaultLocale("en-US");
        entity.setContentTranslations(Map.of("en-US", "Body", "zh-CN", "正文"));
        entity.setDescriptionDefaultLocale("en-US");
        entity.setDescriptionTranslations(Map.of("en-US", "Description", "zh-CN", "描述"));
        entity.setCreatedAt(OffsetDateTime.now().minusDays(1));
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }
}

