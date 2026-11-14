package com.power_puff.bms.backup.controller;

import com.power_puff.bms.backup.dto.DatabaseBackupRequest;
import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.dto.DatabaseRestoreRequest;
import com.power_puff.bms.backup.dto.DatabaseRestoreResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import com.power_puff.bms.backup.service.DatabaseRestoreService;
import com.power_puff.bms.security.ActiveSessionRegistry;
import com.power_puff.bms.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backup")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseBackupController {

    private final DatabaseBackupService backupService;
    private final DatabaseRestoreService restoreService;
    private final ActiveSessionRegistry activeSessionRegistry;

    public DatabaseBackupController(DatabaseBackupService backupService,
                                    DatabaseRestoreService restoreService,
                                    ActiveSessionRegistry activeSessionRegistry) {
        this.backupService = backupService;
        this.restoreService = restoreService;
        this.activeSessionRegistry = activeSessionRegistry;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DatabaseBackupResult>> createBackup(
            @Valid @RequestBody DatabaseBackupRequest request) {
        DatabaseBackupResult result = backupService.createBackup(request.destinationPath());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Database backup created successfully.", result));
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<DatabaseRestoreResult>> restoreBackup(
            @Valid @RequestBody DatabaseRestoreRequest request) {
        DatabaseRestoreResult result = restoreService.restore(request.backupPath());
        // Restore replaces the users table as well as business data. Revoke every
        // session authenticated against the previous database snapshot.
        activeSessionRegistry.invalidateAll();
        return ResponseEntity.ok(
                ApiResponse.success("Database restored successfully.", result));
    }
}
