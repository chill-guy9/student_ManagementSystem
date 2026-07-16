package com.sms.nexus.service;

import com.sms.nexus.dto.request.NotificationSettingsRequest;
import com.sms.nexus.dto.response.ApiResponse;

import java.util.Map;

public interface SettingsService {

    ApiResponse<Map<String, Object>> getAllSettings();

    ApiResponse<Map<String, String>> getNotificationSettings();

    ApiResponse<Void> updateNotificationSettings(NotificationSettingsRequest request, String operatorId, String operatorName);
}
