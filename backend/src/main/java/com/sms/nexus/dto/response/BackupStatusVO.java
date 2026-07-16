package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupStatusVO {

    private boolean autoBackupEnabled;
    private int backupRetentionDays;
    private BackupRecordVO latestBackup;

    @Data
    public static class BackupRecordVO {
        private Long id;
        private String fileName;
        private Long fileSize;
        private String status;
        private String triggerType;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }
}
