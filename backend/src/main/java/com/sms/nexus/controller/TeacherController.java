package com.sms.nexus.controller;

import com.sms.nexus.dto.request.CreateTeacherRequest;
import com.sms.nexus.dto.request.UpdateTeacherRequest;
import com.sms.nexus.dto.request.AddCourseRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.TeacherVO;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "教师管理", description = "教师CRUD接口")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "查询教师列表")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<PageResult<TeacherVO>> listTeachers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return teacherService.listTeachers(keyword, department, title, status, page, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取教师详情")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<TeacherVO> getTeacher(@PathVariable("id") String teacherId) {
        return teacherService.getTeacher(teacherId);
    }

    @PostMapping
    @Operation(summary = "创建教师")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<TeacherVO> createTeacher(@Valid @RequestBody CreateTeacherRequest request,
                                                 Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return teacherService.createTeacher(request, operatorId, operatorName);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新教师")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<TeacherVO> updateTeacher(@PathVariable("id") String teacherId,
                                                 @Valid @RequestBody UpdateTeacherRequest request,
                                                 Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return teacherService.updateTeacher(teacherId, request, operatorId, operatorName);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除教师")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<Void> deleteTeacher(@PathVariable("id") String teacherId,
                                            Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return teacherService.deleteTeacher(teacherId, operatorId, operatorName);
    }

    @GetMapping("/{id}/courses")
    @Operation(summary = "获取教师课程列表")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<TeacherVO.CourseInfoList> getTeacherCourses(@PathVariable("id") String teacherId) {
        return teacherService.getTeacherCourses(teacherId);
    }

    @PostMapping("/{id}/courses")
    @Operation(summary = "添加教师课程")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<TeacherVO.CourseInfo> addTeacherCourse(
            @PathVariable("id") String teacherId,
            @Valid @RequestBody AddCourseRequest request,
            Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return teacherService.addTeacherCourse(teacherId, request, operatorId, operatorName);
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    @Operation(summary = "删除教师课程")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin')")
    public ApiResponse<Void> removeTeacherCourse(
            @PathVariable("id") String teacherId,
            @PathVariable("courseId") Long courseId,
            Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return teacherService.removeTeacherCourse(teacherId, courseId, operatorId, operatorName);
    }

    @GetMapping("/export")
    @Operation(summary = "导出教师CSV")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<Void> exportTeachers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) {
        return teacherService.exportTeachers(keyword, department, title, status, response);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
