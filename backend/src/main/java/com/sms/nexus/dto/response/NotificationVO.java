package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private String notificationId;
    private String title;
    private String content;
    private String type;
    private String typeLabel;
    private Integer isRead;
    private LocalDateTime createdAt;
}
