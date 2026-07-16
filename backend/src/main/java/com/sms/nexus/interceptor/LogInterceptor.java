package com.sms.nexus.interceptor;

import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.util.IpUtil;
import com.sms.nexus.entity.Log;
import com.sms.nexus.security.JwtAuthenticationFilter;
import com.sms.nexus.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogInterceptor implements HandlerInterceptor {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        String uri = request.getRequestURI();
        // Skip non-API paths
        if (!uri.startsWith("/api/")) {
            return;
        }
        // Skip auth login (handled separately in AuthService)
        if (uri.equals("/api/auth/login") || uri.equals("/api/auth/logout")) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        String operatorId = authentication.getName();
        String operatorName = "";
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails details) {
            operatorName = details.getUsername();
        }

        String method = request.getMethod();

        // Only log GET requests here; write operations (POST/PUT/DELETE) are already
        // logged by the service layer with more specific details.
        if (!"GET".equalsIgnoreCase(method)) {
            return;
        }

        String operationType = OperationType.SYSTEM.getValue();
        String targetType = extractTargetType(uri);
        String targetId = extractTargetId(uri);
        String ip = IpUtil.getClientIp(request);
        String detail = method + " " + uri;

        if (ex != null) {
            detail += " [ERROR: " + ex.getMessage() + "]";
        }

        try {
            logService.createLog(operatorId, operatorName, operationType,
                    targetType, targetId, null, detail, ip, "INFO");
        } catch (Exception e) {
            log.error("Failed to create log entry", e);
        }
    }

    private String mapMethodToOperationType(String method) {
        return switch (method.toUpperCase()) {
            case "POST" -> OperationType.CREATE.getValue();
            case "PUT", "PATCH" -> OperationType.UPDATE.getValue();
            case "DELETE" -> OperationType.DELETE.getValue();
            case "GET" -> OperationType.SYSTEM.getValue();
            default -> OperationType.SYSTEM.getValue();
        };
    }

    private String extractTargetType(String uri) {
        if (uri.contains("/students")) return "student";
        if (uri.contains("/teachers")) return "teacher";
        if (uri.contains("/admins")) return "admin";
        if (uri.contains("/logs")) return "log";
        if (uri.contains("/dashboard")) return "dashboard";
        if (uri.contains("/system")) return "system";
        if (uri.contains("/backup")) return "backup";
        if (uri.contains("/settings")) return "settings";
        return "unknown";
    }

    private String extractTargetId(String uri) {
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("students".equals(parts[i]) || "teachers".equals(parts[i])
                    || "admins".equals(parts[i]) || "logs".equals(parts[i])) {
                if (i + 1 < parts.length) {
                    String next = parts[i + 1];
                    if (!next.isEmpty() && !next.contains("?")
                            && !next.equals("export") && !next.equals("cleanup")
                            && !next.equals("related")) {
                        return next;
                    }
                }
            }
        }
        return null;
    }
}
