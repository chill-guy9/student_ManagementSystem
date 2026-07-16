package com.sms.nexus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BackupRestoreRequest {

    @NotNull(message = "备份记录ID不能为空")
    private Long backupId;
}
