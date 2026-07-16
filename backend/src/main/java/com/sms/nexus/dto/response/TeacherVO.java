package com.sms.nexus.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeacherVO {

    private String teacherId;
    private String name;
    private Integer gender;
    private String genderLabel;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String department;
    private String title;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CourseInfo> courses;

    @Data
    public static class CourseInfo {
        private Long id;
        private String courseName;
        private String courseCode;
        private String semester;
        private Integer hours;
    }

    @Data
    public static class CourseInfoList {
        private List<CourseInfo> courses;
    }
}
