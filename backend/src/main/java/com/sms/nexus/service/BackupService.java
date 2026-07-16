package com.sms.nexus.service;

import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.BackupStatusVO;

public interface BackupService {

    ApiResponse<BackupStatusVO> getBackupStatus();

    ApiResponse<Long> executeBackup(String operatorId, String operatorName, String triggerType);

    ApiResponse<Void> restoreBackup(Long backupId, String operatorId, String operatorName);

    void executeScheduledBackup();
}
