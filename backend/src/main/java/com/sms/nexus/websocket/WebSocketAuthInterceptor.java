package com.sms.nexus.websocket;

import com.sms.nexus.common.enums.AdminRole;
import com.sms.nexus.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        URI uri = request.getURI();
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid or missing token");
            return false;
        }

        String adminId = jwtTokenProvider.getAdminIdFromToken(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);

        attributes.put("adminId", adminId);
        attributes.put("username", username);
        attributes.put("role", role);
        attributes.put("token", token);

        // Restrict /ws/shell to super_admin only
        String path = uri.getPath();
        if (path != null && path.endsWith("/shell")) {
            if (!AdminRole.SUPER_ADMIN.getValue().equals(role)) {
                log.warn("WebSocket handshake rejected for /ws/shell: role={} requires super_admin", role);
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }
}
