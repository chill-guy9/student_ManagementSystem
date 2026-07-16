package com.sms.nexus.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationSettingsRequest {

    private Map<String, String> configs;
}
