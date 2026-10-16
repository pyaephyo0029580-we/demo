package com.example.demo.controller;

import com.example.demo.service.NgrKillService;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Controller
public class NgrKillController {

    private final NgrKillService killService;

    // ========================================
    // 1. CONSTRUCTOR
    // ========================================

    public NgrKillController(
            NgrKillService killService) {

        this.killService = killService;
    }

    // ========================================
    // 2. PLAYER JOIN
    // ========================================

    @MessageMapping("/ngr/join/{roomId}")
    public void joinPlayer(
            @DestinationVariable String roomId,
            @Payload Map<String, String> data,
            SimpMessageHeaderAccessor accessor) {

        String sessionId = accessor.getSessionId();

        String playerId = data == null
                ? null
                : data.get("playerId");

        killService.join(
                roomId,
                sessionId,
                playerId
        );
    }

    // ========================================
    // 3. RANDOM KILLER ROLE ASSIGNMENT
    // ========================================

    @MessageMapping("/ngr/start/{roomId}")
    public void startGame(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor accessor) {

        String sessionId = accessor.getSessionId();

        // Server randomly selects one killer
        killService.start(
                roomId,
                sessionId
        );
    }

    // ========================================
    // 4. KILLER ACTION
    // ========================================

    @MessageMapping("/ngr/kill/{roomId}")
    public void killPlayer(
            @DestinationVariable String roomId,
            @Payload Map<String, String> data,
            SimpMessageHeaderAccessor accessor) {

        String sessionId = accessor.getSessionId();

        String targetId = data == null
                ? null
                : data.get("targetId");

        // Validate killer role, range,
        // cooldown and target state

        killService.kill(
                roomId,
                sessionId,
                targetId
        );
    }

    // ========================================
    // 5. PLAYER DISCONNECT
    // ========================================

    @EventListener
    public void handleDisconnect(
            SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        if (sessionId != null) {

            killService.disconnect(sessionId);
        }
    }
}