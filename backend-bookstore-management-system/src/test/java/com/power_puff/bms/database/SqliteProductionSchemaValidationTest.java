package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/bms-production-schema-validation-${random.uuid}.db",
        "spring.sql.init.mode=always",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class SqliteProductionSchemaValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void baselineSchemaValidatesAgainstEveryJpaEntityAndRecordsMigrationHistory() {
        assertEquals(12, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sqlite_master
                WHERE type = 'table'
                  AND name NOT LIKE 'sqlite_%'
                  AND name <> 'schema_migrations'
                """, Integer.class));

        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_migrations", Integer.class));

        assertEquals(1, jdbcTemplate.queryForObject(
                "PRAGMA foreign_keys", Integer.class));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_foreign_key_check", Integer.class));
    }
}
