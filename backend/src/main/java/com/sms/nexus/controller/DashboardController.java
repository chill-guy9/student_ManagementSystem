package com.sms.nexus.controller;

import com.sms.nexus.dto.response.*;
import com.sms.nexus.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "Dashboard数据接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取统计概览")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<DashboardStatsVO> getStats() {
        return dashboardService.getStats();
    }

    @GetMapping("/user-growth")
    @Operation(summary = "获取用户增长趋势")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<List<UserGrowthVO>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        return dashboardService.getUserGrowth(days);
    }

    @GetMapping("/log-distribution")
    @Operation(summary = "获取日志分布")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<List<LogDistributionVO>> getLogDistribution(
            @RequestParam(defaultValue = "30") int days) {
        return dashboardService.getLogDistribution(days);
    }

    @GetMapping("/system-load")
    @Operation(summary = "获取系统负载")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<SystemLoadVO> getSystemLoad() {
        return dashboardService.getSystemLoad();
    }

    @GetMapping("/recent-logs")
    @Operation(summary = "获取最近日志")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<List<LogVO>> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentLogs(limit);
    }

    @GetMapping("/trends")
    @Operation(summary = "获取增长趋势（别名）")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<List<UserGrowthVO>> getTrends(
            @RequestParam(defaultValue = "30") int days) {
        return dashboardService.getUserGrowth(days);
    }

    @GetMapping("/activities")
    @Operation(summary = "获取最近活动（别名）")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<List<LogVO>> getActivities(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentLogs(limit);
    }
}
