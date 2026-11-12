package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseLocationInitializerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesRelativeDatabasePathAndCreatesParentBeforeDatasourceStartup() {
        Path requestedPath = temporaryDirectory
                .resolve("user-data")
                .resolve("BookstoreManagementSystem")
                .resolve("bookstore.db");
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path relativePath = workingDirectory.relativize(requestedPath.toAbsolutePath().normalize());

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("testProperties", Map.of(
                            "spring.datasource.url", "jdbc:sqlite:" + relativePath)));

            new DatabaseLocationInitializer().initialize(context);

            Path resolvedPath = requestedPath.toAbsolutePath().normalize();
            assertEquals(
                    "jdbc:sqlite:" + resolvedPath,
                    context.getEnvironment().getProperty("spring.datasource.url"));
            assertEquals(
                    resolvedPath.toString(),
                    context.getEnvironment().getProperty("bms.database.resolved-path"));
            assertTrue(Files.isDirectory(resolvedPath.getParent()));
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
