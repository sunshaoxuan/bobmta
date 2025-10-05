package com.bob.mta.modules.tag.persistence;

import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class TagMapperIntegrationTest {

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
    private PersistenceTagRepository repository;

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
        jdbcTemplate.execute("TRUNCATE TABLE mt_tag_assignment, mt_tag_definition RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldInsertListUpdateAndDeleteTagDefinitions() {
        TagDefinitionEntity highPriority = new TagDefinitionEntity();
        highPriority.setTenantId("tenant-1");
        highPriority.setDefaultLocale("ja-JP");
        highPriority.setDefaultName("高優先度");
        highPriority.setColor("#FF0000");
        highPriority.setIcon("AlertOutlined");
        highPriority.setScope(TagScope.CUSTOMER);
        highPriority.setApplyRule(null);
        highPriority.setEnabled(true);
        repository.insert(highPriority);
        assertThat(highPriority.getId()).isNotNull();

        TagDefinitionEntity maintenance = new TagDefinitionEntity();
        maintenance.setTenantId("tenant-1");
        maintenance.setDefaultLocale("ja-JP");
        maintenance.setDefaultName("メンテナンス");
        maintenance.setColor("#1890FF");
        maintenance.setIcon("ToolOutlined");
        maintenance.setScope(TagScope.PLAN);
        maintenance.setEnabled(true);
        repository.insert(maintenance);

        List<TagDefinitionEntity> customerTags = repository.list("tenant-1", TagScope.CUSTOMER);
        assertThat(customerTags)
                .extracting(TagDefinitionEntity::getDefaultName)
                .containsExactly("高優先度");

        TagDefinitionEntity fetched = repository.findById("tenant-1", highPriority.getId());
        assertThat(fetched.getDefaultName()).isEqualTo("高優先度");

        highPriority.setDefaultName("重点対応");
        int updatedRows = repository.update(highPriority);
        assertThat(updatedRows).isEqualTo(1);
        assertThat(repository.findById("tenant-1", highPriority.getId()).getDefaultName()).isEqualTo("重点対応");

        repository.delete("tenant-1", maintenance.getId());
        assertThat(repository.findById("tenant-1", maintenance.getId())).isNull();
    }

    @Test
    void shouldManageAssignmentsAndQueryByEntity() {
        TagDefinitionEntity definition = new TagDefinitionEntity();
        definition.setTenantId("tenant-1");
        definition.setDefaultLocale("ja-JP");
        definition.setDefaultName("重要顧客");
        definition.setColor("#FF5722");
        definition.setIcon("StarOutlined");
        definition.setScope(TagScope.CUSTOMER);
        definition.setEnabled(true);
        repository.insert(definition);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TagAssignmentEntity assignment = new TagAssignmentEntity(
                definition.getId(),
                "tenant-1",
                TagEntityType.CUSTOMER,
                "cust-100",
                now
        );
        repository.insertAssignment(assignment);

        List<TagAssignmentEntity> assignments = repository.listAssignments("tenant-1", definition.getId());
        assertThat(assignments)
                .hasSize(1)
                .first()
                .satisfies(entity -> {
                    assertThat(entity.entityId()).isEqualTo("cust-100");
                    assertThat(entity.entityType()).isEqualTo(TagEntityType.CUSTOMER);
                });

        List<TagDefinitionEntity> tags = repository.findByEntity("tenant-1", TagEntityType.CUSTOMER, "cust-100");
        assertThat(tags).extracting(TagDefinitionEntity::getDefaultName).containsExactly("重要顧客");

        int removed = repository.deleteAssignment(assignment);
        assertThat(removed).isEqualTo(1);
        assertThat(repository.listAssignments("tenant-1", definition.getId())).isEmpty();
    }
}
