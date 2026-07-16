package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum TriggerType {

    MANUAL("MANUAL", "手动"),
    SCHEDULED("SCHEDULED", "定时");

    private final String value;
    private final String label;

    TriggerType(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
