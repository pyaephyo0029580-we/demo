package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // Client တွေ message လက်ခံမယ့် channel
        config.enableSimpleBroker("/topic");

        // Client က Server ဆီ message ပို့မယ့် prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // Browser / SockJS အတွက်
        registry.addEndpoint("/game-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // JavaFX Native WebSocket အတွက်
        registry.addEndpoint("/game-websocket-native")
                .setAllowedOriginPatterns("*");
    }
}