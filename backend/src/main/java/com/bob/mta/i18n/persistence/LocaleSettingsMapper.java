package com.bob.mta.i18n.persistence;

import org.apache.ibatis.annotations.Param;

public interface LocaleSettingsMapper {

    LocaleSettingEntity find(@Param("key") String key);

    int update(LocaleSettingEntity entity);

    void insert(LocaleSettingEntity entity);
}

