package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum BackupStatus {

    PENDING("PENDING", "等待中"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String value;
    private final String label;

    BackupStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
