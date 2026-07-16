package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class UserGrowthVO {

    private String date;
    private int students;
    private int teachers;
    private int admins;
}
