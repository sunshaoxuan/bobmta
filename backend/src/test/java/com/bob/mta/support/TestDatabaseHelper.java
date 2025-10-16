package com.bob.mta.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Utility helpers for preparing database fixtures in integration tests.
 */
public final class TestDatabaseHelper {

    private TestDatabaseHelper() {
    }

    public static void seedDefaultUsers(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        insertUser(jdbcTemplate, passwordEncoder,
                "user-admin",
                "admin",
                "系统管理员",
                "admin@example.com",
                "admin123",
                List.of("ROLE_ADMIN", "ROLE_OPERATOR"));

        insertUser(jdbcTemplate, passwordEncoder,
                "user-operator",
                "operator",
                "运维专员",
                "operator@example.com",
                "operator123",
                List.of("ROLE_OPERATOR"));
    }

    private static void insertUser(JdbcTemplate jdbcTemplate,
                                   PasswordEncoder passwordEncoder,
                                   String userId,
                                   String username,
                                   String displayName,
                                   String email,
                                   String rawPassword,
                                   List<String> roles) {
        jdbcTemplate.update("DELETE FROM mt_user_activation_token WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM mt_user_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM mt_user WHERE user_id = ? OR lower(username) = lower(?)",
                userId, username);

        jdbcTemplate.update("""
                INSERT INTO mt_user (user_id, username, display_name, email, password_hash, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
                """,
                userId,
                username,
                displayName,
                email,
                passwordEncoder.encode(rawPassword));

        for (String role : roles) {
            jdbcTemplate.update("INSERT INTO mt_user_role (user_id, role) VALUES (?, ?)", userId, role);
        }
    }
}

