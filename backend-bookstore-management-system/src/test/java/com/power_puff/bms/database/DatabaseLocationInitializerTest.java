package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseLocationInitializerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesRelativeDatabasePathAndCreatesParentBeforeDatasourceStartup() throws Exception {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path relativePath = Path.of(
                "target",
                "database-location-test-" + UUID.randomUUID(),
                "BookstoreManagementSystem",
                "bookstore.db");
        Path resolvedPath = workingDirectory.resolve(relativePath).normalize();

        try {
            try (GenericApplicationContext context = new GenericApplicationContext()) {
                context.getEnvironment().getPropertySources().addFirst(
                        new MapPropertySource("testProperties", Map.of(
                                "spring.datasource.url", "jdbc:sqlite:" + relativePath)));

                new DatabaseLocationInitializer().initialize(context);

                assertEquals(
                        "jdbc:sqlite:" + resolvedPath,
                        context.getEnvironment().getProperty("spring.datasource.url"));
                assertEquals(
                        resolvedPath.toString(),
                        context.getEnvironment().getProperty("bms.database.resolved-path"));
                assertTrue(Files.isDirectory(resolvedPath.getParent()));
            }
        } finally {
            Files.deleteIfExists(resolvedPath);
            Files.deleteIfExists(resolvedPath.getParent());
            Files.deleteIfExists(resolvedPath.getParent().getParent());
        }
    }

    @Test
    void keepsConfiguredAbsolutePathStable() {
        Path requestedPath = temporaryDirectory.resolve("persistent/bookstore.db")
                .toAbsolutePath().normalize();

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("testProperties", Map.of(
                            "spring.datasource.url", "jdbc:sqlite:" + requestedPath)));

            new DatabaseLocationInitializer().initialize(context);

            assertEquals(
                    "jdbc:sqlite:" + requestedPath,
                    context.getEnvironment().getProperty("spring.datasource.url"));
            assertTrue(Files.isDirectory(requestedPath.getParent()));
        }
    }
}
