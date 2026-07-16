package com.sms.nexus.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sms.nexus.entity.SystemSetting;
import com.sms.nexus.mapper.SystemSettingMapper;
import com.sms.nexus.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupScheduleTask {

    private final LogService logService;
    private final SystemSettingMapper systemSettingMapper;

    /**
     * Scheduled log cleanup - runs every day at 3:00 AM.
     * Cleans up logs older than the configured retention period.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledLogCleanup() {
        log.info("Starting scheduled log cleanup...");

        try {
            // Get retention days from settings
            int retentionDays = 90; // default
            LambdaQueryWrapper<SystemSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemSetting::getSettingKey, "log_retention_days");
            SystemSetting setting = systemSettingMapper.selectOne(wrapper);

            if (setting != null) {
                try {
                    retentionDays = Integer.parseInt(setting.getSettingValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid log_retention_days value: {}, using default 90", setting.getSettingValue());
                }
            }

            logService.cleanupLogs(retentionDays, "system", "系统定时任务");
            log.info("Scheduled log cleanup completed, retention days: {}", retentionDays);
        } catch (Exception e) {
            log.error("Scheduled log cleanup failed", e);
        }
    }
}
