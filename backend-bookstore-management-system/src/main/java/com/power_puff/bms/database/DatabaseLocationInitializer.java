package com.power_puff.bms.database;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Resolves the configured SQLite file to an absolute location and creates its
 * parent directory before Spring Boot initializes the datasource.
 */
public class DatabaseLocationInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String jdbcUrl = environment.getProperty("spring.datasource.url");

        if (jdbcUrl == null || !jdbcUrl.startsWith(SQLITE_PREFIX)) {
            throw new IllegalStateException(
                    "The configured datasource must use a file-backed SQLite JDBC URL.");
        }

        String configuredLocation = jdbcUrl.substring(SQLITE_PREFIX.length()).trim();
        if (configuredLocation.isEmpty()
                || ":memory:".equalsIgnoreCase(configuredLocation)
                || configuredLocation.startsWith("file::memory:")) {
            throw new IllegalStateException(
                    "The production bookstore database must be a persistent SQLite file.");
        }

        final Path databasePath;
        try {
            databasePath = Path.of(configuredLocation).toAbsolutePath().normalize();
            Path parent = databasePath.getParent();
            if (parent == null) {
                throw new IllegalStateException(
                        "The configured SQLite database path has no parent directory.");
            }
            Files.createDirectories(parent);
            if (!Files.isDirectory(parent) || !Files.isWritable(parent)) {
                throw new IllegalStateException(
                        "The configured SQLite database directory is not writable.");
            }
        } catch (IOException | java.nio.file.InvalidPathException exception) {
            throw new IllegalStateException(
                    "The configured SQLite database directory could not be prepared.",
                    exception);
        }

        environment.getPropertySources().addFirst(new MapPropertySource(
                "resolvedBookstoreDatabaseLocation",
                Map.of(
                        "spring.datasource.url", SQLITE_PREFIX + databasePath,
                        "bms.database.resolved-path", databasePath.toString())));
    }
}
