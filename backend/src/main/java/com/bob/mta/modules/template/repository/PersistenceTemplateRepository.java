package com.bob.mta.modules.template.repository;

import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.persistence.TemplateEntity;
import com.bob.mta.modules.template.persistence.TemplateMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnBean(TemplateMapper.class)
public class PersistenceTemplateRepository implements TemplateRepository {

    private final TemplateMapper mapper;

    public PersistenceTemplateRepository(TemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<TemplateEntity> findAll(TemplateType type) {
        return mapper.findAll(type);
    }

    @Override
    public Optional<TemplateEntity> findById(long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public TemplateEntity insert(TemplateEntity entity) {
        mapper.insert(entity);
        if (entity.getId() == null) {
            throw new IllegalStateException("Template insert did not return generated id");
        }
        return mapper.findById(entity.getId());
    }

    @Override
    public TemplateEntity update(TemplateEntity entity) {
        int updated = mapper.update(entity);
        if (updated == 0) {
            throw new IllegalStateException("Template update failed for id=" + entity.getId());
        }
        return mapper.findById(entity.getId());
    }

    @Override
    public void delete(long id) {
        mapper.delete(id);
    }
}
