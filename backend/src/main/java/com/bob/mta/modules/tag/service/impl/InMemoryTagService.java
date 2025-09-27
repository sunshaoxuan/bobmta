package com.bob.mta.modules.tag.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.service.TagService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryTagService implements TagService {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, TagDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<Long, Set<TagAssignment>> assignmentIndex = new ConcurrentHashMap<>();

    public InMemoryTagService() {
        seedDefaults();
    }

    private void seedDefaults() {
        create(Localization.text(LocalizationKeys.Seeds.TAG_PRIORITY_NAME), "#FF5722", "StarOutlined", TagScope.CUSTOMER, null, true);
        create(Localization.text(LocalizationKeys.Seeds.TAG_PLAN_ANNUAL_NAME), "#1890FF", "CalendarOutlined", TagScope.PLAN, null, true);
    }

    @Override
    public List<TagDefinition> list(TagScope scope) {
        if (scope == null) {
            return definitions.values().stream()
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toList();
        }
        return definitions.values().stream()
                .filter(def -> def.getScope() == scope || def.getScope() == TagScope.BOTH)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Override
    public TagDefinition getById(long id) {
        TagDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return definition;
    }

    @Override
    public TagDefinition create(String name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        long id = idGenerator.incrementAndGet();
        TagDefinition definition = new TagDefinition(id, name, color, icon, scope, applyRule, enabled, OffsetDateTime.now());
        definitions.put(id, definition);
        assignmentIndex.putIfAbsent(id, ConcurrentHashMap.newKeySet());
        return definition;
    }

    @Override
    public TagDefinition update(long id, String name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        TagDefinition definition = getById(id);
        TagDefinition updated = definition
                .withName(name)
                .withColor(color)
                .withIcon(icon)
                .withScope(scope)
                .withApplyRule(applyRule)
                .withEnabled(enabled);
        definitions.put(id, updated);
        return updated;
    }

    @Override
    public void delete(long id) {
        definitions.remove(id);
        assignmentIndex.remove(id);
    }

    @Override
    public TagAssignment assign(long tagId, TagEntityType entityType, String entityId) {
        TagDefinition definition = getById(tagId);
        if (!definition.getScope().supports(entityType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    Localization.text(LocalizationKeys.Errors.TAG_SCOPE_UNSUPPORTED));
        }
        Set<TagAssignment> assignments = assignmentIndex.computeIfAbsent(tagId, key -> ConcurrentHashMap.newKeySet());
        TagAssignment newAssignment = new TagAssignment(tagId, entityType, entityId);
        assignments.removeIf(existing -> existing.getEntityType() == entityType && existing.getEntityId().equals(entityId));
        assignments.add(newAssignment);
        return newAssignment;
    }

    @Override
    public void removeAssignment(long tagId, TagEntityType entityType, String entityId) {
        Set<TagAssignment> assignments = assignmentIndex.getOrDefault(tagId, Collections.emptySet());
        assignments.removeIf(existing -> existing.getEntityType() == entityType && existing.getEntityId().equals(entityId));
    }

    @Override
    public List<TagAssignment> listAssignments(long tagId) {
        Set<TagAssignment> assignments = assignmentIndex.get(tagId);
        if (assignments == null) {
            return List.of();
        }
        return new ArrayList<>(assignments);
    }

    @Override
    public List<TagDefinition> findByEntity(TagEntityType entityType, String entityId) {
        return assignmentIndex.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(assignment -> assignment.getEntityType() == entityType && assignment.getEntityId().equals(entityId)))
                .map(entry -> definitions.get(entry.getKey()))
                .filter(def -> def != null)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }
}
