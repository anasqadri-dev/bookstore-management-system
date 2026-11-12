package com.power_puff.bms.backup.service;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class BackupPathValidator {

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of(".db", ".sqlite", ".sqlite3");

    public Path validateBackupDestination(String requestedPath, Path activeDatabasePath) {
        Path destination = parseAbsolutePath(requestedPath, "Backup destination");

        String fileName = destination.getFileName() == null
                ? ""
                : destination.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean supportedExtension = SUPPORTED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
        if (!supportedExtension) {
            throw new IllegalArgumentException(
                    "Backup destination must use a .db, .sqlite, or .sqlite3 extension.");
        }

        if (activeDatabasePath != null
                && destination.equals(activeDatabasePath.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "Backup destination cannot be the active application database.");
        }

        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(
                    "Backup destination already exists. Choose a new file name.");
        }

        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException(
                    "Backup destination directory does not exist.");
        }
        if (!Files.isWritable(parent)) {
            throw new IllegalArgumentException(
                    "Backup destination directory is not writable.");
        }

        return destination;
    }

    public Path validateBackupSource(String requestedPath, Path activeDatabasePath) {
        Path source = parseAbsolutePath(requestedPath, "Backup file");

        if (activeDatabasePath != null
                && source.equals(activeDatabasePath.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "The active application database cannot be used as a backup source.");
        }

        if (Files.isSymbolicLink(source)
                || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(source)) {
            throw new IllegalArgumentException(
                    "Backup source must be an existing, readable regular file.");
        }

        try {
            if (Files.size(source) == 0) {
                throw new IllegalArgumentException("Backup file is empty.");
            }
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Backup file size could not be verified.", exception);
        }

        String fileName = source.getFileName() == null
                ? ""
                : source.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean supportedExtension = SUPPORTED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
        if (!supportedExtension) {
            throw new IllegalArgumentException(
                    "Backup file must use a .db, .sqlite, or .sqlite3 extension.");
        }

        return source;
    }

    private Path parseAbsolutePath(String requestedPath, String description) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException(description + " path is required.");
        }
        if (requestedPath.length() > 4096) {
            throw new IllegalArgumentException(description + " path is too long.");
        }

        try {
            Path path = Path.of(requestedPath.trim());
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(description + " path must be absolute.");
            }
            return path.normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException(description + " path is invalid.", exception);
        }
    }
}
