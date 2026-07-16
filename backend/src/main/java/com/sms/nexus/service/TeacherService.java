package com.sms.nexus.service;

import com.sms.nexus.dto.request.AddCourseRequest;
import com.sms.nexus.dto.request.CreateTeacherRequest;
import com.sms.nexus.dto.request.UpdateTeacherRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.TeacherVO;
import jakarta.servlet.http.HttpServletResponse;

public interface TeacherService {

    ApiResponse<PageResult<TeacherVO>> listTeachers(String keyword, String department, String title, Integer status, Integer page, Integer pageSize);

    ApiResponse<TeacherVO> getTeacher(String teacherId);

    ApiResponse<TeacherVO> createTeacher(CreateTeacherRequest request, String operatorId, String operatorName);

    ApiResponse<TeacherVO> updateTeacher(String teacherId, UpdateTeacherRequest request, String operatorId, String operatorName);

    ApiResponse<Void> deleteTeacher(String teacherId, String operatorId, String operatorName);

    ApiResponse<Void> exportTeachers(String keyword, String department, String title, Integer status, HttpServletResponse response);

    ApiResponse<TeacherVO.CourseInfoList> getTeacherCourses(String teacherId);

    ApiResponse<TeacherVO.CourseInfo> addTeacherCourse(String teacherId, AddCourseRequest request, String operatorId, String operatorName);

    ApiResponse<Void> removeTeacherCourse(String teacherId, Long courseId, String operatorId, String operatorName);
}
