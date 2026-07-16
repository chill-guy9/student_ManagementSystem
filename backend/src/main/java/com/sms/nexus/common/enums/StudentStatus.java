package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum StudentStatus {

    ACTIVE("active", "在读"),
    SUSPENDED("suspended", "休学"),
    GRADUATED("graduated", "毕业"),
    DROPPED("dropped", "退学");

    private final String value;
    private final String label;

    StudentStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static StudentStatus fromValue(String value) {
        for (StudentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
