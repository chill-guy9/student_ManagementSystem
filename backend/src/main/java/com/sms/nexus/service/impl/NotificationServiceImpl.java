package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.NotificationVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.entity.Notification;
import com.sms.nexus.mapper.NotificationMapper;
import com.sms.nexus.service.NotificationService;
import com.sms.nexus.websocket.NotificationStreamHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final IdGenerator idGenerator;
    private final NotificationStreamHandler notificationStreamHandler;

    @Override
    public ApiResponse<PageResult<NotificationVO>> listNotifications(String adminId, Boolean unread, int page, int pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getAdminId, adminId);
        if (unread != null && unread) {
            wrapper.eq(Notification::getIsRead, 0);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<NotificationVO> voList = new ArrayList<>();
        for (Notification n : result.getRecords()) {
            voList.add(toVO(n));
        }

        return ApiResponse.success(new PageResult<>(voList, result.getTotal(),
                result.getCurrent(), result.getSize()));
    }

    @Override
    public ApiResponse<Long> getUnreadCount(String adminId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getAdminId, adminId)
               .eq(Notification::getIsRead, 0);
        Long count = notificationMapper.selectCount(wrapper);
        return ApiResponse.success(count);
    }

    @Override
    public ApiResponse<Void> markAsRead(String notificationId, String adminId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationId, notificationId)
               .eq(Notification::getAdminId, adminId);
        Notification notification = notificationMapper.selectOne(wrapper);
        if (notification == null) {
            return ApiResponse.error(404, "通知不存在");
        }
        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> markAllAsRead(String adminId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getAdminId, adminId)
               .eq(Notification::getIsRead, 0);
        // Batch update: set all unread notifications to read in one query
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, wrapper);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> deleteNotification(String notificationId, String adminId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationId, notificationId)
               .eq(Notification::getAdminId, adminId);
        int deleted = notificationMapper.delete(wrapper);
        if (deleted == 0) {
            return ApiResponse.error(404, "通知不存在");
        }
        return ApiResponse.success();
    }

    @Override
    public void createNotification(String adminId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setNotificationId(idGenerator.nextNotificationId());
        notification.setAdminId(adminId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);

        // Broadcast via WebSocket
        try {
            notificationStreamHandler.sendToAdmin(adminId, Map.of(
                "type", "notification.new",
                "data", Map.of(
                    "notificationId", notification.getNotificationId(),
                    "title", notification.getTitle(),
                    "content", notification.getContent(),
                    "type", notification.getType(),
                    "createdAt", notification.getCreatedAt().toString()
                )
            ));
        } catch (Exception e) {
            log.error("Failed to broadcast notification via WebSocket", e);
        }
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setNotificationId(n.getNotificationId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setType(n.getType());
        vo.setTypeLabel(getTypeLabel(n.getType()));
        vo.setIsRead(n.getIsRead());
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }

    private String getTypeLabel(String type) {
        return switch (type) {
            case "SYSTEM" -> "系统通知";
            case "LOGIN" -> "登录通知";
            case "OPERATION" -> "操作通知";
            case "BACKUP" -> "备份通知";
            case "ALERT" -> "告警通知";
            default -> type;
        };
    }
}
