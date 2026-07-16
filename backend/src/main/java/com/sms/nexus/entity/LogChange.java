package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("log_changes")
public class LogChange {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("log_id")
    private String logId;

    @TableField("field_name")
    private String fieldName;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
