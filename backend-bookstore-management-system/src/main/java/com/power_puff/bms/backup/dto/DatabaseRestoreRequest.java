package com.power_puff.bms.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatabaseRestoreRequest(
        @NotBlank(message = "Backup file path is required")
        @Size(max = 4096, message = "Backup file path is too long")
        String backupPath
) {
}
