package com.bob.mta.common.i18n.persistence;

import com.bob.mta.common.i18n.MultilingualTextScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MultilingualTextMapper {

    MultilingualTextEntity find(@Param("scope") MultilingualTextScope scope,
                                @Param("entityId") String entityId,
                                @Param("field") String field);

    int update(MultilingualTextEntity entity);

    void insert(MultilingualTextEntity entity);
}
