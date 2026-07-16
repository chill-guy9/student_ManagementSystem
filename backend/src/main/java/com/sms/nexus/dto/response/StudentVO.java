package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentVO {

    private String studentId;
    private String name;
    private Integer gender;
    private String genderLabel;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;
    private String className;
    private String major;
    private String grade;
    private LocalDate enrollmentDate;
    private String status;
    private String statusLabel;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
