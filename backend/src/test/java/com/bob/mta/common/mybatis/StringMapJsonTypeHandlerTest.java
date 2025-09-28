package com.bob.mta.common.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StringMapJsonTypeHandlerTest {

    private final StringMapJsonTypeHandler handler = new StringMapJsonTypeHandler();

    @Test
    void shouldWriteJsonObjectToPreparedStatement() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 1, Map.of("actor", "u-1"), JdbcType.OTHER);

        verify(statement).setString(eq(1), eq("{\"actor\":\"u-1\"}"));
    }

    @Test
    void shouldReturnEmptyMapWhenColumnIsBlank() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("attributes")).thenReturn(" ");

        Map<String, String> result = handler.getNullableResult(resultSet, "attributes");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReadJsonObjectFromResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("attributes")).thenReturn("{\"actor\":\"u-1\"}");

        Map<String, String> result = handler.getNullableResult(resultSet, "attributes");

        assertThat(result).containsEntry("actor", "u-1");
    }

    @Test
    void shouldFailWhenJsonObjectIsInvalid() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(1)).thenReturn("{invalid}");

        assertThatThrownBy(() -> handler.getNullableResult(resultSet, 1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("deserialize JSON object");
    }
}
