package com.sms.nexus.controller;

import com.sms.nexus.dto.request.CreateAdminRequest;
import com.sms.nexus.dto.request.UpdateAdminRequest;
import com.sms.nexus.dto.response.AdminVO;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员CRUD接口")
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "切换管理员状态")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<AdminVO> toggleAdminStatus(@PathVariable("id") String adminId,
                                                    Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return adminService.toggleAdminStatus(adminId, operatorId, operatorName);
    }

    @GetMapping("/permissions")
    @Operation(summary = "获取角色权限")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<List<Map<String, Object>>> getPermissions(@RequestParam String role) {
        return adminService.getPermissions(role);
    }

    @PutMapping("/permissions")
    @Operation(summary = "更新角色权限")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<Void> updatePermissions(@RequestBody Map<String, Object> request) {
        String role = (String) request.get("role");
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) request.get("permissions");
        return adminService.updatePermissions(role, permissions);
    }

    @GetMapping
    @Operation(summary = "查询管理员列表")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<PageResult<AdminVO>> listAdmins(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminService.listAdmins(keyword, role, status, page, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取管理员详情")
    @PreAuthorize("hasAnyRole('super_admin', 'user_admin', 'log_auditor', 'read_only')")
    public ApiResponse<AdminVO> getAdmin(@PathVariable("id") String adminId) {
        return adminService.getAdmin(adminId);
    }

    @PostMapping
    @Operation(summary = "创建管理员")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<AdminVO> createAdmin(@Valid @RequestBody CreateAdminRequest request,
                                             Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return adminService.createAdmin(request, operatorId, operatorName);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新管理员")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<AdminVO> updateAdmin(@PathVariable("id") String adminId,
                                             @Valid @RequestBody UpdateAdminRequest request,
                                             Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return adminService.updateAdmin(adminId, request, operatorId, operatorName);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除管理员")
    @PreAuthorize("hasRole('super_admin')")
    public ApiResponse<Void> deleteAdmin(@PathVariable("id") String adminId,
                                          Authentication authentication) {
        String operatorId = authentication.getName();
        String operatorName = getOperatorName(authentication);
        return adminService.deleteAdmin(adminId, operatorId, operatorName);
    }

    private String getOperatorName(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            return details.getUsername();
        }
        return authentication.getName();
    }
}
