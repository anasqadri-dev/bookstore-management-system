package com.power_puff.bms.health.controller;

import com.power_puff.bms.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void healthEndpointReportsUpOnlyWhenSqliteCanExecuteARead() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        var response = new HealthController(dataSource).health();

        assertEquals(200, response.getStatusCode().value());
        ApiResponse<Map<String, String>> body = response.getBody();
        assertTrue(body.success());
        assertEquals("UP", body.data().get("status"));
        assertEquals("UP", body.data().get("database"));
    }

    @Test
    void healthEndpointReturnsServiceUnavailableWhenSqliteCannotBeReached()
            throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("private database path and SQL details"));

        var response = new HealthController(dataSource).health();

        assertEquals(503, response.getStatusCode().value());
        ApiResponse<Map<String, String>> body = response.getBody();
        assertFalse(body.success());
        assertEquals("Backend database is unavailable", body.message());
        assertEquals("DOWN", body.data().get("status"));
        assertEquals("DOWN", body.data().get("database"));
        assertFalse(body.message().contains("private"));
    }
}
