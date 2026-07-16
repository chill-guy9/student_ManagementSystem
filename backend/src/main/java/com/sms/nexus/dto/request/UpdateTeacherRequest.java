package com.sms.nexus.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTeacherRequest {

    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String department;
    private String title;
    private String avatar;
    private Integer status;
}
