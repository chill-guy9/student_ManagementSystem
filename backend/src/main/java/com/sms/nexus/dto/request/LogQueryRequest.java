package com.sms.nexus.dto.request;

import lombok.Data;

@Data
public class LogQueryRequest {

    private String operatorName;
    private String username;
    private String operationType;
    private String operation;
    private String targetType;
    private String module;
    private String level;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;
    private Integer page = 1;
    private Integer pageSize = 10;

    // Resolve frontend parameter names to backend names
    public String getEffectiveOperatorName() {
        return StringUtils.hasText(operatorName) ? operatorName : username;
    }

    public String getEffectiveOperationType() {
        return StringUtils.hasText(operationType) ? operationType : operation;
    }

    public String getEffectiveTargetType() {
        return StringUtils.hasText(targetType) ? targetType : module;
    }

    public String getEffectiveStartDate() {
        return StringUtils.hasText(startDate) ? startDate : startTime;
    }

    public String getEffectiveEndDate() {
        return StringUtils.hasText(endDate) ? endDate : endTime;
    }

    private static class StringUtils {
        static boolean hasText(String str) {
            return str != null && !str.isBlank();
        }
    }
}
