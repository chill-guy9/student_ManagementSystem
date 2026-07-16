package com.sms.nexus.controller;

import com.sms.nexus.dto.request.BackupRestoreRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.BackupStatusVO;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Tag(name = "备份管理", description = "数据库备份恢复接口")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/status")
    @Operation(summary = "获取备份状态")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<BackupStatusVO> getBackupStatus() {
        return backupService.getBackupStatus();
    }

    @PostMapping("/execute")
    @Operation(summary = "执行备份")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<Long> executeBackup(Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return backupService.executeBackup(operatorId, operatorName, "MANUAL");
    }

    @PostMapping("/restore")
    @Operation(summary = "恢复备份")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<Void> restoreBackup(@Valid @RequestBody BackupRestoreRequest request,
                                            Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return backupService.restoreBackup(request.getBackupId(), operatorId, operatorName);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
