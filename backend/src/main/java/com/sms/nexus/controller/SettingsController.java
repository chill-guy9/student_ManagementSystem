package com.sms.nexus.controller;

import com.sms.nexus.dto.request.NotificationSettingsRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "设置管理", description = "系统设置接口")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "获取所有设置")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'read_only')")
    public ApiResponse<Map<String, Object>> getAllSettings() {
        return settingsService.getAllSettings();
    }

    @GetMapping("/notifications")
    @Operation(summary = "获取通知设置")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'read_only')")
    public ApiResponse<Map<String, String>> getNotificationSettings() {
        return settingsService.getNotificationSettings();
    }

    @PutMapping("/notifications")
    @Operation(summary = "更新通知设置")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<Void> updateNotificationSettings(@Valid @RequestBody NotificationSettingsRequest request,
                                                         Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return settingsService.updateNotificationSettings(request, operatorId, operatorName);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
