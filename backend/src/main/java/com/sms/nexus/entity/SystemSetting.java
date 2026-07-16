package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_settings")
public class SystemSetting {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("setting_key")
    private String settingKey;

    @TableField("setting_value")
    private String settingValue;

    @TableField("description")
    private String description;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
