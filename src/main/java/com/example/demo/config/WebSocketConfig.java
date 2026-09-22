package com.example.demo.config;

//ipackage com.example.demo;

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
        config.enableSimpleBroker("/topic"); // Client တွေ စာလက်ခံရယူမယ့် channel prefix
        config.setApplicationDestinationPrefixes("/app"); // Client က ဆာဗာဆီ ပို့မယ့် prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/game-websocket").withSockJS(); // Client တွေ ချိတ်ဆက်မယ့် WebSocket Endpoint
    }
}