package com.power_puff.bms.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatabaseBackupRequest(
        @NotBlank(message = "Backup destination path is required")
        @Size(max = 4096, message = "Backup destination path is too long")
        String destinationPath
) {
}
