package com.elneb.monitoring;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class MonitoringWebSocketConfig implements WebSocketConfigurer {
    private final MonitoringWebSocketHandler handler;
    public MonitoringWebSocketConfig(MonitoringWebSocketHandler handler){this.handler=handler;}
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/ws/monitoring").setAllowedOrigins("*");}
}
