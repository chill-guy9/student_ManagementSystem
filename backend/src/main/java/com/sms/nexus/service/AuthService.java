package com.sms.nexus.service;

import com.sms.nexus.dto.request.ChangePasswordRequest;
import com.sms.nexus.dto.request.LoginRequest;
import com.sms.nexus.dto.request.RegisterRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.LoginResponse;

public interface AuthService {

    ApiResponse<LoginResponse> login(LoginRequest request, String ip, String userAgent);

    ApiResponse<Void> logout(String token);

    ApiResponse<Void> checkLoginRateLimit(String ip);

    ApiResponse<Void> register(RegisterRequest request);

    ApiResponse<Void> changePassword(String adminId, ChangePasswordRequest request);
}
