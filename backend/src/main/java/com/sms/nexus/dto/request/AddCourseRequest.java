package com.sms.nexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCourseRequest {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String courseCode;

    private String semester;

    private Integer hours;
}
