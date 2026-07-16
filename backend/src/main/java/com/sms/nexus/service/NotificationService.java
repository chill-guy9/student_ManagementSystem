package com.sms.nexus.service;

import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.NotificationVO;
import com.sms.nexus.dto.response.PageResult;

public interface NotificationService {

    ApiResponse<PageResult<NotificationVO>> listNotifications(String adminId, Boolean unread, int page, int pageSize);

    ApiResponse<Long> getUnreadCount(String adminId);

    ApiResponse<Void> markAsRead(String notificationId, String adminId);

    ApiResponse<Void> markAllAsRead(String adminId);

    ApiResponse<Void> deleteNotification(String notificationId, String adminId);

    void createNotification(String adminId, String title, String content, String type);
}
