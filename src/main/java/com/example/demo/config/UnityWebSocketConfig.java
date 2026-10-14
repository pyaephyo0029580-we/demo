package com.example.demo.config;

import com.example.demo.controller.UnityWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class UnityWebSocketConfig implements WebSocketConfigurer {

    private final UnityWebSocketHandler unityWebSocketHandler;

    public UnityWebSocketConfig(UnityWebSocketHandler unityWebSocketHandler) {
        this.unityWebSocketHandler = unityWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(unityWebSocketHandler, "/unity-websocket")
                .setAllowedOriginPatterns("*");
    }
}