package com.power_puff.bms.backup.dto;

import java.time.Instant;

public record DatabaseBackupResult(
        String fileName,
        String filePath,
        long sizeBytes,
        Instant completedAt
) {
}
