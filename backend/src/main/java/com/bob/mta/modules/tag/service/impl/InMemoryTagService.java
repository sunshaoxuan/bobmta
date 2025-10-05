package com.bob.mta.modules.tag.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextScope;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.service.TagService;
import com.bob.mta.modules.tag.persistence.TagMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnMissingBean(TagMapper.class)
public class InMemoryTagService implements TagService {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, TagDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<Long, Set<TagAssignment>> assignmentIndex = new ConcurrentHashMap<>();
    private final MultilingualTextService multilingualTextService;

    public InMemoryTagService(MultilingualTextService multilingualTextService) {
        this.multilingualTextService = multilingualTextService;
        seedDefaults();
    }

    private void seedDefaults() {
        create(seedText(LocalizationKeys.Seeds.TAG_PRIORITY_NAME), "#FF5722", "StarOutlined", TagScope.CUSTOMER, null, true);
        create(seedText(LocalizationKeys.Seeds.TAG_PLAN_ANNUAL_NAME), "#1890FF", "CalendarOutlined", TagScope.PLAN, null, true);
    }

    private MultilingualText seedText(String code) {
        Locale defaultLocale = Localization.getDefaultLocale();
        return MultilingualText.of(defaultLocale.toLanguageTag(), Map.of(
                defaultLocale.toLanguageTag(), Localization.text(defaultLocale, code),
                Locale.CHINA.toLanguageTag(), Localization.text(Locale.CHINA, code)
        ));
    }

    @Override
    public List<TagDefinition> list(TagScope scope, Locale locale) {
        String localeTag = locale == null ? null : locale.toLanguageTag();
        Comparator<TagDefinition> comparator = Comparator.comparing(
                def -> def.getName().getValueOrDefault(localeTag),
                String.CASE_INSENSITIVE_ORDER);
        return definitions.values().stream()
                .filter(def -> scope == null || def.getScope() == scope || def.getScope() == TagScope.BOTH)
                .sorted(comparator)
                .toList();
    }

    @Override
    public TagDefinition getById(long id, Locale locale) {
        return require(id);
    }

    @Override
    public TagDefinition create(MultilingualText name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        long id = idGenerator.incrementAndGet();
        TagDefinition definition = new TagDefinition(id, name, color, icon, scope, applyRule, enabled, OffsetDateTime.now());
        definitions.put(id, definition);
        assignmentIndex.putIfAbsent(id, ConcurrentHashMap.newKeySet());
        multilingualTextService.upsert(MultilingualTextScope.TAG_DEFINITION, String.valueOf(id), "name", name);
        return definition;
    }

    @Override
    public TagDefinition update(long id, MultilingualText name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        TagDefinition definition = require(id);
        TagDefinition updated = definition
                .withName(name)
                .withColor(color)
                .withIcon(icon)
                .withScope(scope)
                .withApplyRule(applyRule)
                .withEnabled(enabled);
        definitions.put(id, updated);
        multilingualTextService.upsert(MultilingualTextScope.TAG_DEFINITION, String.valueOf(id), "name", name);
        return updated;
    }

    @Override
    public void delete(long id) {
        require(id);
        definitions.remove(id);
        assignmentIndex.remove(id);
    }

    @Override
    public TagAssignment assign(long tagId, TagEntityType entityType, String entityId) {
        TagDefinition definition = require(tagId);
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
    public List<TagDefinition> findByEntity(TagEntityType entityType, String entityId, Locale locale) {
        String localeTag = locale == null ? null : locale.toLanguageTag();
        return assignmentIndex.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(assignment -> assignment.getEntityType() == entityType && assignment.getEntityId().equals(entityId)))
                .map(entry -> definitions.get(entry.getKey()))
                .filter(def -> def != null)
                .sorted((a, b) -> a.getName().getValueOrDefault(localeTag)
                        .compareToIgnoreCase(b.getName().getValueOrDefault(localeTag)))
                .toList();
    }

    private TagDefinition require(long id) {
        TagDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return definition;
    }
}
