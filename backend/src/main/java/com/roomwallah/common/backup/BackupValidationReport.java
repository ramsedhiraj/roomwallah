package com.roomwallah.common.backup;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class BackupValidationReport {
    private String backupFile;
    private Instant validationTime;
    private boolean successful;
    private List<String> logs;
    private int tablesRestoredCount;
    private String integrityDetails;
}
