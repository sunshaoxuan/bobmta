package com.bob.mta.common.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StringListJsonTypeHandlerTest {

    private final StringListJsonTypeHandler handler = new StringListJsonTypeHandler();

    @Test
    void shouldWriteJsonArrayToPreparedStatement() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 3, List.of("EMAIL", "SMS"), JdbcType.VARCHAR);

        verify(statement).setString(eq(3), eq("[\"EMAIL\",\"SMS\"]"));
    }

    @Test
    void shouldReturnEmptyListWhenColumnIsNull() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("channels")).thenReturn(null);

        List<String> result = handler.getNullableResult(resultSet, "channels");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReadJsonArrayFromResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("channels")).thenReturn("[\"EMAIL\",\"SMS\"]");

        List<String> result = handler.getNullableResult(resultSet, "channels");

        assertThat(result).containsExactly("EMAIL", "SMS");
    }

    @Test
    void shouldReadJsonArrayFromCallableStatement() throws SQLException {
        CallableStatement callableStatement = mock(CallableStatement.class);
        when(callableStatement.getString(4)).thenReturn("[\"EMAIL\"]");

        List<String> result = handler.getNullableResult(callableStatement, 4);

        assertThat(result).containsExactly("EMAIL");
    }

    @Test
    void shouldFailWhenJsonIsInvalid() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(2)).thenReturn("not-json");

        assertThatThrownBy(() -> handler.getNullableResult(resultSet, 2))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("deserialize JSON array");
    }
}
