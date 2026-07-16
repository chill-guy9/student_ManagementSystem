package com.sms.nexus.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStudentRequest {

    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;
    private String className;
    private String major;
    private String grade;
    private LocalDate enrollmentDate;
    private String status;
    private String avatar;
}
