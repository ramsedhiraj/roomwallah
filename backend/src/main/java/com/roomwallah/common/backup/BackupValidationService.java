package com.roomwallah.common.backup;

public interface BackupValidationService {
    BackupValidationReport validateBackup(String backupFilePath);
}
