package com.sms.nexus.service;

import com.sms.nexus.dto.request.CreateStudentRequest;
import com.sms.nexus.dto.request.StudentQueryRequest;
import com.sms.nexus.dto.request.UpdateStudentRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.StudentVO;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface StudentService {

    ApiResponse<PageResult<StudentVO>> listStudents(StudentQueryRequest request);

    ApiResponse<StudentVO> getStudent(String studentId);

    ApiResponse<StudentVO> createStudent(CreateStudentRequest request, String operatorId, String operatorName);

    ApiResponse<StudentVO> updateStudent(String studentId, UpdateStudentRequest request, String operatorId, String operatorName);

    ApiResponse<Void> deleteStudent(String studentId, String operatorId, String operatorName);

    ApiResponse<Void> batchDeleteStudents(List<String> studentIds, String operatorId, String operatorName);

    ApiResponse<Void> exportStudents(StudentQueryRequest request, HttpServletResponse response);
}
