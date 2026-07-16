package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("students")
public class Student {

    @TableId("student_id")
    private String studentId;

    @TableField("name")
    private String name;

    @TableField("gender")
    private Integer gender;

    @TableField("birth_date")
    private LocalDate birthDate;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("address")
    private String address;

    @TableField("class_name")
    private String className;

    @TableField("major")
    private String major;

    @TableField("grade")
    private String grade;

    @TableField("enrollment_date")
    private LocalDate enrollmentDate;

    @TableField("status")
    private String status;

    @TableField("avatar")
    private String avatar;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
