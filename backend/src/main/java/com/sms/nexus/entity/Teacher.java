package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("teachers")
public class Teacher {

    @TableId("teacher_id")
    private String teacherId;

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

    @TableField("department")
    private String department;

    @TableField("title")
    private String title;

    @TableField("avatar")
    private String avatar;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
