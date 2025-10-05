package com.bob.mta.modules.tag.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextScope;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.common.tenant.TenantContext;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.persistence.TagAssignmentEntity;
import com.bob.mta.modules.tag.persistence.TagDefinitionEntity;
import com.bob.mta.modules.tag.repository.TagRepository;
import com.bob.mta.modules.tag.service.TagService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnBean(TagRepository.class)
public class PersistenceTagService implements TagService {

    private final TagRepository tagRepository;
    private final MultilingualTextService multilingualTextService;
    private final TenantContext tenantContext;

    public PersistenceTagService(
            TagRepository tagRepository,
            MultilingualTextService multilingualTextService,
            TenantContext tenantContext) {
        this.tagRepository = tagRepository;
        this.multilingualTextService = multilingualTextService;
        this.tenantContext = tenantContext;
    }

    @Override
    public List<TagDefinition> list(TagScope scope, Locale locale) {
        String tenantId = tenantContext.getCurrentTenantId();
        return tagRepository.list(tenantId, scope).stream()
                .map(this::toDomain)
                .sorted((a, b) -> a.getDisplayName(locale).compareToIgnoreCase(b.getDisplayName(locale)))
                .toList();
    }

    @Override
    public TagDefinition getById(long id, Locale locale) {
        String tenantId = tenantContext.getCurrentTenantId();
        TagDefinitionEntity entity = tagRepository.findById(tenantId, id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return toDomain(entity);
    }

    @Override
    public TagDefinition create(MultilingualText name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        String tenantId = tenantContext.getCurrentTenantId();
        TagDefinitionEntity entity = new TagDefinitionEntity();
        entity.setTenantId(tenantId);
        entity.setDefaultLocale(name.getDefaultLocale());
        entity.setDefaultName(name.getValueOrDefault(name.getDefaultLocale()));
        entity.setColor(color);
        entity.setIcon(icon);
        entity.setScope(scope);
        entity.setApplyRule(applyRule);
        entity.setEnabled(enabled);
        entity.setCreatedAt(OffsetDateTime.now());
        tagRepository.insert(entity);
        persistName(entity.getId(), name);
        return toDomain(entity);
    }

    @Override
    public TagDefinition update(long id, MultilingualText name, String color, String icon, TagScope scope, String applyRule, boolean enabled) {
        String tenantId = tenantContext.getCurrentTenantId();
        TagDefinitionEntity existing = tagRepository.findById(tenantId, id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        TagDefinitionEntity updated = new TagDefinitionEntity();
        updated.setId(id);
        updated.setTenantId(tenantId);
        updated.setDefaultLocale(name.getDefaultLocale());
        updated.setDefaultName(name.getValueOrDefault(name.getDefaultLocale()));
        updated.setColor(color);
        updated.setIcon(icon);
        updated.setScope(scope);
        updated.setApplyRule(applyRule);
        updated.setEnabled(enabled);
        updated.setCreatedAt(existing.getCreatedAt());
        int affected = tagRepository.update(updated);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        persistName(id, name);
        return toDomain(updated);
    }

    @Override
    public void delete(long id) {
        String tenantId = tenantContext.getCurrentTenantId();
        TagDefinitionEntity existing = tagRepository.findById(tenantId, id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        tagRepository.deleteAssignmentsForTag(tenantId, id);
        tagRepository.delete(tenantId, id);
    }

    @Override
    public TagAssignment assign(long tagId, TagEntityType entityType, String entityId) {
        String tenantId = tenantContext.getCurrentTenantId();
        TagDefinition definition = getById(tagId, null);
        if (!definition.getScope().supports(entityType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "tag.scope.unsupported");
        }
        TagAssignmentEntity entity = new TagAssignmentEntity(tagId, tenantId, entityType, entityId, OffsetDateTime.now());
        tagRepository.insertAssignment(entity);
        return new TagAssignment(tagId, entityType, entityId);
    }

    @Override
    public void removeAssignment(long tagId, TagEntityType entityType, String entityId) {
        String tenantId = tenantContext.getCurrentTenantId();
        tagRepository.deleteAssignment(new TagAssignmentEntity(tagId, tenantId, entityType, entityId, null));
    }

    @Override
    public List<TagAssignment> listAssignments(long tagId) {
        String tenantId = tenantContext.getCurrentTenantId();
        return tagRepository.listAssignments(tenantId, tagId).stream()
                .map(entity -> new TagAssignment(entity.tagId(), entity.entityType(), entity.entityId()))
                .toList();
    }

    @Override
    public List<TagDefinition> findByEntity(TagEntityType entityType, String entityId, Locale locale) {
        String tenantId = tenantContext.getCurrentTenantId();
        return tagRepository.findByEntity(tenantId, entityType, entityId).stream()
                .map(this::toDomain)
                .sorted((a, b) -> a.getDisplayName(locale).compareToIgnoreCase(b.getDisplayName(locale)))
                .toList();
    }

    private void persistName(Long id, MultilingualText name) {
        multilingualTextService.upsert(
                MultilingualTextScope.TAG_DEFINITION,
                String.valueOf(id),
                "name",
                name
        );
    }

    private TagDefinition toDomain(TagDefinitionEntity entity) {
        MultilingualText name = multilingualTextService.find(
                        MultilingualTextScope.TAG_DEFINITION,
                        String.valueOf(entity.getId()),
                        "name")
                .orElseGet(() -> MultilingualText.of(
                        entity.getDefaultLocale(),
                        Map.of(entity.getDefaultLocale(), entity.getDefaultName())));
        return new TagDefinition(
                entity.getId(),
                name,
                entity.getColor(),
                entity.getIcon(),
                entity.getScope(),
                entity.getApplyRule(),
                entity.isEnabled(),
                entity.getCreatedAt()
        );
    }
}
