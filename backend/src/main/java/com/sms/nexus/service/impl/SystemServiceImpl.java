package com.sms.nexus.service.impl;

import com.sms.nexus.common.constant.AppConstant;
import com.sms.nexus.common.util.SystemInfoUtil;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.SystemInfoVO;
import com.sms.nexus.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    @Override
    public ApiResponse<SystemInfoVO> getSystemInfo() {
        SystemInfoVO vo = new SystemInfoVO();
        vo.setAppName(AppConstant.APP_NAME);
        vo.setVersion(AppConstant.APP_VERSION);
        vo.setJavaVersion(SystemInfoUtil.getJavaVersion());
        vo.setOsName(SystemInfoUtil.getOsName());
        vo.setOsArch(SystemInfoUtil.getOsArch());
        vo.setUptime(SystemInfoUtil.getUptime());
        vo.setActiveConnections(0); // Could be derived from HikariCP metrics
        vo.setServerTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return ApiResponse.success(vo);
    }

    @Override
    public ApiResponse<Object> getHealth() {
        var health = java.util.Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        return ApiResponse.success(health);
    }
}
