package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sms.nexus.common.enums.AdminRole;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.exception.BusinessException;
import com.sms.nexus.common.exception.ResourceNotFoundException;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.dto.request.CreateAdminRequest;
import com.sms.nexus.dto.request.UpdateAdminRequest;
import com.sms.nexus.dto.response.AdminVO;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.entity.Admin;
import com.sms.nexus.mapper.AdminMapper;
import com.sms.nexus.service.AdminService;
import com.sms.nexus.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    @Override
    public ApiResponse<PageResult<AdminVO>> listAdmins(String keyword, String role, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Admin::getUsername, keyword)
                    .or().like(Admin::getRealName, keyword)
                    .or().like(Admin::getEmail, keyword)
            );
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(Admin::getRole, role);
        }
        if (status != null) {
            wrapper.eq(Admin::getStatus, status);
        }

        wrapper.orderByDesc(Admin::getCreatedAt);

        Page<Admin> pageResult = adminMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<AdminVO> voList = new ArrayList<>();
        for (Admin a : pageResult.getRecords()) {
            voList.add(toAdminVO(a));
        }

        PageResult<AdminVO> result = new PageResult<>(voList, pageResult.getTotal(),
                pageResult.getCurrent(), pageResult.getSize());

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<AdminVO> getAdmin(String adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new ResourceNotFoundException("管理员不存在: " + adminId);
        }
        return ApiResponse.success(toAdminVO(admin));
    }

    @Override
    public ApiResponse<AdminVO> createAdmin(CreateAdminRequest request, String operatorId, String operatorName) {
        // Check username uniqueness
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, request.getUsername());
        if (adminMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在: " + request.getUsername());
        }

        // Validate role
        try {
            AdminRole.fromValue(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色: " + request.getRole());
        }

        Admin admin = new Admin();
        admin.setAdminId(idGenerator.nextAdminId());
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRealName(request.getRealName());
        admin.setRole(request.getRole());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setStatus(1);

        adminMapper.insert(admin);

        logService.createLog(operatorId, operatorName, OperationType.CREATE.getValue(),
                "admin", admin.getAdminId(), admin.getRealName(),
                "创建管理员: " + admin.getUsername(), null, "INFO");

        return ApiResponse.success(toAdminVO(admin));
    }

    @Override
    public ApiResponse<AdminVO> updateAdmin(String adminId, UpdateAdminRequest request,
                                             String operatorId, String operatorName) {
        Admin existing = adminMapper.selectById(adminId);
        if (existing == null) {
            throw new ResourceNotFoundException("管理员不存在: " + adminId);
        }

        List<ChangeVO> changes = new ArrayList<>();

        if (request.getRealName() != null && !request.getRealName().equals(existing.getRealName())) {
            addChange(changes, "realName", "真实姓名", existing.getRealName(), request.getRealName());
            existing.setRealName(request.getRealName());
        }
        if (request.getRole() != null && !request.getRole().equals(existing.getRole())) {
            // Validate the new role value
            try {
                AdminRole.fromValue(request.getRole());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的角色: " + request.getRole());
            }
            addChange(changes, "role", "角色", existing.getRole(), request.getRole());
            // Prevent last super_admin from demoting themselves
            if (AdminRole.SUPER_ADMIN.getValue().equals(existing.getRole())
                    && !AdminRole.SUPER_ADMIN.getValue().equals(request.getRole())) {
                LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Admin::getRole, AdminRole.SUPER_ADMIN.getValue());
                if (adminMapper.selectCount(wrapper) <= 1) {
                    throw new BusinessException("不能降级最后一个超级管理员的角色");
                }
            }
            existing.setRole(request.getRole());
        }
        if (request.getEmail() != null && !request.getEmail().equals(existing.getEmail())) {
            addChange(changes, "email", "邮箱", existing.getEmail(), request.getEmail());
            existing.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(existing.getPhone())) {
            addChange(changes, "phone", "手机号", existing.getPhone(), request.getPhone());
            existing.setPhone(request.getPhone());
        }
        if (request.getStatus() != null && !request.getStatus().equals(existing.getStatus())) {
            addChange(changes, "status", "状态", String.valueOf(existing.getStatus()), String.valueOf(request.getStatus()));
            existing.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            addChange(changes, "password", "密码", "***", "***");
        }

        adminMapper.updateById(existing);

        logService.createLogWithChanges(operatorId, operatorName, OperationType.UPDATE.getValue(),
                "admin", adminId, existing.getRealName(),
                "更新管理员: " + existing.getUsername(), null, "INFO", changes);

        return ApiResponse.success(toAdminVO(existing));
    }

    @Override
    public ApiResponse<Void> deleteAdmin(String adminId, String operatorId, String operatorName) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new ResourceNotFoundException("管理员不存在: " + adminId);
        }

        // Prevent deleting the last super_admin
        if (AdminRole.SUPER_ADMIN.getValue().equals(admin.getRole())) {
            LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Admin::getRole, AdminRole.SUPER_ADMIN.getValue());
            if (adminMapper.selectCount(wrapper) <= 1) {
                throw new BusinessException("不能删除最后一个超级管理员");
            }
        }

        adminMapper.deleteById(adminId);

        logService.createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                "admin", adminId, admin.getRealName(),
                "删除管理员: " + admin.getUsername(), null, "WARN");

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<AdminVO> toggleAdminStatus(String adminId, String operatorId, String operatorName) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new ResourceNotFoundException("管理员不存在: " + adminId);
        }
        int currentStatus = admin.getStatus() != null ? admin.getStatus() : 1;
        int newStatus = currentStatus == 1 ? 0 : 1;
        // Prevent self-deactivation
        if (adminId.equals(operatorId) && newStatus == 0) {
            throw new BusinessException("不能禁用自己的账号");
        }
        // Prevent last super_admin deactivation
        if (AdminRole.SUPER_ADMIN.getValue().equals(admin.getRole()) && newStatus == 0) {
            LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Admin::getRole, AdminRole.SUPER_ADMIN.getValue()).eq(Admin::getStatus, 1);
            if (adminMapper.selectCount(wrapper) <= 1) {
                throw new BusinessException("不能禁用最后一个超级管理员");
            }
        }
        admin.setStatus(newStatus);
        adminMapper.updateById(admin);

        logService.createLog(operatorId, operatorName, OperationType.UPDATE.getValue(),
                "admin", adminId, admin.getRealName(),
                (newStatus == 1 ? "启用" : "禁用") + "管理员: " + admin.getUsername(), null, "INFO");

        return ApiResponse.success(toAdminVO(admin));
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getPermissions(String role) {
        // Return permission matrix for the given role
        List<Map<String, Object>> permissions = new ArrayList<>();
        String[] modules = {"students", "teachers", "admins", "logs", "settings", "shell", "backup"};
        Map<String, String> moduleLabels = Map.of(
                "students", "学生管理", "teachers", "教师管理", "admins", "管理员管理",
                "logs", "系统日志", "settings", "系统设置", "shell", "终端", "backup", "数据备份"
        );

        Map<String, Map<String, boolean[]>> rolePermissions = new HashMap<>();
        rolePermissions.put("super_admin", Map.of(
                "students", new boolean[]{true, true, true, true},
                "teachers", new boolean[]{true, true, true, true},
                "admins", new boolean[]{true, true, true, true},
                "logs", new boolean[]{true, true, true, true},
                "settings", new boolean[]{true, true, true, true},
                "shell", new boolean[]{true, true, true, true},
                "backup", new boolean[]{true, true, true, true}
        ));
        rolePermissions.put("user_admin", Map.of(
                "students", new boolean[]{true, true, true, true},
                "teachers", new boolean[]{true, true, true, true},
                "admins", new boolean[]{true, false, false, false},
                "logs", new boolean[]{true, true, true, true},
                "settings", new boolean[]{true, false, true, false},
                "shell", new boolean[]{true, false, false, false},
                "backup", new boolean[]{true, false, false, false}
        ));
        rolePermissions.put("log_auditor", Map.of(
                "students", new boolean[]{true, false, false, false},
                "teachers", new boolean[]{true, false, false, false},
                "admins", new boolean[]{true, false, false, false},
                "logs", new boolean[]{true, true, true, true},
                "settings", new boolean[]{true, false, false, false},
                "shell", new boolean[]{false, false, false, false},
                "backup", new boolean[]{true, false, false, false}
        ));
        rolePermissions.put("read_only", Map.of(
                "students", new boolean[]{true, false, false, false},
                "teachers", new boolean[]{true, false, false, false},
                "admins", new boolean[]{true, false, false, false},
                "logs", new boolean[]{true, false, false, false},
                "settings", new boolean[]{true, false, false, false},
                "shell", new boolean[]{false, false, false, false},
                "backup", new boolean[]{false, false, false, false}
        ));

        Map<String, boolean[]> rolePerms = rolePermissions.getOrDefault(role, new HashMap<>());

        for (String module : modules) {
            Map<String, Object> perm = new HashMap<>();
            perm.put("module", module);
            perm.put("label", moduleLabels.getOrDefault(module, module));
            boolean[] actions = rolePerms.getOrDefault(module, new boolean[]{false, false, false, false});
            Map<String, Boolean> actionMap = new HashMap<>();
            actionMap.put("view", actions[0]);
            actionMap.put("create", actions[1]);
            actionMap.put("edit", actions[2]);
            actionMap.put("delete", actions[3]);
            perm.put("actions", actionMap);
            permissions.add(perm);
        }

        return ApiResponse.success(permissions);
    }

    @Override
    public ApiResponse<Void> updatePermissions(String role, List<Map<String, Object>> permissions) {
        // Permissions are role-based and defined in code, so this is a no-op for now
        // In a real system, these would be persisted to a permissions table
        logService.createLog("system", "系统", OperationType.UPDATE.getValue(),
                "permission", role, role,
                "更新角色权限: " + role, null, "INFO");
        return ApiResponse.success();
    }

    private AdminVO toAdminVO(Admin admin) {
        AdminVO vo = new AdminVO();
        vo.setAdminId(admin.getAdminId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setRole(admin.getRole());
        vo.setRoleLabel(admin.getRole() != null ? getRoleLabel(admin.getRole()) : "未知");
        vo.setEmail(admin.getEmail());
        vo.setPhone(admin.getPhone());
        vo.setAvatar(admin.getAvatar());
        vo.setStatus(admin.getStatus());
        vo.setLastLoginAt(admin.getLastLoginAt());
        vo.setLastLoginIp(admin.getLastLoginIp());
        vo.setCreatedAt(admin.getCreatedAt());
        return vo;
    }

    private String getRoleLabel(String role) {
        try {
            return AdminRole.fromValue(role).getLabel();
        } catch (IllegalArgumentException e) {
            return role;
        }
    }

    private void addChange(List<ChangeVO> changes, String fieldName, String fieldLabel,
                           String oldValue, String newValue) {
        ChangeVO change = new ChangeVO();
        change.setFieldName(fieldName);
        change.setFieldLabel(fieldLabel);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        changes.add(change);
    }
}
