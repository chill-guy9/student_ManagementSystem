package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sms.nexus.common.constant.AppConstant;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.exception.AuthenticationException;
import com.sms.nexus.common.exception.BusinessException;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.common.util.IpUtil;
import com.sms.nexus.dto.request.ChangePasswordRequest;
import com.sms.nexus.dto.request.LoginRequest;
import com.sms.nexus.dto.request.RegisterRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.LoginResponse;
import com.sms.nexus.entity.Admin;
import com.sms.nexus.entity.AdminSession;
import com.sms.nexus.mapper.AdminMapper;
import com.sms.nexus.mapper.AdminSessionMapper;
import com.sms.nexus.security.JwtTokenProvider;
import com.sms.nexus.service.AuthService;
import com.sms.nexus.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminMapper adminMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final StringRedisTemplate redisTemplate;
    private final IdGenerator idGenerator;

    @Override
    public ApiResponse<Void> checkLoginRateLimit(String ip) {
        String key = AppConstant.REDIS_LOGIN_ATTEMPTS_PREFIX + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= AppConstant.MAX_LOGIN_ATTEMPTS) {
            throw new AuthenticationException("登录尝试次数过多，请" + AppConstant.LOGIN_LOCK_MINUTES + "分钟后再试");
        }
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request, String ip, String userAgent) {
        // Check rate limit
        checkLoginRateLimit(ip);

        // Find admin by username
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, request.getUsername());
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            incrementLoginAttempts(ip);
            throw new AuthenticationException("用户名或密码错误");
        }

        // Check status
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new AuthenticationException("账号已被禁用");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            incrementLoginAttempts(ip);
            throw new AuthenticationException("用户名或密码错误");
        }

        // Clear login attempts on success
        String key = AppConstant.REDIS_LOGIN_ATTEMPTS_PREFIX + ip;
        redisTemplate.delete(key);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(admin.getAdminId(), admin.getUsername(), admin.getRole());

        // Save session
        AdminSession session = new AdminSession();
        session.setAdminId(admin.getAdminId());
        session.setToken(token);
        session.setIp(ip);
        session.setUserAgent(userAgent);
        session.setExpiredAt(LocalDateTime.now().plusHours(24));
        adminSessionMapper.insert(session);

        // Update last login info
        admin.setLastLoginAt(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        adminMapper.updateById(admin);

        // Log the login
        logService.createLog(admin.getAdminId(), admin.getUsername(), OperationType.LOGIN.getValue(),
                "admin", admin.getAdminId(), admin.getRealName(),
                "用户登录", ip, "INFO");

        LoginResponse loginResponse = new LoginResponse(token, admin.getAdminId(),
                admin.getUsername(), admin.getRealName(), admin.getRole(), admin.getAvatar());

        return ApiResponse.success(loginResponse);
    }

    @Override
    public ApiResponse<Void> logout(String token) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String adminId = jwtTokenProvider.getAdminIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Blacklist the token
            jwtTokenProvider.blacklistToken(token);

            // Log the logout
            logService.createLog(adminId, username, OperationType.LOGOUT.getValue(),
                    "admin", adminId, null, "用户登出", null, "INFO");
        }
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> register(RegisterRequest request) {
        // Check username uniqueness
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, request.getUsername());
        if (adminMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在: " + request.getUsername());
        }

        // Check email uniqueness
        LambdaQueryWrapper<Admin> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(Admin::getEmail, request.getEmail());
        if (adminMapper.selectCount(emailWrapper) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        // Create admin with read_only role by default
        Admin admin = new Admin();
        admin.setAdminId(idGenerator.nextAdminId());
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRealName(request.getRealName());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setRole("read_only");
        admin.setStatus(1);

        adminMapper.insert(admin);

        logService.createLog(admin.getAdminId(), admin.getUsername(), OperationType.CREATE.getValue(),
                "admin", admin.getAdminId(), admin.getRealName(),
                "用户注册: " + admin.getUsername(), null, "INFO");

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> changePassword(String adminId, ChangePasswordRequest request) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminMapper.updateById(admin);

        logService.createLog(adminId, admin.getUsername(), OperationType.UPDATE.getValue(),
                "admin", adminId, admin.getRealName(),
                "修改密码", null, "INFO");

        return ApiResponse.success();
    }

    private void incrementLoginAttempts(String ip) {
        String key = AppConstant.REDIS_LOGIN_ATTEMPTS_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, AppConstant.LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }
}
