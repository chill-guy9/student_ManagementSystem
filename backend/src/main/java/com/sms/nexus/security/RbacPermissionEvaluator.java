package com.sms.nexus.security;

import com.sms.nexus.common.enums.AdminRole;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Component
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private static final Map<String, Set<String>> MODULE_PERMISSIONS = Map.of(
            "student", Set.of("super_admin", "user_admin", "read_only"),
            "teacher", Set.of("super_admin", "user_admin", "read_only"),
            "admin",   Set.of("super_admin"),
            "log",     Set.of("super_admin", "log_auditor"),
            "system",  Set.of("super_admin"),
            "backup",  Set.of("super_admin")
    );

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String role = extractRole(authentication);
        String module = targetDomainObject.toString();
        String action = permission.toString();

        Set<String> allowedRoles = MODULE_PERMISSIONS.get(module);
        if (allowedRoles == null) {
            return false;
        }

        if (!allowedRoles.contains(role)) {
            return false;
        }

        // read_only can only read
        if (AdminRole.READ_ONLY.name().equalsIgnoreCase(role)) {
            return "read".equalsIgnoreCase(action) || "export".equalsIgnoreCase(action);
        }

        return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, targetType, permission);
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring(5))
                .findFirst()
                .orElse("");
    }
}
