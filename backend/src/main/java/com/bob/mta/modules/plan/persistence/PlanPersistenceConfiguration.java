package com.bob.mta.modules.plan.persistence;

import com.bob.mta.common.mybatis.StringListJsonTypeHandler;
import com.bob.mta.common.mybatis.StringMapJsonTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(org.apache.ibatis.session.SqlSessionFactory.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@EnableTransactionManagement
@MapperScan(basePackageClasses = {
        PlanAggregateMapper.class,
        com.bob.mta.common.i18n.persistence.MultilingualTextMapper.class,
        com.bob.mta.i18n.persistence.LocaleSettingsMapper.class,
        com.bob.mta.modules.file.persistence.FileMetadataMapper.class
})
public class PlanPersistenceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    @ConditionalOnMissingBean
    public DataSourceProperties planDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
    public DataSource planDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager planTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

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
