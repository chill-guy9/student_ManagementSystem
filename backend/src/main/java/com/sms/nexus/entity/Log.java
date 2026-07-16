package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logs")
public class Log {

    @TableId("log_id")
    private String logId;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operation_type")
    private String operationType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("target_name")
    private String targetName;

    @TableField("detail")
    private String detail;

    @TableField("ip")
    private String ip;

    @TableField("level")
    private String level;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
