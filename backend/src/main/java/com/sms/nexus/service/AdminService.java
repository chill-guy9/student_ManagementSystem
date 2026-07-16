package com.sms.nexus.service;

import com.sms.nexus.dto.request.CreateAdminRequest;
import com.sms.nexus.dto.request.UpdateAdminRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.AdminVO;

import java.util.List;
import java.util.Map;

public interface AdminService {

    ApiResponse<PageResult<AdminVO>> listAdmins(String keyword, String role, Integer status, Integer page, Integer pageSize);

    ApiResponse<AdminVO> getAdmin(String adminId);

    ApiResponse<AdminVO> createAdmin(CreateAdminRequest request, String operatorId, String operatorName);

    ApiResponse<AdminVO> updateAdmin(String adminId, UpdateAdminRequest request, String operatorId, String operatorName);

    ApiResponse<Void> deleteAdmin(String adminId, String operatorId, String operatorName);

    ApiResponse<AdminVO> toggleAdminStatus(String adminId, String operatorId, String operatorName);

    ApiResponse<List<Map<String, Object>>> getPermissions(String role);

    ApiResponse<Void> updatePermissions(String role, List<Map<String, Object>> permissions);
}
