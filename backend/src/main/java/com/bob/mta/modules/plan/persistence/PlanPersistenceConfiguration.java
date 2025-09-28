package com.bob.mta.modules.plan.persistence;

import com.bob.mta.common.mybatis.StringListJsonTypeHandler;
import com.bob.mta.common.mybatis.StringMapJsonTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(org.apache.ibatis.session.SqlSessionFactory.class)
@ConditionalOnBean(DataSource.class)
@MapperScan(basePackageClasses = {
        PlanAggregateMapper.class,
        com.bob.mta.common.i18n.persistence.MultilingualTextMapper.class,
        com.bob.mta.i18n.persistence.LocaleSettingsMapper.class
})
public class PlanPersistenceConfiguration {

    @Bean
    public ConfigurationCustomizer planConfigurationCustomizer() {
        return this::registerTypeHandlers;
    }

    private void registerTypeHandlers(org.apache.ibatis.session.Configuration configuration) {
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        registry.register(StringListJsonTypeHandler.class);
        registry.register(StringMapJsonTypeHandler.class);
    }
}
