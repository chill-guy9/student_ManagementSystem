package com.sms.nexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateStudentRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;

    @NotBlank(message = "班级不能为空")
    private String className;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "年级不能为空")
    private String grade;

    private LocalDate enrollmentDate;
    private String status;
    private String avatar;
}
