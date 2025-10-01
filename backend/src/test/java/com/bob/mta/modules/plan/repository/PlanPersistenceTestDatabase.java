package com.bob.mta.modules.plan.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

final class PlanPersistenceTestDatabase {

    private static final String[] DROP_STATEMENTS = {
            "DROP TABLE IF EXISTS mt_plan_activity",
            "DROP TABLE IF EXISTS mt_plan_node_attachment",
            "DROP TABLE IF EXISTS mt_plan_node_execution",
            "DROP TABLE IF EXISTS mt_plan_node",
            "DROP TABLE IF EXISTS mt_plan_participant",
            "DROP TABLE IF EXISTS mt_plan_reminder_rule",
            "DROP TABLE IF EXISTS mt_plan",
            "DROP SEQUENCE IF EXISTS mt_plan_id_seq",
            "DROP SEQUENCE IF EXISTS mt_plan_node_id_seq",
            "DROP SEQUENCE IF EXISTS mt_plan_reminder_id_seq"
    };

    private static final String[] CREATE_STATEMENTS = {
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_id_seq START WITH 1",
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_node_id_seq START WITH 1",
            "CREATE SEQUENCE IF NOT EXISTS mt_plan_reminder_id_seq START WITH 1",
            "CREATE TABLE IF NOT EXISTS mt_plan (" +
                    "plan_id VARCHAR(64) PRIMARY KEY, " +
                    "tenant_id VARCHAR(64), " +
                    "customer_id VARCHAR(64), " +
                    "owner_id VARCHAR(64), " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "status VARCHAR(32) NOT NULL, " +
                    "planned_start_time TIMESTAMPTZ, " +
                    "planned_end_time TIMESTAMPTZ, " +
                    "actual_start_time TIMESTAMPTZ, " +
                    "actual_end_time TIMESTAMPTZ, " +
                    "cancel_reason TEXT, " +
                    "canceled_by VARCHAR(64), " +
                    "canceled_at TIMESTAMPTZ, " +
                    "timezone VARCHAR(64), " +
                    "created_at TIMESTAMPTZ, " +
                    "updated_at TIMESTAMPTZ, " +
                    "reminder_updated_at TIMESTAMPTZ, " +
                    "reminder_updated_by VARCHAR(64))",
            "CREATE TABLE IF NOT EXISTS mt_plan_participant (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "participant_id VARCHAR(64) NOT NULL, " +
                    "PRIMARY KEY (plan_id, participant_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "parent_node_id VARCHAR(64), " +
                    "name VARCHAR(255) NOT NULL, " +
                    "type VARCHAR(64) NOT NULL, " +
                    "assignee VARCHAR(64), " +
                    "order_index INT NOT NULL, " +
                    "expected_duration_minutes INT, " +
                    "action_type VARCHAR(64), " +
                    "completion_threshold INT, " +
                    "action_ref VARCHAR(255), " +
                    "description TEXT, " +
                    "PRIMARY KEY (plan_id, node_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node_execution (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "status VARCHAR(32) NOT NULL, " +
                    "start_time TIMESTAMPTZ, " +
                    "end_time TIMESTAMPTZ, " +
                    "operator_id VARCHAR(64), " +
                    "result_summary TEXT, " +
                    "execution_log TEXT, " +
                    "PRIMARY KEY (plan_id, node_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_node_attachment (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "node_id VARCHAR(64) NOT NULL, " +
                    "file_id VARCHAR(128) NOT NULL, " +
                    "PRIMARY KEY (plan_id, node_id, file_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_activity (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "activity_id VARCHAR(64) NOT NULL, " +
                    "activity_type VARCHAR(64) NOT NULL, " +
                    "occurred_at TIMESTAMPTZ NOT NULL, " +
                    "actor_id VARCHAR(64), " +
                    "message_key VARCHAR(255), " +
                    "reference_id VARCHAR(64), " +
                    "attributes JSONB, " +
                    "PRIMARY KEY (plan_id, activity_id))",
            "CREATE TABLE IF NOT EXISTS mt_plan_reminder_rule (" +
                    "plan_id VARCHAR(64) NOT NULL, " +
                    "rule_id VARCHAR(64) NOT NULL, " +
                    "trigger VARCHAR(64) NOT NULL, " +
                    "offset_minutes INT NOT NULL, " +
                    "channels JSONB, " +
                    "template_id VARCHAR(64), " +
                    "recipients JSONB, " +
                    "description TEXT, " +
                    "active BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (plan_id, rule_id))"
    };

    private static final String[] RESET_SEQUENCES = {
            "ALTER SEQUENCE mt_plan_id_seq RESTART WITH 1",
            "ALTER SEQUENCE mt_plan_node_id_seq RESTART WITH 1",
            "ALTER SEQUENCE mt_plan_reminder_id_seq RESTART WITH 1"
    };

    private static final List<String> TABLES = List.of(
            "mt_plan_activity",
            "mt_plan_node_attachment",
            "mt_plan_node_execution",
            "mt_plan_node",
            "mt_plan_participant",
            "mt_plan_reminder_rule",
            "mt_plan"
    );

    private PlanPersistenceTestDatabase() {
    }

    static void initializeSchema(JdbcTemplate jdbcTemplate) {
        runStatements(jdbcTemplate, DROP_STATEMENTS);
        runStatements(jdbcTemplate, CREATE_STATEMENTS);
    }

    static void cleanDatabase(JdbcTemplate jdbcTemplate) {
        for (String table : TABLES) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }
        runStatements(jdbcTemplate, RESET_SEQUENCES);
    }

    static long countRows(JdbcTemplate jdbcTemplate, String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table, Long.class);
    }

    static List<String> tableNames() {
        return TABLES;
    }

    private static void runStatements(JdbcTemplate jdbcTemplate, String[] statements) {
        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
    }
}

