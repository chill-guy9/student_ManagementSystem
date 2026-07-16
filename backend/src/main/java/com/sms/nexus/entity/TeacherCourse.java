package com.sms.nexus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teacher_courses")
public class TeacherCourse {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private String teacherId;

    @TableField("course_name")
    private String courseName;

    @TableField("course_code")
    private String courseCode;

    @TableField("semester")
    private String semester;

    @TableField("hours")
    private Integer hours;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
