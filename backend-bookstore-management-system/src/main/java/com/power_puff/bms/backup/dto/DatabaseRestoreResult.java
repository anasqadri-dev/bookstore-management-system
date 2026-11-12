package com.power_puff.bms.backup.dto;

import java.time.Instant;

public record DatabaseRestoreResult(
        String fileName,
        long productCount,
        long saleCount,
        Instant restoredAt
) {
}
