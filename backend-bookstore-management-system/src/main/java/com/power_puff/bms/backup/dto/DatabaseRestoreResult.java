package com.power_puff.bms.backup.dto;

import java.time.Instant;
import java.util.Map;

public record DatabaseRestoreResult(
        String fileName,
        long productCount,
        long saleCount,
        Map<String, Long> tableCounts,
        Instant restoredAt
) {
}
