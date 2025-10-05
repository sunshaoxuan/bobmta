package com.bob.mta.modules.template.repository;

import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.persistence.TemplateEntity;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {

    List<TemplateEntity> findAll(TemplateType type);

    Optional<TemplateEntity> findById(long id);

    TemplateEntity insert(TemplateEntity entity);

    TemplateEntity update(TemplateEntity entity);

    void delete(long id);
}
