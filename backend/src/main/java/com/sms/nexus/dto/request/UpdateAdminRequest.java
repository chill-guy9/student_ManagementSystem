package com.sms.nexus.dto.request;

import lombok.Data;

@Data
public class UpdateAdminRequest {

    private String realName;
    private String role;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private String password;
}
