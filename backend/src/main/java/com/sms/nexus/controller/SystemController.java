package com.sms.nexus.controller;

import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.SystemInfoVO;
import com.sms.nexus.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "系统信息和健康检查")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/info")
    @Operation(summary = "获取系统信息")
    public ApiResponse<SystemInfoVO> getSystemInfo() {
        return systemService.getSystemInfo();
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ApiResponse<Object> getHealth() {
        return systemService.getHealth();
    }
}
