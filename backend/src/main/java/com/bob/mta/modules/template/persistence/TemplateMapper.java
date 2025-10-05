package com.bob.mta.modules.template.persistence;

import com.bob.mta.modules.template.domain.TemplateType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateMapper {

    List<TemplateEntity> findAll(@Param("type") TemplateType type);

    TemplateEntity findById(@Param("id") long id);

    void insert(TemplateEntity entity);

    int update(TemplateEntity entity);

    int delete(@Param("id") long id);
}
