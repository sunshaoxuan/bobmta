package com.bob.mta.modules.template.persistence;

import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.repository.PersistenceTemplateRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class TemplateRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bobmta")
            .withUsername("bobmta")
            .withPassword("secret");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private PersistenceTemplateRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeAll
    void migrateDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @BeforeEach
    void resetData() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_template_definition RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldInsertUpdateQueryAndDeleteTemplate() {
        TemplateEntity entity = new TemplateEntity();
        entity.setType(TemplateType.EMAIL);
        entity.setToRecipients(List.of("ops@example.com"));
        entity.setCcRecipients(List.of("lead@example.com"));
        entity.setEndpoint(null);
        entity.setEnabled(true);
        entity.setNameDefaultLocale("ja-JP");
        entity.setNameTranslations(Map.of("ja-JP", "運用連絡", "zh-CN", "运维通知"));
        entity.setSubjectDefaultLocale("ja-JP");
        entity.setSubjectTranslations(Map.of("ja-JP", "巡检予定", "en-US", "Maintenance Schedule"));
        entity.setContentDefaultLocale("ja-JP");
        entity.setContentTranslations(Map.of("ja-JP", "{{name}} 様\n点検予定をご確認ください。"));
        entity.setDescriptionDefaultLocale("ja-JP");
        entity.setDescriptionTranslations(Map.of("ja-JP", "定期巡检テンプレート"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        TemplateEntity inserted = repository.insert(entity);
        assertThat(inserted.getId()).isNotNull();
        assertThat(inserted.getToRecipients()).containsExactly("ops@example.com");

        inserted.setEnabled(false);
        inserted.setEndpoint("https://hooks.example.com/template");
        inserted.setUpdatedAt(now.plusHours(1));
        TemplateEntity updated = repository.update(inserted);
        assertThat(updated.isEnabled()).isFalse();
        assertThat(updated.getEndpoint()).isEqualTo("https://hooks.example.com/template");

        List<TemplateEntity> all = repository.findAll(null);
        assertThat(all).hasSize(1).first()
                .satisfies(def -> {
                    assertThat(def.getType()).isEqualTo(TemplateType.EMAIL);
                    assertThat(def.getSubjectTranslations()).containsEntry("en-US", "Maintenance Schedule");
                });

        assertThat(repository.findAll(TemplateType.SCRIPT)).isEmpty();

        repository.delete(updated.getId());
        assertThat(repository.findById(updated.getId())).isEmpty();
    }
}
