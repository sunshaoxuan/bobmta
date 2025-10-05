package com.bob.mta.modules.customer.persistence;

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
class CustomerMapperIntegrationTest {

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
    private CustomerMapper mapper;

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
        jdbcTemplate.execute("TRUNCATE TABLE mt_tag_assignment, mt_tag_definition, mt_customer RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldSearchCustomersAndCountByKeywordAndRegion() {
        insertCustomer("tenant-1", "cust-001", "CUST-001", "Alpha Systems", "Alpha", "Finance", "Kanto", OffsetDateTime.now(ZoneOffset.UTC));
        insertCustomer("tenant-1", "cust-002", "CUST-002", "Beta Manufacturing", "Beta", "Industry", "Kansai", OffsetDateTime.now(ZoneOffset.UTC));
        long tagId = insertTagDefinition("tenant-1", "重点关注");
        assignTag(tagId, "tenant-1", "cust-001");

        List<CustomerSummaryRecord> summaries = mapper.search("tenant-1", "alpha", null, 0, 10);
        assertThat(summaries)
                .hasSize(1)
                .first()
                .satisfies(summary -> {
                    assertThat(summary.code()).isEqualTo("CUST-001");
                    assertThat(summary.tags()).containsExactly("重点关注");
                });

        long count = mapper.count("tenant-1", "alpha", null);
        assertThat(count).isEqualTo(1);

        assertThat(mapper.count("tenant-1", null, "Kansai")).isEqualTo(1);
        assertThat(mapper.search("tenant-1", null, "Kansai", 0, 5)).extracting(CustomerSummaryRecord::code)
                .containsExactly("CUST-002");
    }

    @Test
    void shouldFindDetailWithTags() {
        insertCustomer("tenant-1", "cust-010", "CUST-010", "Gamma Labs", "Gamma", "Research", "Tohoku", OffsetDateTime.parse("2024-05-01T12:00:00Z"));
        long tagA = insertTagDefinition("tenant-1", "核心客户");
        long tagB = insertTagDefinition("tenant-1", "年度合同");
        assignTag(tagA, "tenant-1", "cust-010");
        assignTag(tagB, "tenant-1", "cust-010");

        CustomerDetailRecord detail = mapper.findDetail("tenant-1", "cust-010");
        assertThat(detail).isNotNull();
        assertThat(detail.code()).isEqualTo("CUST-010");
        assertThat(detail.tags()).containsExactly("年度合同", "核心客户");

        CustomerEntity entity = mapper.findById("tenant-1", "cust-010");
        assertThat(entity).isNotNull();
        assertThat(entity.name()).isEqualTo("Gamma Labs");
    }

    @Test
    void shouldReturnNullWhenCustomerMissing() {
        assertThat(mapper.findDetail("tenant-1", "missing")).isNull();
        assertThat(mapper.findById("tenant-1", "missing")).isNull();
    }

    private void insertCustomer(String tenantId,
                                String customerId,
                                String code,
                                String name,
                                String shortName,
                                String groupName,
                                String region,
                                OffsetDateTime timestamp) {
        jdbcTemplate.update(
                """
                        INSERT INTO mt_customer (customer_id, tenant_id, code, name, short_name, group_name, region, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                customerId,
                tenantId,
                code,
                name,
                shortName,
                groupName,
                region,
                timestamp,
                timestamp
        );
    }

    private long insertTagDefinition(String tenantId, String defaultName) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO mt_tag_definition (tenant_id, default_locale, default_name, color, icon, scope, apply_rule, enabled)
                        VALUES (?, 'ja-JP', ?, '#FF5722', 'StarOutlined', 'CUSTOMER', NULL, TRUE)
                        RETURNING tag_id
                        """,
                Long.class,
                tenantId,
                defaultName
        );
    }

    private void assignTag(long tagId, String tenantId, String customerId) {
        jdbcTemplate.update(
                "INSERT INTO mt_tag_assignment (tag_id, tenant_id, entity_type, entity_id) VALUES (?, ?, 'CUSTOMER', ?)",
                tagId,
                tenantId,
                customerId
        );
    }
}
