package com.power_puff.bms.backup.controller;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseBackupControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseBackupService backupService;

    @TempDir
    Path temporaryDirectory;

    private Path backupFile;

    @AfterEach
    void cleanUp() throws Exception {
        if (backupFile != null) {
            Files.deleteIfExists(backupFile);
        }
    }

    @Test
    @WithMockUser(username = "backup-admin", roles = "ADMIN")
    void adminCanCreateDatabaseBackup() throws Exception {
        backupFile = temporaryDirectory.resolve("api-backup.db");

        mockMvc.perform(post("/api/backup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPath":"%s"}
                                """.formatted(jsonEscape(backupFile.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Database backup created successfully."))
                .andExpect(jsonPath("$.data.fileName").value("api-backup.db"))
                .andExpect(jsonPath("$.data.sizeBytes").isNumber());

        assertTrue(Files.isRegularFile(backupFile));
    }

    @Test
    @WithMockUser(username = "backup-admin", roles = "ADMIN")
    void adminCanRestoreValidatedDatabaseBackup() throws Exception {
        backupFile = temporaryDirectory.resolve("api-restore.db");
        DatabaseBackupResult backup = backupService.createBackup(backupFile.toString());

        mockMvc.perform(post("/api/backup/restore").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":"%s"}
                                """.formatted(backup.filePath())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Database restored successfully."))
                .andExpect(jsonPath("$.data.fileName").value("api-restore.db"))
                .andExpect(jsonPath("$.data.productCount").isNumber())
                .andExpect(jsonPath("$.data.saleCount").isNumber());
    }

    @Test
    @WithMockUser(username = "backup-admin", roles = "ADMIN")
    void rejectsMissingBackupPaths() throws Exception {
        mockMvc.perform(post("/api/backup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPath":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/backup/restore").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "backup-staff", roles = "STAFF")
    void staffCannotCreateOrRestoreBackups() throws Exception {
        backupFile = temporaryDirectory.resolve("staff-backup.db");

        mockMvc.perform(post("/api/backup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPath":"%s"}
                                """.formatted(backupFile.toString())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/backup/restore").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":"%s"}
                                """.formatted(backupFile.toString())))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(backupFile));
    }

    @Test
    void unauthenticatedUsersCannotCreateOrRestoreBackups() throws Exception {
        mockMvc.perform(post("/api/backup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPath":"%s"}
                                """.formatted(jsonEscape(temporaryDirectory.resolve("anonymous.db").toString()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/backup/restore").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":"%s"}
                                """.formatted(jsonEscape(temporaryDirectory.resolve("anonymous.db").toString()))))
                .andExpect(status().isUnauthorized());
    }
    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
