package com.sms.nexus.controller;

import com.sms.nexus.dto.request.CreateStudentRequest;
import com.sms.nexus.dto.request.StudentQueryRequest;
import com.sms.nexus.dto.request.UpdateStudentRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.StudentVO;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "学生管理", description = "学生CRUD接口")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "查询学生列表")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<PageResult<StudentVO>> listStudents(StudentQueryRequest request) {
        return studentService.listStudents(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取学生详情")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<StudentVO> getStudent(@PathVariable("id") String studentId) {
        return studentService.getStudent(studentId);
    }

    @PostMapping
    @Operation(summary = "创建学生")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<StudentVO> createStudent(@Valid @RequestBody CreateStudentRequest request,
                                                 Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return studentService.createStudent(request, operatorId, operatorName);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新学生")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<StudentVO> updateStudent(@PathVariable("id") String studentId,
                                                 @Valid @RequestBody UpdateStudentRequest request,
                                                 Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return studentService.updateStudent(studentId, request, operatorId, operatorName);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除学生")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<Void> deleteStudent(@PathVariable("id") String studentId,
                                            Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return studentService.deleteStudent(studentId, operatorId, operatorName);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除学生")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<Void> batchDeleteStudents(@RequestBody Map<String, List<String>> body,
                                                   Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        List<String> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error(400, "请选择要删除的学生");
        }
        return studentService.batchDeleteStudents(ids, operatorId, operatorName);
    }

    @GetMapping("/export")
    @Operation(summary = "导出学生CSV")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<Void> exportStudents(StudentQueryRequest request,
                                             HttpServletResponse response) {
        return studentService.exportStudents(request, response);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
