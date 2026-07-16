package com.sms.nexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAdminRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String email;
    private String phone;
}
