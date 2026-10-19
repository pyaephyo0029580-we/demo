

//package com.example.demo.controller;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class UnityWebSocketHandler extends TextWebSocketHandler {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    // Session ID -> WebSocket Session
//    private final Map<String, WebSocketSession> sessions =
//            new ConcurrentHashMap<>();
//
//    // Session ID -> Room Code
//    private final Map<String, String> sessionRooms =
//            new ConcurrentHashMap<>();
//
//    // Session ID -> Username
//    private final Map<String, String> sessionUsers =
//            new ConcurrentHashMap<>();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) {
//        sessions.put(session.getId(), session);
//
//        System.out.println(
//                "Unity WebSocket connected: " + session.getId()
//        );
//    }
//
//    @Override
//    protected void handleTextMessage(
//            WebSocketSession session,
//            TextMessage message) throws IOException {
//
//        JsonNode data;
//
//        try {
//            data = objectMapper.readTree(message.getPayload());
//        } catch (Exception e) {
//            sendError(session, "INVALID_JSON", "Invalid JSON message");
//            return;
//        }
//
//        String type = data.path("type").asText("");
//
//        switch (type) {
//            case "JOIN":
//                handleJoin(session, data);
//                break;
//
//            case "PLAYER_MOVE":
//                handlePlayerMove(session, data);
//                break;
//
//            case "KILL":
//                handleKill(session, data);
//                break;
//
//            case "START_MEETING":
//            case "EMERGENCY_MEETING":
//                handleMeeting(session, data);
//                break;
//
//            // 🟢 Lobby Customization (Color & Hat) Sync အတွက် ထည့်သွင်းထားသည်
//            case "UPDATE_CUSTOMIZATION":
//                handleCustomization(session, data);
//                break;
//
//            default:
//                sendError(
//                        session,
//                        "UNKNOWN_MESSAGE",
//                        "Unknown message type: " + type
//                );
//        }
//    }
//
//    // =====================================================
//    // JOIN ROOM
//    // =====================================================
//
//    private void handleJoin(
//            WebSocketSession session,
//            JsonNode data) throws IOException {
//
//        String roomCode = data.path("roomCode").asText("")
//                .trim().toUpperCase();
//
//        String username = data.path("username").asText("").trim();
//
//        if (roomCode.isEmpty() || username.isEmpty()) {
//            sendError(
//                    session,
//                    "INVALID_JOIN",
//                    "roomCode and username are required"
//            );
//            return;
//        }
//
//        GameRoom room = RoomController.getRoom(roomCode);
//
//        if (room == null) {
//            sendError(session, "ROOM_NOT_FOUND", "Room does not exist");
//            return;
//        }
//
//        if (!room.getPlayers().containsKey(username)) {
//            sendError(
//                    session,
//                    "PLAYER_NOT_IN_ROOM",
//                    "Player is not registered in this room"
//            );
//            return;
//        }
//
//        String oldRoom = sessionRooms.put(session.getId(), roomCode);
//        sessionUsers.put(session.getId(), username);
//
//        if (oldRoom != null && !oldRoom.equals(roomCode)) {
//            broadcastToRoom(
//                    oldRoom,
//                    Map.of(
//                            "type", "PLAYER_LEFT",
//                            "username", username
//                    ),
//                    session.getId()
//            );
//        }
//
//        sendJson(
//                session,
//                Map.of(
//                        "type", "JOINED",
//                        "roomCode", roomCode,
//                        "username", username,
//                        "gameStarted", room.isGameStarted()
//                )
//        );
//
//        System.out.println(
//                "Unity JOIN: room=" + roomCode
//                        + ", username=" + username
//        );
//
//        if (room.isGameStarted()) {
//            java.util.List<Map<String, Object>> assignedTasks = room.getRandomTasksForLocations();
//            sendJson(
//                    session,
//                    Map.of(
//                            "type", "GAME_START",
//                            "roomCode", roomCode,
//                            "scene", "SampleScene",
//                            "assignedTasks", assignedTasks
//                    )
//            );
//        }
//    }
//
//    // =====================================================
//    // PLAYER MOVEMENT
//    // =====================================================
//
//    private void handlePlayerMove(
//            WebSocketSession session,
//            JsonNode data) throws IOException {
//
//        String roomCode = sessionRooms.get(session.getId());
//        String username = sessionUsers.get(session.getId());
//
//        if (roomCode == null || username == null) {
//            sendError(
//                    session,
//                    "NOT_JOINED",
//                    "Send JOIN before PLAYER_MOVE"
//            );
//            return;
//        }
//
//        if (!data.has("x") || !data.has("y")
//                || !data.path("x").isNumber()
//                || !data.path("y").isNumber()) {
//
//            sendError(
//                    session,
//                    "INVALID_POSITION",
//                    "Numeric x and y are required"
//            );
//            return;
//        }
//
//        float x = (float) data.path("x").asDouble();
//        float y = (float) data.path("y").asDouble();
//
//        if (!Float.isFinite(x) || !Float.isFinite(y)) {
//            sendError(
//                    session,
//                    "INVALID_POSITION",
//                    "Position must be finite"
//            );
//            return;
//        }
//
//        Map<String, Object> payload = new java.util.HashMap<>();
//
//        payload.put("type", "PLAYER_MOVE");
//        payload.put("roomCode", roomCode);
//        payload.put("username", username);
//        payload.put("x", x);
//        payload.put("y", y);
//
//        // Animation data
//        payload.put("inputX", data.path("inputX").asDouble(0));
//        payload.put("inputY", data.path("inputY").asDouble(0));
//        payload.put("lastInputX", data.path("lastInputX").asDouble(0));
//        payload.put("lastInputY", data.path("lastInputY").asDouble(-1));
//        payload.put("isWalking", data.path("isWalking").asBoolean(false));
//
//        broadcastToRoom(roomCode, payload, session.getId());
//    }
//
//    // =====================================================
//    // CUSTOMIZATION UPDATE (Color & Hat Sync)
//    // =====================================================
//
//    private void handleCustomization(WebSocketSession session, JsonNode data) throws IOException {
//        String roomCode = sessionRooms.get(session.getId());
//        String username = sessionUsers.get(session.getId());
//
//        if (roomCode == null || username == null) {
//            return;
//        }
//
//        String color = data.path("color").asText("#FFFFFF");
//        String hat = data.path("hat").asText("");
//
//        Map<String, Object> payload = Map.of(
//                "type", "UPDATE_CUSTOMIZATION",
//                "username", username,
//                "color", color,
//                "hat", hat
//        );
//
//        broadcastToRoom(roomCode, payload, session.getId());
//    }
//
//    // =====================================================
//    // KILL MECHANIC (Among Us Style) - FIXED & MEETING INTEGRATED
//    // =====================================================
//
//    private void handleKill(
//            WebSocketSession session,
//            JsonNode data) throws IOException {
//
//        String roomCode = sessionRooms.get(session.getId());
//        String username = sessionUsers.get(session.getId());
//
//        if (roomCode == null || username == null) {
//            sendError(
//                    session,
//                    "NOT_JOINED",
//                    "Send JOIN before KILL"
//            );
//            return;
//        }
//
//        String targetUsername = data.path("targetUsername").asText("");
//
//        System.out.println("PLAYER KILLED: Killer=" + username + ", Target=" + targetUsername + " in room " + roomCode);
//
//        // 1. Unity ဘက်သို့ Kill ဖြစ်ကြောင်း (Animation နှင့် Audio အတွက်) ပို့မည်
//        Map<String, Object> killPayload = Map.of(
//                "type", "KILL",
//                "killerUsername", username,
//                "targetUsername", targetUsername
//        );
//        broadcastToRoom(roomCode, killPayload, null);
//
//        // 2. လူတစ်ယောက်သေသည်နှင့် Meeting ကို အလိုအလျောက် စတင်မည်
//        GameRoom room = RoomController.getRoom(roomCode);
//        if (room == null) return;
//
//        room.setMeetingActive(true);
//
//        java.util.List<Map<String, Object>> playerList = new java.util.ArrayList<>();
//        int index = 0;
//        for (String pUsername : room.getPlayers().keySet()) {
//            Map<String, Object> pData = new HashMap<>();
//            pData.put("username", pUsername);
//            pData.put("spawnIndex", index++);
//            pData.put("eligible", !room.isEliminated(pUsername) && !pUsername.equals(targetUsername));
//            pData.put("eliminated", room.isEliminated(pUsername) || pUsername.equals(targetUsername));
//            playerList.add(pData);
//        }
//
//        String calledText = targetUsername + " was killed";
//
//        Map<String, Object> meetingPayload = Map.of(
//                "type", "MEETING_STARTED",
//                "calledBy", calledText,
//                "durationSeconds", 10,
//                "players", playerList
//        );
//
//        broadcastToRoom(roomCode, meetingPayload, null);
//
//        new Thread(() -> {
//            try {
//                Thread.sleep(10000);
//
//                Map<String, Object> votingPayload = Map.of(
//                        "type", "VOTING_STARTED",
//                        "durationSeconds", 15,
//                        "players", playerList
//                );
//                broadcastToRoom(roomCode, votingPayload, null);
//
//                Thread.sleep(15000);
//
//                Map<String, Object> endPayload = Map.of(
//                        "type", "MEETING_END"
//                );
//                broadcastToRoom(roomCode, endPayload, null);
//
//                room.setMeetingActive(false);
//                room.startEmergencyCooldown(10000);
//
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }).start();
//    }
//
//    // =====================================================
//    // MEETING / EMERGENCY BUTTON MECHANIC
//    // =====================================================
//
//    private void handleMeeting(
//            WebSocketSession session,
//            JsonNode data) throws IOException {
//
//        String roomCode = sessionRooms.get(session.getId());
//        String username = sessionUsers.get(session.getId());
//
//        if (roomCode == null || username == null) {
//            sendError(session, "NOT_JOINED", "Send JOIN before MEETING");
//            return;
//        }
//
//        GameRoom room = RoomController.getRoom(roomCode);
//        if (room == null) return;
//
//        if (!room.canCallEmergencyMeeting()) {
//            long remaining = room.getEmergencyCooldownRemainingMillis() / 1000;
//            sendError(session, "COOLDOWN", "Emergency meeting is on cooldown. Please wait " + remaining + " seconds.");
//            return;
//        }
//
//        room.setMeetingActive(true);
//
//        java.util.List<Map<String, Object>> playerList = new java.util.ArrayList<>();
//        int index = 0;
//        for (String pUsername : room.getPlayers().keySet()) {
//            Map<String, Object> pData = new HashMap<>();
//            pData.put("username", pUsername);
//            pData.put("spawnIndex", index++);
//            pData.put("eligible", !room.isEliminated(pUsername));
//            pData.put("eliminated", room.isEliminated(pUsername));
//            playerList.add(pData);
//        }
//
//        System.out.println("MEETING STARTED by: " + username + " in room " + roomCode);
//
//        Map<String, Object> payload = Map.of(
//                "type", "MEETING_STARTED",
//                "calledBy", username,
//                "durationSeconds", 10,
//                "players", playerList
//        );
//
//        broadcastToRoom(roomCode, payload, null);
//
//        new Thread(() -> {
//            try {
//                Thread.sleep(10000);
//
//                Map<String, Object> votingPayload = Map.of(
//                        "type", "VOTING_STARTED",
//                        "durationSeconds", 15,
//                        "players", playerList
//                );
//                broadcastToRoom(roomCode, votingPayload, null);
//
//                Thread.sleep(15000);
//
//                Map<String, Object> endPayload = Map.of(
//                        "type", "MEETING_END"
//                );
//                broadcastToRoom(roomCode, endPayload, null);
//
//                room.setMeetingActive(false);
//                room.startEmergencyCooldown(10000);
//
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }).start();
//    }
//
//    // =====================================================
//    // START GAME - CALLED BY RoomController
//    // =====================================================
//
//    public void broadcastGameStart(String roomCode) {
//        GameRoom room = RoomController.getRoom(roomCode);
//        java.util.List<Map<String, Object>> assignedTasks =
//                (room != null) ? room.getRandomTasksForLocations() : new java.util.ArrayList<>();
//
//        Map<String, Object> payload = Map.of(
//                "type", "GAME_START",
//                "roomCode", roomCode,
//                "scene", "SampleScene",
//                "assignedTasks", assignedTasks
//        );
//
//        broadcastToRoom(roomCode, payload, null);
//    }
//
//    // =====================================================
//    // BROADCAST
//    // =====================================================
//
//    private void broadcastToRoom(
//            String roomCode,
//            Object payload,
//            String excludedSessionId) {
//
//        try {
//            String json = objectMapper.writeValueAsString(payload);
//
//            for (Map.Entry<String, String> entry
//                    : sessionRooms.entrySet()) {
//
//                if (!roomCode.equals(entry.getValue())) {
//                    continue;
//                }
//
//                String sessionId = entry.getKey();
//
//                if (excludedSessionId != null
//                        && excludedSessionId.equals(sessionId)) {
//                    continue;
//                }
//
//                WebSocketSession target = sessions.get(sessionId);
//
//                if (target == null || !target.isOpen()) {
//                    continue;
//                }
//
//                synchronized (target) {
//                    if (target.isOpen()) {
//                        target.sendMessage(new TextMessage(json));
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println(
//                    "Unity broadcast failed: " + e.getMessage()
//            );
//        }
//    }
//
//    private void sendError(
//            WebSocketSession session,
//            String code,
//            String message) {
//
//        try {
//            sendJson(
//                    session,
//                    Map.of(
//                            "type", "ERROR",
//                            "code", code,
//                            "message", message
//                    )
//            );
//        } catch (IOException e) {
//            System.err.println(
//                    "Failed to send Unity error: " + e.getMessage()
//            );
//        }
//    }
//
//    private void sendJson(
//            WebSocketSession session,
//            Object payload) throws IOException {
//
//        if (session == null || !session.isOpen()) {
//            return;
//        }
//
//        String json = objectMapper.writeValueAsString(payload);
//
//        synchronized (session) {
//            if (session.isOpen()) {
//                session.sendMessage(new TextMessage(json));
//            }
//        }
//    }
//
//    // =====================================================
//    // DISCONNECT
//    // =====================================================
//
//    @Override
//    public void afterConnectionClosed(
//            WebSocketSession session,
//            CloseStatus status) {
//
//        String roomCode = sessionRooms.remove(session.getId());
//        String username = sessionUsers.remove(session.getId());
//
//        sessions.remove(session.getId());
//
//        if (roomCode != null && username != null) {
//            broadcastToRoom(
//                    roomCode,
//                    Map.of(
//                            "type", "PLAYER_LEFT",
//                            "username", username
//                    ),
//                    null
//            );
//        }
//
//        System.out.println(
//                "Unity WebSocket disconnected: " + session.getId()
//        );
//    }
//}

package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
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

    // 🟢 Session ID -> Player Color (အရောင်များကို မှတ်ထားရန်)
    private final Map<String, String> sessionColors =
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

            case "KILL":
                handleKill(session, data);
                break;

            case "START_MEETING":
            case "EMERGENCY_MEETING":
                handleMeeting(session, data);
                break;

            // 🟢 Lobby Customization (Color & Hat) Sync အတွက် ထည့်သွင်းထားသည်
            case "UPDATE_CUSTOMIZATION":
                handleCustomization(session, data);
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
        String color = data.path("color").asText("#C51111").trim(); // 🟢 အရောင်ကို ဖမ်းယူခြင်း

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
        sessionColors.put(session.getId(), color); // 🟢 Color ကို Session တွင် သိမ်းမည်

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
                        "color", color, // 🟢 Joined ပေးသည့်အခါ အရောင်ပါ ပို့ပေးမည်
                        "gameStarted", room.isGameStarted()
                )
        );

        System.out.println(
                "Unity JOIN: room=" + roomCode
                        + ", username=" + username
                        + ", color=" + color
        );

        if (room.isGameStarted()) {
            java.util.List<Map<String, Object>> assignedTasks = room.getRandomTasksForLocations();
            sendJson(
                    session,
                    Map.of(
                            "type", "GAME_START",
                            "roomCode", roomCode,
                            "scene", "SampleScene",
                            "assignedTasks", assignedTasks
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
        String color = sessionColors.getOrDefault(session.getId(), "#C51111"); // 🟢 Player ၏ အရောင်ကို ထည့်ပေးမည်

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

        Map<String, Object> payload = new java.util.HashMap<>();

        payload.put("type", "PLAYER_MOVE");
        payload.put("roomCode", roomCode);
        payload.put("username", username);
        payload.put("color", color); // 🟢 Broadcast တွင် color ပါ တစ်ခါတည်း ပါသွားစေရန်
        payload.put("x", x);
        payload.put("y", y);

        // Animation data
        payload.put("inputX", data.path("inputX").asDouble(0));
        payload.put("inputY", data.path("inputY").asDouble(0));
        payload.put("lastInputX", data.path("lastInputX").asDouble(0));
        payload.put("lastInputY", data.path("lastInputY").asDouble(-1));
        payload.put("isWalking", data.path("isWalking").asBoolean(false));

        broadcastToRoom(roomCode, payload, session.getId());
    }

    // =====================================================
    // CUSTOMIZATION UPDATE (Color & Hat Sync)
    // =====================================================

    private void handleCustomization(WebSocketSession session, JsonNode data) throws IOException {
        String roomCode = sessionRooms.get(session.getId());
        String username = sessionUsers.get(session.getId());

        if (roomCode == null || username == null) {
            return;
        }

        String color = data.path("color").asText("#FFFFFF");
        String hat = data.path("hat").asText("");

        sessionColors.put(session.getId(), color); // 🟢 Update ဖြစ်သွားသော အရောင်ကို မှတ်ထားမည်

        Map<String, Object> payload = Map.of(
                "type", "UPDATE_CUSTOMIZATION",
                "username", username,
                "color", color,
                "hat", hat
        );

        broadcastToRoom(roomCode, payload, session.getId());
    }

    // =====================================================
    // KILL MECHANIC (Among Us Style) - FIXED & MEETING INTEGRATED
    // =====================================================

    private void handleKill(
            WebSocketSession session,
            JsonNode data) throws IOException {

        String roomCode = sessionRooms.get(session.getId());
        String username = sessionUsers.get(session.getId());

        if (roomCode == null || username == null) {
            sendError(
                    session,
                    "NOT_JOINED",
                    "Send JOIN before KILL"
            );
            return;
        }

        String targetUsername = data.path("targetUsername").asText("");

        System.out.println("PLAYER KILLED: Killer=" + username + ", Target=" + targetUsername + " in room " + roomCode);

        Map<String, Object> killPayload = Map.of(
                "type", "KILL",
                "killerUsername", username,
                "targetUsername", targetUsername
        );
        broadcastToRoom(roomCode, killPayload, null);

        GameRoom room = RoomController.getRoom(roomCode);
        if (room == null) return;

        room.setMeetingActive(true);

        java.util.List<Map<String, Object>> playerList = new java.util.ArrayList<>();
        int index = 0;
        for (String pUsername : room.getPlayers().keySet()) {
            Map<String, Object> pData = new HashMap<>();
            pData.put("username", pUsername);
            pData.put("spawnIndex", index++);
            pData.put("eligible", !room.isEliminated(pUsername) && !pUsername.equals(targetUsername));
            pData.put("eliminated", room.isEliminated(pUsername) || pUsername.equals(targetUsername));
            playerList.add(pData);
        }

        String calledText = targetUsername + " was killed";

        Map<String, Object> meetingPayload = Map.of(
                "type", "MEETING_STARTED",
                "calledBy", calledText,
                "durationSeconds", 10,
                "players", playerList
        );

        broadcastToRoom(roomCode, meetingPayload, null);

        new Thread(() -> {
            try {
                Thread.sleep(10000);

                Map<String, Object> votingPayload = Map.of(
                        "type", "VOTING_STARTED",
                        "durationSeconds", 15,
                        "players", playerList
                );
                broadcastToRoom(roomCode, votingPayload, null);

                Thread.sleep(15000);

                Map<String, Object> endPayload = Map.of(
                        "type", "MEETING_END"
                );
                broadcastToRoom(roomCode, endPayload, null);

                room.setMeetingActive(false);
                room.startEmergencyCooldown(10000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // =====================================================
    // MEETING / EMERGENCY BUTTON MECHANIC
    // =====================================================

    private void handleMeeting(
            WebSocketSession session,
            JsonNode data) throws IOException {

        String roomCode = sessionRooms.get(session.getId());
        String username = sessionUsers.get(session.getId());

        if (roomCode == null || username == null) {
            sendError(session, "NOT_JOINED", "Send JOIN before MEETING");
            return;
        }

        GameRoom room = RoomController.getRoom(roomCode);
        if (room == null) return;

        if (!room.canCallEmergencyMeeting()) {
            long remaining = room.getEmergencyCooldownRemainingMillis() / 1000;
            sendError(session, "COOLDOWN", "Emergency meeting is on cooldown. Please wait " + remaining + " seconds.");
            return;
        }

        room.setMeetingActive(true);

        java.util.List<Map<String, Object>> playerList = new java.util.ArrayList<>();
        int index = 0;
        for (String pUsername : room.getPlayers().keySet()) {
            Map<String, Object> pData = new HashMap<>();
            pData.put("username", pUsername);
            pData.put("spawnIndex", index++);
            pData.put("eligible", !room.isEliminated(pUsername));
            pData.put("eliminated", room.isEliminated(pUsername));
            playerList.add(pData);
        }

        System.out.println("MEETING STARTED by: " + username + " in room " + roomCode);

        Map<String, Object> payload = Map.of(
                "type", "MEETING_STARTED",
                "calledBy", username,
                "durationSeconds", 10,
                "players", playerList
        );

        broadcastToRoom(roomCode, payload, null);

        new Thread(() -> {
            try {
                Thread.sleep(10000);

                Map<String, Object> votingPayload = Map.of(
                        "type", "VOTING_STARTED",
                        "durationSeconds", 15,
                        "players", playerList
                );
                broadcastToRoom(roomCode, votingPayload, null);

                Thread.sleep(15000);

                Map<String, Object> endPayload = Map.of(
                        "type", "MEETING_END"
                );
                broadcastToRoom(roomCode, endPayload, null);

                room.setMeetingActive(false);
                room.startEmergencyCooldown(10000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // =====================================================
    // START GAME - CALLED BY RoomController
    // =====================================================

    public void broadcastGameStart(String roomCode) {
        GameRoom room = RoomController.getRoom(roomCode);
        java.util.List<Map<String, Object>> assignedTasks =
                (room != null) ? room.getRandomTasksForLocations() : new java.util.ArrayList<>();

        Map<String, Object> payload = Map.of(
                "type", "GAME_START",
                "roomCode", roomCode,
                "scene", "SampleScene",
                "assignedTasks", assignedTasks
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
        sessionColors.remove(session.getId()); // 🟢 Session ပိတ်သည့်အခါ Color ကိုပါ ဖယ်ရှားမည်

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