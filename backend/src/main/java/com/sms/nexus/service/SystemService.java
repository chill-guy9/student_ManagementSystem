package com.sms.nexus.service;

import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.SystemInfoVO;

public interface SystemService {

    ApiResponse<SystemInfoVO> getSystemInfo();

    ApiResponse<Object> getHealth();
}
