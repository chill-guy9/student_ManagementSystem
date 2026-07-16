package com.sms.nexus.service;

import com.sms.nexus.dto.response.*;

import java.util.List;

public interface DashboardService {

    ApiResponse<DashboardStatsVO> getStats();

    ApiResponse<List<UserGrowthVO>> getUserGrowth(int days);

    ApiResponse<List<LogDistributionVO>> getLogDistribution(int days);

    ApiResponse<SystemLoadVO> getSystemLoad();

    ApiResponse<List<LogVO>> getRecentLogs(int limit);
}
