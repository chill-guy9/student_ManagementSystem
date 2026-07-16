package com.sms.nexus.service;

import com.sms.nexus.dto.request.LogQueryRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.LogVO;
import com.sms.nexus.dto.response.PageResult;

import java.util.List;

public interface LogService {

    void createLog(String operatorId, String operatorName, String operationType,
                   String targetType, String targetId, String targetName,
                   String detail, String ip, String level);

    void createLogWithChanges(String operatorId, String operatorName, String operationType,
                              String targetType, String targetId, String targetName,
                              String detail, String ip, String level,
                              List<ChangeVO> changes);

    ApiResponse<PageResult<LogVO>> listLogs(LogQueryRequest request);

    ApiResponse<LogVO> getLog(String logId);

    ApiResponse<List<ChangeVO>> getLogChanges(String logId);

    ApiResponse<Void> cleanupLogs(int retentionDays, String operatorId, String operatorName);
}
