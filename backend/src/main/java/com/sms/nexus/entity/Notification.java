package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("notification_id")
    private String notificationId;

    @TableField("admin_id")
    private String adminId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type;

    @TableField("is_read")
    private Integer isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
