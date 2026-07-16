package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum AdminRole {

    SUPER_ADMIN("super_admin", "超级管理员"),
    USER_ADMIN("user_admin", "用户管理员"),
    LOG_AUDITOR("log_auditor", "日志审计员"),
    READ_ONLY("read_only", "只读用户");

    private final String value;
    private final String label;

    AdminRole(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AdminRole fromValue(String value) {
        for (AdminRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown admin role: " + value);
    }
}
