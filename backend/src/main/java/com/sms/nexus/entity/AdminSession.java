package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_sessions")
public class AdminSession {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("admin_id")
    private String adminId;

    @TableField("token")
    private String token;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("expired_at")
    private LocalDateTime expiredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
