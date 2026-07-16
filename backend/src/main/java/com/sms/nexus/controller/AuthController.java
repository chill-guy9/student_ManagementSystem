package com.sms.nexus.controller;

import com.sms.nexus.common.util.IpUtil;
import com.sms.nexus.dto.request.ChangePasswordRequest;
import com.sms.nexus.dto.request.LoginRequest;
import com.sms.nexus.dto.request.RegisterRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.LoginResponse;
import com.sms.nexus.entity.Admin;
import com.sms.nexus.mapper.AdminMapper;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录登出接口")
public class AuthController {

    private final AuthService authService;
    private final AdminMapper adminMapper;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return authService.login(request, ip, userAgent);
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/user")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<LoginResponse> getCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails jwtUser) {
            Admin admin = adminMapper.selectById(jwtUser.getAdminId());
            if (admin != null) {
                LoginResponse userResponse = new LoginResponse(
                        null, admin.getAdminId(), admin.getUsername(),
                        admin.getRealName(), admin.getRole(), admin.getAvatar());
                return ApiResponse.success(userResponse);
            }
        }
        return ApiResponse.error(401, "未登录");
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        return authService.logout(token);
    }

    @PutMapping("/change-password")
    @Operation(summary = "修改密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             Authentication authentication) {
        String adminId = authentication.getName();
        return authService.changePassword(adminId, request);
    }
}
