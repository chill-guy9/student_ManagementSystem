package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogVO {

    private String logId;
    private String operatorId;
    private String operatorName;
    private String operationType;
    private String operationTypeLabel;
    private String targetType;
    private String targetId;
    private String targetName;
    private String detail;
    private String ip;
    private String level;
    private LocalDateTime createdAt;
}
