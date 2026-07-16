package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminVO {

    private String adminId;
    private String username;
    private String realName;
    private String role;
    private String roleLabel;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
}
