package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sms.nexus.common.enums.BackupStatus;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.enums.TriggerType;
import com.sms.nexus.common.exception.BusinessException;
import com.sms.nexus.common.exception.ResourceNotFoundException;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.BackupStatusVO;
import com.sms.nexus.entity.BackupRecord;
import com.sms.nexus.entity.SystemSetting;
import com.sms.nexus.mapper.BackupRecordMapper;
import com.sms.nexus.mapper.SystemSettingMapper;
import com.sms.nexus.service.BackupService;
import com.sms.nexus.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupRecordMapper backupRecordMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final LogService logService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private static final String BACKUP_DIR = "./backups";
    private static final int DEFAULT_RETENTION_DAYS = 30;

    @Override
    public ApiResponse<BackupStatusVO> getBackupStatus() {
        BackupStatusVO vo = new BackupStatusVO();

        // Get auto backup setting
        SystemSetting autoBackup = systemSettingMapper.selectOne(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "auto_backup_enabled"));
        vo.setAutoBackupEnabled(autoBackup != null && "true".equals(autoBackup.getSettingValue()));

        // Get retention days (with safe parsing)
        SystemSetting retention = systemSettingMapper.selectOne(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "backup_retention_days"));
        vo.setBackupRetentionDays(parseRetentionDays(retention));

        // Get latest backup
        BackupRecord latest = backupRecordMapper.selectOne(
                new LambdaQueryWrapper<BackupRecord>().orderByDesc(BackupRecord::getCreatedAt).last("LIMIT 1"));

        if (latest != null) {
            BackupStatusVO.BackupRecordVO recordVO = new BackupStatusVO.BackupRecordVO();
            recordVO.setId(latest.getId());
            recordVO.setFileName(latest.getFileName());
            recordVO.setFileSize(latest.getFileSize());
            recordVO.setStatus(latest.getStatus());
            recordVO.setTriggerType(latest.getTriggerType());
            recordVO.setStartedAt(latest.getStartedAt());
            recordVO.setFinishedAt(latest.getFinishedAt());
            vo.setLatestBackup(recordVO);
        }

        return ApiResponse.success(vo);
    }

    @Override
    public ApiResponse<Long> executeBackup(String operatorId, String operatorName, String triggerType) {
        // Create backup directory
        Path backupDir = Paths.get(BACKUP_DIR);
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new BusinessException("创建备份目录失败: " + e.getMessage());
        }

        // Create backup record
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "sms_nexus_backup_" + timestamp + ".sql";
        String filePath = backupDir.resolve(fileName).toString();

        BackupRecord record = new BackupRecord();
        record.setFileName(fileName);
        record.setFilePath(filePath);
        record.setStatus(BackupStatus.RUNNING.getValue());
        record.setTriggerType(triggerType);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setStartedAt(LocalDateTime.now());

        backupRecordMapper.insert(record);

        // Execute mysqldump
        try {
            String dbName = extractDbName(dbUrl);
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump", "-u" + dbUsername,
                    "-p",  // Use MYSQL_PWD env var instead of -p<password>
                    dbName,
                    "--result-file=" + filePath
            );
            // Pass password via environment variable to avoid exposure in process list
            pb.environment().put("MYSQL_PWD", dbPassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain process output in background to prevent deadlock
            Thread outputDrainer = new Thread(() -> {
                try (var is = process.getInputStream()) {
                    is.transferTo(OutputStream.nullOutputStream());
                } catch (IOException e) {
                    log.warn("Error draining backup process output", e);
                }
            });
            outputDrainer.setDaemon(true);
            outputDrainer.start();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File backupFile = new File(filePath);
                record.setFileSize(backupFile.length());
                record.setStatus(BackupStatus.SUCCESS.getValue());
                record.setFinishedAt(LocalDateTime.now());

                logService.createLog(operatorId, operatorName, OperationType.BACKUP.getValue(),
                        "backup", String.valueOf(record.getId()), fileName,
                        "数据库备份成功: " + fileName, null, "INFO");
            } else {
                record.setStatus(BackupStatus.FAILED.getValue());
                record.setFinishedAt(LocalDateTime.now());
                record.setErrorMessage("mysqldump exited with code: " + exitCode);

                logService.createLog(operatorId, operatorName, OperationType.BACKUP.getValue(),
                        "backup", String.valueOf(record.getId()), fileName,
                        "数据库备份失败", null, "ERROR");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record.setStatus(BackupStatus.FAILED.getValue());
            record.setFinishedAt(LocalDateTime.now());
            record.setErrorMessage("备份被中断");
            log.error("Backup interrupted", e);
        } catch (Exception e) {
            record.setStatus(BackupStatus.FAILED.getValue());
            record.setFinishedAt(LocalDateTime.now());
            record.setErrorMessage(e.getMessage());
            log.error("Backup failed", e);
        }

        backupRecordMapper.updateById(record);
        return ApiResponse.success(record.getId());
    }

    @Override
    public ApiResponse<Void> restoreBackup(Long backupId, String operatorId, String operatorName) {
        BackupRecord record = backupRecordMapper.selectById(backupId);
        if (record == null) {
            throw new ResourceNotFoundException("备份记录不存在: " + backupId);
        }
        if (!BackupStatus.SUCCESS.getValue().equals(record.getStatus())) {
            throw new BusinessException("只能恢复成功的备份");
        }

        File backupFile = new File(record.getFilePath());
        if (!backupFile.exists()) {
            throw new BusinessException("备份文件不存在: " + record.getFilePath());
        }

        try {
            String dbName = extractDbName(dbUrl);
            ProcessBuilder pb = new ProcessBuilder(
                    "mysql", "-u" + dbUsername,
                    "-p",  // Use MYSQL_PWD env var
                    dbName
            );
            // Pass password via environment variable
            pb.environment().put("MYSQL_PWD", dbPassword);
            pb.redirectInput(backupFile);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain process output in background to prevent deadlock
            Thread outputDrainer = new Thread(() -> {
                try (var is = process.getInputStream()) {
                    is.transferTo(OutputStream.nullOutputStream());
                } catch (IOException e) {
                    log.warn("Error draining restore process output", e);
                }
            });
            outputDrainer.setDaemon(true);
            outputDrainer.start();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logService.createLog(operatorId, operatorName, OperationType.RESTORE.getValue(),
                        "backup", String.valueOf(backupId), record.getFileName(),
                        "数据库恢复成功: " + record.getFileName(), null, "WARN");
            } else {
                throw new BusinessException("数据库恢复失败，退出码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("数据库恢复被中断");
        } catch (IOException e) {
            throw new BusinessException("数据库恢复失败: " + e.getMessage());
        }

        return ApiResponse.success();
    }

    @Override
    public void executeScheduledBackup() {
        log.info("Executing scheduled backup...");
        executeBackup("system", "系统", TriggerType.SCHEDULED.getValue());
    }

    private String extractDbName(String url) {
        // jdbc:mysql://localhost:13306/sms_nexus?params -> sms_nexus
        int lastSlash = url.lastIndexOf("/");
        if (lastSlash < 0) {
            throw new BusinessException("无法从JDBC URL解析数据库名: " + url);
        }
        String clean = url.substring(lastSlash + 1);
        if (clean.contains("?")) {
            clean = clean.substring(0, clean.indexOf("?"));
        }
        // Validate extracted name does not contain URL-like characters
        if (clean.isEmpty() || clean.contains(":") || clean.contains("/")) {
            throw new BusinessException("无法从JDBC URL解析数据库名: " + url);
        }
        return clean;
    }

    private int parseRetentionDays(SystemSetting retention) {
        if (retention == null || retention.getSettingValue() == null) {
            return DEFAULT_RETENTION_DAYS;
        }
        try {
            return Integer.parseInt(retention.getSettingValue());
        } catch (NumberFormatException e) {
            log.warn("Invalid backup_retention_days value: {}, using default: {}",
                    retention.getSettingValue(), DEFAULT_RETENTION_DAYS);
            return DEFAULT_RETENTION_DAYS;
        }
    }
}
