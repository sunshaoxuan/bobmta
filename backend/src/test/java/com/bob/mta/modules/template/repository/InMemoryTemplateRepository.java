package com.bob.mta.modules.template.repository;

import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.persistence.TemplateEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTemplateRepository implements TemplateRepository {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, TemplateEntity> storage = new ConcurrentHashMap<>();

    @Override
    public List<TemplateEntity> findAll(TemplateType type) {
        return storage.values().stream()
                .filter(entity -> type == null || entity.getType() == type)
                .sorted(Comparator.comparingLong(TemplateEntity::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<TemplateEntity> findById(long id) {
        return Optional.ofNullable(storage.get(id)).map(this::copy);
    }

    @Override
    public TemplateEntity insert(TemplateEntity entity) {
        long id = idGenerator.incrementAndGet();
        entity.setId(id);
        storage.put(id, copy(entity));
        return copy(entity);
    }

    @Override
    public TemplateEntity update(TemplateEntity entity) {
        if (entity.getId() == null || !storage.containsKey(entity.getId())) {
            throw new IllegalArgumentException("Template not found: " + entity.getId());
        }
        storage.put(entity.getId(), copy(entity));
        return copy(entity);
    }

    @Override
    public void delete(long id) {
        storage.remove(id);
    }

    private TemplateEntity copy(TemplateEntity source) {
        TemplateEntity target = new TemplateEntity();
        target.setId(source.getId());
        target.setType(source.getType());
        target.setToRecipients(source.getToRecipients() == null ? List.of()
                : new ArrayList<>(source.getToRecipients()));
        target.setCcRecipients(source.getCcRecipients() == null ? List.of()
                : new ArrayList<>(source.getCcRecipients()));
        target.setEndpoint(source.getEndpoint());
        target.setEnabled(source.isEnabled());
        target.setNameDefaultLocale(source.getNameDefaultLocale());
        target.setNameTranslations(copyMap(source.getNameTranslations()));
        target.setSubjectDefaultLocale(source.getSubjectDefaultLocale());
        target.setSubjectTranslations(copyMap(source.getSubjectTranslations()));
        target.setContentDefaultLocale(source.getContentDefaultLocale());
        target.setContentTranslations(copyMap(source.getContentTranslations()));
        target.setDescriptionDefaultLocale(source.getDescriptionDefaultLocale());
        target.setDescriptionTranslations(copyMap(source.getDescriptionTranslations()));
        OffsetDateTime createdAt = source.getCreatedAt();
        OffsetDateTime updatedAt = source.getUpdatedAt();
        target.setCreatedAt(createdAt);
        target.setUpdatedAt(updatedAt);
        return target;
    }

    private Map<String, String> copyMap(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(source);
    }
}
