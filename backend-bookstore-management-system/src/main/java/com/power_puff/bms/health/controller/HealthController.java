package com.power_puff.bms.health.controller;

import com.power_puff.bms.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                return databaseUnavailable();
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Backend and database are ready",
                    Map.of("status", "UP", "database", "UP")));
        } catch (SQLException | RuntimeException exception) {
            // Do not leak the database path, SQL, or driver details in a public health response.
            return databaseUnavailable();
        }
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> databaseUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "Backend database is unavailable",
                        Map.of("status", "DOWN", "database", "DOWN")));
    }
}
