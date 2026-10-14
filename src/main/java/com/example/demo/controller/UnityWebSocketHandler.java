package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UnityWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Session ID -> WebSocket Session
    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    // Session ID -> Room Code
    private final Map<String, String> sessionRooms =
            new ConcurrentHashMap<>();

    // Session ID -> Username
    private final Map<String, String> sessionUsers =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);

        System.out.println(
                "Unity WebSocket connected: " + session.getId()
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws IOException {

        JsonNode data;

        try {
            data = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            sendError(session, "INVALID_JSON", "Invalid JSON message");
            return;
        }

        String type = data.path("type").asText("");

        switch (type) {
            case "JOIN":
                handleJoin(session, data);
                break;

            case "PLAYER_MOVE":
                handlePlayerMove(session, data);
                break;

            default:
                sendError(
                        session,
                        "UNKNOWN_MESSAGE",
                        "Unknown message type: " + type
                );
        }
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    private void handleJoin(
            WebSocketSession session,
            JsonNode data) throws IOException {

        String roomCode = data.path("roomCode").asText("")
                .trim().toUpperCase();

        String username = data.path("username").asText("").trim();

        if (roomCode.isEmpty() || username.isEmpty()) {
            sendError(
                    session,
                    "INVALID_JOIN",
                    "roomCode and username are required"
            );
            return;
        }

        GameRoom room = RoomController.getRoom(roomCode);

        if (room == null) {
            sendError(session, "ROOM_NOT_FOUND", "Room does not exist");
            return;
        }

        if (!room.getPlayers().containsKey(username)) {
            sendError(
                    session,
                    "PLAYER_NOT_IN_ROOM",
                    "Player is not registered in this room"
            );
            return;
        }

        String oldRoom = sessionRooms.put(session.getId(), roomCode);
        sessionUsers.put(session.getId(), username);

        if (oldRoom != null && !oldRoom.equals(roomCode)) {
            broadcastToRoom(
                    oldRoom,
                    Map.of(
                            "type", "PLAYER_LEFT",
                            "username", username
                    ),
                    session.getId()
            );
        }

        sendJson(
                session,
                Map.of(
                        "type", "JOINED",
                        "roomCode", roomCode,
                        "username", username,
                        "gameStarted", room.isGameStarted()
                )
        );

        System.out.println(
                "Unity JOIN: room=" + roomCode
                        + ", username=" + username
        );

        // Host က Start Game ကို အရင်နှိပ်ထားပြီးသားဆိုရင်
        // JOIN လုပ်လာတဲ့ client ကိုလည်း Game စတင်ဖို့ အသိပေးမယ်။
        if (room.isGameStarted()) {
            sendJson(
                    session,
                    Map.of(
                            "type", "GAME_START",
                            "roomCode", roomCode,
                            "scene", "SampleScene"
                    )
            );
        }
    }

    // =====================================================
    // PLAYER MOVEMENT
    // =====================================================

    private void handlePlayerMove(
            WebSocketSession session,
            JsonNode data) throws IOException {

        String roomCode = sessionRooms.get(session.getId());
        String username = sessionUsers.get(session.getId());

        if (roomCode == null || username == null) {
            sendError(
                    session,
                    "NOT_JOINED",
                    "Send JOIN before PLAYER_MOVE"
            );
            return;
        }

        if (!data.has("x") || !data.has("y")
                || !data.path("x").isNumber()
                || !data.path("y").isNumber()) {

            sendError(
                    session,
                    "INVALID_POSITION",
                    "Numeric x and y are required"
            );
            return;
        }

        float x = (float) data.path("x").asDouble();
        float y = (float) data.path("y").asDouble();

        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            sendError(
                    session,
                    "INVALID_POSITION",
                    "Position must be finite"
            );
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", "PLAYER_MOVE",
                "roomCode", roomCode,
                "username", username,
                "x", x,
                "y", y
        );

        broadcastToRoom(roomCode, payload, session.getId());
    }

    // =====================================================
    // START GAME - CALLED BY RoomController
    // =====================================================

    public void broadcastGameStart(String roomCode) {
        Map<String, Object> payload = Map.of(
                "type", "GAME_START",
                "roomCode", roomCode,
                "scene", "SampleScene"
        );

        broadcastToRoom(roomCode, payload, null);
    }

    // =====================================================
    // BROADCAST
    // =====================================================

    private void broadcastToRoom(
            String roomCode,
            Object payload,
            String excludedSessionId) {

        try {
            String json = objectMapper.writeValueAsString(payload);

            for (Map.Entry<String, String> entry
                    : sessionRooms.entrySet()) {

                if (!roomCode.equals(entry.getValue())) {
                    continue;
                }

                String sessionId = entry.getKey();

                if (excludedSessionId != null
                        && excludedSessionId.equals(sessionId)) {
                    continue;
                }

                WebSocketSession target = sessions.get(sessionId);

                if (target == null || !target.isOpen()) {
                    continue;
                }

                synchronized (target) {
                    if (target.isOpen()) {
                        target.sendMessage(new TextMessage(json));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Unity broadcast failed: " + e.getMessage()
            );
        }
    }

    private void sendError(
            WebSocketSession session,
            String code,
            String message) {

        try {
            sendJson(
                    session,
                    Map.of(
                            "type", "ERROR",
                            "code", code,
                            "message", message
                    )
            );
        } catch (IOException e) {
            System.err.println(
                    "Failed to send Unity error: " + e.getMessage()
            );
        }
    }

    private void sendJson(
            WebSocketSession session,
            Object payload) throws IOException {

        if (session == null || !session.isOpen()) {
            return;
        }

        String json = objectMapper.writeValueAsString(payload);

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    // =====================================================
    // DISCONNECT
    // =====================================================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        String roomCode = sessionRooms.remove(session.getId());
        String username = sessionUsers.remove(session.getId());

        sessions.remove(session.getId());

        if (roomCode != null && username != null) {
            broadcastToRoom(
                    roomCode,
                    Map.of(
                            "type", "PLAYER_LEFT",
                            "username", username
                    ),
                    null
            );
        }

        System.out.println(
                "Unity WebSocket disconnected: " + session.getId()
        );
    }
}