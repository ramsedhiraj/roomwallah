package com.roomwallah.common.backup;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/backup")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupValidationService backupValidationService;

    @PostMapping("/validate")
    public ResponseEntity<BackupValidationReport> validateBackup(@RequestParam String backupFile) {
        BackupValidationReport report = backupValidationService.validateBackup(backupFile);
        return ResponseEntity.ok(report);
    }
}
