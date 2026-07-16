package com.sms.nexus.common.enums;

import lombok.Getter;

@Getter
public enum Gender {

    FEMALE(0, "女"),
    MALE(1, "男");

    private final int value;
    private final String label;

    Gender(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static String getLabel(Integer value) {
        if (value == null) return "未知";
        return value == 1 ? MALE.label : FEMALE.label;
    }
}
