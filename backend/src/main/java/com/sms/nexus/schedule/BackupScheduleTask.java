package com.sms.nexus.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sms.nexus.entity.SystemSetting;
import com.sms.nexus.mapper.SystemSettingMapper;
import com.sms.nexus.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduleTask {

    private final BackupService backupService;
    private final SystemSettingMapper systemSettingMapper;

    /**
     * Scheduled backup - runs every day at 2:00 AM.
     * Only executes if auto_backup_enabled is set to true in system_settings.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        log.info("Checking if scheduled backup is enabled...");

        // Check the auto_backup_enabled setting before executing
        SystemSetting autoBackup = systemSettingMapper.selectOne(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "auto_backup_enabled"));

        if (autoBackup == null || !"true".equals(autoBackup.getSettingValue())) {
            log.info("Scheduled backup is disabled (auto_backup_enabled={}), skipping.",
                    autoBackup != null ? autoBackup.getSettingValue() : "null");
            return;
        }

        try {
            backupService.executeScheduledBackup();
        } catch (Exception e) {
            log.error("Scheduled backup failed", e);
        }
    }
}
