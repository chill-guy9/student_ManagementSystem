package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum LogLevel {

    INFO("INFO", "信息"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误");

    private final String value;
    private final String label;

    LogLevel(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
