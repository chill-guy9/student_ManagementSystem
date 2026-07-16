package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.dto.request.LogQueryRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.LogVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.entity.Log;
import com.sms.nexus.entity.LogChange;
import com.sms.nexus.entity.NotificationConfig;
import com.sms.nexus.mapper.LogChangeMapper;
import com.sms.nexus.mapper.LogMapper;
import com.sms.nexus.mapper.NotificationConfigMapper;
import com.sms.nexus.service.LogService;
import com.sms.nexus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;
    private final LogChangeMapper logChangeMapper;
    private final IdGenerator idGenerator;
    private final NotificationService notificationService;
    private final NotificationConfigMapper notificationConfigMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void createLog(String operatorId, String operatorName, String operationType,
                          String targetType, String targetId, String targetName,
                          String detail, String ip, String level) {
        Log logEntry = new Log();
        logEntry.setLogId(idGenerator.nextLogId());
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setOperationType(operationType);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setTargetName(targetName);
        logEntry.setDetail(detail);
        logEntry.setIp(ip);
        logEntry.setLevel(level);

        logMapper.insert(logEntry);

        // Trigger notification based on config
        try {
            triggerNotificationIfNeeded(logEntry);
        } catch (Exception e) {
            log.error("Failed to trigger notification", e);
        }
    }

    @Override
    public void createLogWithChanges(String operatorId, String operatorName, String operationType,
                                      String targetType, String targetId, String targetName,
                                      String detail, String ip, String level,
                                      List<ChangeVO> changes) {
        // Create the log entry and use the generated logId directly
        Log logEntry = new Log();
        logEntry.setLogId(idGenerator.nextLogId());
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setOperationType(operationType);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setTargetName(targetName);
        logEntry.setDetail(detail);
        logEntry.setIp(ip);
        logEntry.setLevel(level);

        logMapper.insert(logEntry);

        // Trigger notification based on config
        try {
            triggerNotificationIfNeeded(logEntry);
        } catch (Exception e) {
            log.error("Failed to trigger notification", e);
        }

        // Use the logId directly instead of re-querying to avoid race condition
        if (changes != null && !changes.isEmpty()) {
            for (ChangeVO change : changes) {
                LogChange logChange = new LogChange();
                logChange.setLogId(logEntry.getLogId());
                logChange.setFieldName(change.getFieldName());
                logChange.setOldValue(change.getOldValue());
                logChange.setNewValue(change.getNewValue());
                logChangeMapper.insert(logChange);
            }
        }
    }

    @Override
    public ApiResponse<PageResult<LogVO>> listLogs(LogQueryRequest request) {
        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getEffectiveOperatorName())) {
            wrapper.like(Log::getOperatorName, request.getEffectiveOperatorName());
        }
        if (StringUtils.hasText(request.getEffectiveOperationType())) {
            wrapper.eq(Log::getOperationType, request.getEffectiveOperationType());
        }
        if (StringUtils.hasText(request.getEffectiveTargetType())) {
            wrapper.eq(Log::getTargetType, request.getEffectiveTargetType());
        }
        if (StringUtils.hasText(request.getLevel())) {
            wrapper.in(Log::getLevel, request.getLevel().split(","));
        }
        if (StringUtils.hasText(request.getEffectiveStartDate())) {
            wrapper.ge(Log::getCreatedAt, LocalDateTime.parse(request.getEffectiveStartDate() + " 00:00:00", FORMATTER));
        }
        if (StringUtils.hasText(request.getEffectiveEndDate())) {
            wrapper.le(Log::getCreatedAt, LocalDateTime.parse(request.getEffectiveEndDate() + " 23:59:59", FORMATTER));
        }

        wrapper.orderByDesc(Log::getCreatedAt);

        Page<Log> page = logMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()), wrapper);

        List<LogVO> voList = new ArrayList<>();
        for (Log l : page.getRecords()) {
            voList.add(toLogVO(l));
        }

        PageResult<LogVO> result = new PageResult<>(voList, page.getTotal(),
                page.getCurrent(), page.getSize());

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<LogVO> getLog(String logId) {
        Log logEntry = logMapper.selectById(logId);
        if (logEntry == null) {
            return ApiResponse.error(404, "日志不存在");
        }
        return ApiResponse.success(toLogVO(logEntry));
    }

    @Override
    public ApiResponse<List<ChangeVO>> getLogChanges(String logId) {
        LambdaQueryWrapper<LogChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogChange::getLogId, logId);
        List<LogChange> changes = logChangeMapper.selectList(wrapper);

        List<ChangeVO> voList = new ArrayList<>();
        for (LogChange c : changes) {
            ChangeVO vo = new ChangeVO();
            vo.setId(c.getId());
            vo.setLogId(c.getLogId());
            vo.setFieldName(c.getFieldName());
            vo.setFieldLabel(c.getFieldName()); // Could map to Chinese labels
            vo.setOldValue(c.getOldValue());
            vo.setNewValue(c.getNewValue());
            voList.add(vo);
        }

        return ApiResponse.success(voList);
    }

    @Override
    public ApiResponse<Void> cleanupLogs(int retentionDays, String operatorId, String operatorName) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // First, find log IDs that will be deleted
        LambdaQueryWrapper<Log> findWrapper = new LambdaQueryWrapper<>();
        findWrapper.lt(Log::getCreatedAt, cutoff).select(Log::getLogId);
        List<Log> logsToDelete = logMapper.selectList(findWrapper);
        List<String> logIds = logsToDelete.stream().map(Log::getLogId).toList();

        if (!logIds.isEmpty()) {
            // Delete related log_changes first to avoid orphaned records
            LambdaQueryWrapper<LogChange> changeWrapper = new LambdaQueryWrapper<>();
            changeWrapper.in(LogChange::getLogId, logIds);
            logChangeMapper.delete(changeWrapper);

            // Then delete the logs
            LambdaQueryWrapper<Log> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.lt(Log::getCreatedAt, cutoff);
            int deleted = logMapper.delete(deleteWrapper);

            createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                    "log", null, null,
                    "清理过期日志，保留" + retentionDays + "天，删除" + deleted + "条记录",
                    null, "INFO");
        }

        return ApiResponse.success();
    }

    private LogVO toLogVO(Log logEntry) {
        LogVO vo = new LogVO();
        vo.setLogId(logEntry.getLogId());
        vo.setOperatorId(logEntry.getOperatorId());
        vo.setOperatorName(logEntry.getOperatorName());
        vo.setOperationType(logEntry.getOperationType());
        try {
            vo.setOperationTypeLabel(com.sms.nexus.common.enums.OperationType.fromValue(logEntry.getOperationType()).getLabel());
        } catch (Exception e) {
            vo.setOperationTypeLabel(logEntry.getOperationType());
        }
        vo.setTargetType(logEntry.getTargetType());
        vo.setTargetId(logEntry.getTargetId());
        vo.setTargetName(logEntry.getTargetName());
        vo.setDetail(logEntry.getDetail());
        vo.setIp(logEntry.getIp());
        vo.setLevel(logEntry.getLevel());
        vo.setCreatedAt(logEntry.getCreatedAt());
        return vo;
    }

    private void triggerNotificationIfNeeded(Log logEntry) {
        String operationType = logEntry.getOperationType();
        String configKey = null;
        String notifType = null;

        if ("LOGIN".equals(operationType) || "LOGOUT".equals(operationType)) {
            configKey = "login_notification";
            notifType = "LOGIN";
        } else if ("CREATE".equals(operationType) || "UPDATE".equals(operationType) || "DELETE".equals(operationType)) {
            configKey = "operation_notification";
            notifType = "OPERATION";
        } else if ("BACKUP".equals(operationType) || "RESTORE".equals(operationType)) {
            configKey = "backup_notification";
            notifType = "BACKUP";
        } else if ("ERROR".equals(logEntry.getLevel())) {
            configKey = "system_alert";
            notifType = "ALERT";
        }

        if (configKey != null) {
            LambdaQueryWrapper<NotificationConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NotificationConfig::getConfigKey, configKey);
            NotificationConfig config = notificationConfigMapper.selectOne(wrapper);
            if (config != null && "true".equals(config.getConfigValue())) {
                String title = logEntry.getOperationType() + " - " + (logEntry.getTargetName() != null ? logEntry.getTargetName() : logEntry.getDetail());
                String content = logEntry.getDetail();
                // Send to the operator themselves
                if (logEntry.getOperatorId() != null) {
                    notificationService.createNotification(logEntry.getOperatorId(), title, content, notifType);
                }
            }
        }
    }
}
