package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String adminId;
    private String username;
    private String realName;
    private String role;
    private String avatar;

    public LoginResponse(String token, String adminId, String username, String realName, String role, String avatar) {
        this.token = token;
        this.adminId = adminId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.avatar = avatar;
    }
}
