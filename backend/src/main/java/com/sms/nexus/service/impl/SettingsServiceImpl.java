package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.dto.request.NotificationSettingsRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.entity.NotificationConfig;
import com.sms.nexus.entity.SystemSetting;
import com.sms.nexus.mapper.NotificationConfigMapper;
import com.sms.nexus.mapper.SystemSettingMapper;
import com.sms.nexus.service.LogService;
import com.sms.nexus.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final NotificationConfigMapper notificationConfigMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final LogService logService;

    @Override
    public ApiResponse<Map<String, Object>> getAllSettings() {
        Map<String, Object> result = new HashMap<>();

        // Notification configs
        LambdaQueryWrapper<NotificationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NotificationConfig::getId);
        var configs = notificationConfigMapper.selectList(wrapper);
        Map<String, String> notifMap = new LinkedHashMap<>();
        for (NotificationConfig config : configs) {
            notifMap.put(config.getConfigKey(), config.getConfigValue());
        }
        result.put("notifications", notifMap);

        // System settings
        var settings = systemSettingMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> sysMap = new LinkedHashMap<>();
        for (SystemSetting setting : settings) {
            sysMap.put(setting.getSettingKey(), setting.getSettingValue());
        }
        result.put("system", sysMap);

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<Map<String, String>> getNotificationSettings() {
        LambdaQueryWrapper<NotificationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NotificationConfig::getId);
        var configs = notificationConfigMapper.selectList(wrapper);

        Map<String, String> result = new LinkedHashMap<>();
        for (NotificationConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<Void> updateNotificationSettings(NotificationSettingsRequest request,
                                                         String operatorId, String operatorName) {
        if (request.getConfigs() == null || request.getConfigs().isEmpty()) {
            return ApiResponse.success();
        }

        for (Map.Entry<String, String> entry : request.getConfigs().entrySet()) {
            LambdaQueryWrapper<NotificationConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NotificationConfig::getConfigKey, entry.getKey());
            NotificationConfig config = notificationConfigMapper.selectOne(wrapper);

            if (config != null) {
                config.setConfigValue(entry.getValue());
                notificationConfigMapper.updateById(config);
            }
        }

        logService.createLog(operatorId, operatorName, OperationType.UPDATE.getValue(),
                "settings", null, "通知配置",
                "更新通知配置", null, "INFO");

        return ApiResponse.success();
    }
}
