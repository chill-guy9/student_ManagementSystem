package com.sms.nexus.controller;

import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.NotificationVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "站内通知接口")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public ApiResponse<PageResult<NotificationVO>> listNotifications(
            @RequestParam(required = false) Boolean unread,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Authentication authentication) {
        String adminId = authentication.getName();
        return notificationService.listNotifications(adminId, unread, page, pageSize);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ApiResponse<Long> getUnreadCount(Authentication authentication) {
        String adminId = authentication.getName();
        return notificationService.getUnreadCount(adminId);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    public ApiResponse<Void> markAsRead(@PathVariable("id") String notificationId,
                                        Authentication authentication) {
        String adminId = authentication.getName();
        return notificationService.markAsRead(notificationId, adminId);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        String adminId = authentication.getName();
        return notificationService.markAllAsRead(adminId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ApiResponse<Void> deleteNotification(@PathVariable("id") String notificationId,
                                                 Authentication authentication) {
        String adminId = authentication.getName();
        return notificationService.deleteNotification(notificationId, adminId);
    }
}
