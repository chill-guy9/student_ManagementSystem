package com.sms.nexus.controller;

import com.sms.nexus.common.util.CsvExporter;
import com.sms.nexus.dto.request.LogQueryRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.LogVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.mapper.LogMapper;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "操作日志查询接口")
public class LogController {

    private final LogService logService;
    private final LogMapper logMapper;

    @GetMapping
    @Operation(summary = "查询日志列表")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor')")
    public ApiResponse<PageResult<LogVO>> listLogs(LogQueryRequest request) {
        return logService.listLogs(request);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取日志统计")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor')")
    public ApiResponse<Map<String, Object>> getLogStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("levelDistribution", logMapper.countByLevel(startDate, endDate));
        stats.put("hourlyDistribution", logMapper.countByHour(startDate, endDate));
        stats.put("topOperators", logMapper.topOperators(startDate, endDate, 5));
        return ApiResponse.success(stats);
    }

    @GetMapping("/export")
    @Operation(summary = "导出日志")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor')")
    public void exportLogs(LogQueryRequest request, HttpServletResponse response) throws IOException {
        // Fetch all matching logs (use large page size for export)
        request.setPage(1);
        request.setPageSize(10000);
        ApiResponse<PageResult<LogVO>> result = logService.listLogs(request);
        List<LogVO> logs = result.getData() != null ? result.getData().getRecords() : Collections.emptyList();

        String[] headers = {"日志ID", "操作人", "操作类型", "目标类型", "目标名称", "IP", "级别", "时间"};
        List<String[]> data = new ArrayList<>();
        for (LogVO log : logs) {
            data.add(new String[]{
                    log.getLogId(),
                    log.getOperatorName(),
                    log.getOperationTypeLabel() != null ? log.getOperationTypeLabel() : log.getOperationType(),
                    log.getTargetType(),
                    log.getTargetName(),
                    log.getIp(),
                    log.getLevel(),
                    log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""
            });
        }

        CsvExporter.export(response, "logs_export.csv", headers, data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取日志详情")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor')")
    public ApiResponse<LogVO> getLog(@PathVariable("id") String logId) {
        return logService.getLog(logId);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "获取日志关联变更")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor')")
    public ApiResponse<List<ChangeVO>> getLogChanges(@PathVariable("id") String logId) {
        return logService.getLogChanges(logId);
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期日志")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<Void> cleanupLogs(@RequestParam(defaultValue = "90") int retentionDays,
                                          Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return logService.cleanupLogs(retentionDays, operatorId, operatorName);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
