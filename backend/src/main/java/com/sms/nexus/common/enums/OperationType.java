package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum OperationType {

    CREATE("CREATE", "创建"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入"),
    BACKUP("BACKUP", "备份"),
    RESTORE("RESTORE", "恢复"),
    SYSTEM("SYSTEM", "系统");

    private final String value;
    private final String label;

    OperationType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static OperationType fromValue(String value) {
        for (OperationType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown operation type: " + value);
    }
}
