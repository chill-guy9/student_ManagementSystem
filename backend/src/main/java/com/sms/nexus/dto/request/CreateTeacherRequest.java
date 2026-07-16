package com.sms.nexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTeacherRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String email;

    @NotBlank(message = "院系不能为空")
    private String department;

    private String title;
    private String avatar;
}
