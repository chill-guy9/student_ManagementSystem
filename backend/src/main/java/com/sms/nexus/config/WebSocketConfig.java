package com.sms.nexus.config;

import com.sms.nexus.websocket.LogStreamHandler;
import com.sms.nexus.websocket.NotificationStreamHandler;
import com.sms.nexus.websocket.ShellHandler;
import com.sms.nexus.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogStreamHandler logStreamHandler;
    private final ShellHandler shellHandler;
    private final NotificationStreamHandler notificationStreamHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(LogStreamHandler logStreamHandler,
                           ShellHandler shellHandler,
                           NotificationStreamHandler notificationStreamHandler,
                           WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.logStreamHandler = logStreamHandler;
        this.shellHandler = shellHandler;
        this.notificationStreamHandler = notificationStreamHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logStreamHandler, "/ws/logs")
                .addHandler(shellHandler, "/ws/shell")
                .addHandler(notificationStreamHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
