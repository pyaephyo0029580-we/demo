package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class RoomController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Unity native WebSocket handler
    @Autowired
    private UnityWebSocketHandler unityWebSocketHandler;

    // Room Code -> GameRoom
    private static final Map<String, GameRoom> rooms =
            new ConcurrentHashMap<>();

    // UnityWebSocketHandler က အသုံးပြုရန်
    public static GameRoom getRoom(String roomCode) {
        if (roomCode == null) {
            return null;
        }

        return rooms.get(roomCode.trim().toUpperCase());
    }

    // =========================================================
    // 1. CREATE ROOM
    // =========================================================

    @MessageMapping("/create-room")
    public void createRoom(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");

        if (username == null || username.trim().isEmpty()) {
            return;
        }

        username = username.trim();

        String roomCode = generateRoomCode();

        GameRoom room = new GameRoom(roomCode, username);
        rooms.put(roomCode, room);

        System.out.println(
                "Room Created: " + roomCode + " | Host: " + username
        );

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_CREATED");
        response.put("roomCode", roomCode);
        response.put("host", username);

        messagingTemplate.convertAndSend(
                "/topic/room-created/" + username,
                response
        );

        broadcastRoomPlayers(roomCode);
    }

    // =========================================================
    // 2. JOIN ROOM
    // =========================================================

    @MessageMapping("/join-room")
    public void joinRoom(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");
        String roomCode = payload.get("roomCode");

        if (username == null || username.trim().isEmpty()
                || roomCode == null || roomCode.trim().isEmpty()) {
            return;
        }

        username = username.trim();
        roomCode = roomCode.trim().toUpperCase();

        GameRoom room = rooms.get(roomCode);

        if (room == null) {
            sendRoomError(username, "ROOM_NOT_FOUND",
                    "Room not found");
            return;
        }

        if (room.isGameStarted()) {
            sendRoomError(username, "GAME_ALREADY_STARTED",
                    "Game has already started");
            return;
        }

        boolean added = room.addPlayer(username);

        if (!added) {
            sendRoomError(username, "NAME_ALREADY_USED",
                    "Username already exists in this room");
            return;
        }

        System.out.println(
                username + " joined room " + roomCode
        );

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_JOINED");
        response.put("roomCode", roomCode);
        response.put("host", room.getHost());

        messagingTemplate.convertAndSend(
                "/topic/room-joined/" + username,
                response
        );

        broadcastRoomPlayers(roomCode);
    }

    private void sendRoomError(
            String username,
            String type,
            String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);

        messagingTemplate.convertAndSend(
                "/topic/room-error/" + username,
                response
        );
    }

    // =========================================================
    // 3. ROOM PLAYER LIST
    // =========================================================

    private void broadcastRoomPlayers(String roomCode) {

        GameRoom room = rooms.get(roomCode);

        if (room == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", roomCode);
        response.put("host", room.getHost());
        response.put(
                "players",
                new ArrayList<>(room.getPlayers().keySet())
        );

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                response
        );
    }

    // =========================================================
    // 4. START GAME
    // =========================================================

    @MessageMapping("/start-game")
    public void startGame(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");
        String roomCode = payload.get("roomCode");

        if (username == null || username.trim().isEmpty()
                || roomCode == null || roomCode.trim().isEmpty()) {
            return;
        }

        username = username.trim();
        roomCode = roomCode.trim().toUpperCase();

        GameRoom room = rooms.get(roomCode);

        if (room == null) {
            System.out.println("Cannot start: room not found.");
            return;
        }

        if (!room.getHost().equals(username)) {
            System.out.println("Only host can start the game.");
            return;
        }

        if (room.isGameStarted()) {
            System.out.println("Game already started: " + roomCode);
            return;
        }

        room.setGameStarted(true);

        System.out.println("GAME STARTED: " + roomCode);

        // Lobby ထဲရှိ STOMP clients များထံ ပို့ခြင်း
        Map<String, Object> response = new HashMap<>();
        response.put("type", "GAME_START");
        response.put("roomCode", roomCode);

        // SampleScene သည် Unity Build Settings ထဲတွင်
        // ထည့်ထားသော Scene အမည်ဖြစ်ရမည်။
        response.put("scene", "SampleScene");

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game-start",
                response
        );

        // Unity native WebSocket clients များထံ ပို့ခြင်း
        unityWebSocketHandler.broadcastGameStart(roomCode);
    }

    // =========================================================
    // 5. PLAYER MOVEMENT (STOMP)
    // =========================================================

    @MessageMapping("/player-move")
    public void playerMove(
            StompHeaderAccessor accessor,
            Map<String, Object> payload) {

        String roomCode = getStringValue(payload.get("roomCode"));
        String username = getStringValue(payload.get("username"));

        if (roomCode == null || username == null
                || roomCode.trim().isEmpty()
                || username.trim().isEmpty()) {
            return;
        }

        roomCode = roomCode.trim().toUpperCase();
        username = username.trim();

        GameRoom room = rooms.get(roomCode);

        if (room == null || !room.isGameStarted()) {
            return;
        }

        if (!room.getPlayers().containsKey(username)) {
            return;
        }

        Object x = payload.get("x");
        Object y = payload.get("y");

        if (!(x instanceof Number) || !(y instanceof Number)) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "PLAYER_MOVE");
        response.put("roomCode", roomCode);
        response.put("username", username);
        response.put("x", ((Number) x).doubleValue());
        response.put("y", ((Number) y).doubleValue());

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/player-move",
                response
        );
    }

    // =========================================================
    // 6. HELPERS
    // =========================================================

    private String getStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String generateRoomCode() {

        final String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        Random random = new Random();

        while (true) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < 6; i++) {
                builder.append(
                        characters.charAt(
                                random.nextInt(characters.length())
                        )
                );
            }

            String roomCode = builder.toString();

            if (!rooms.containsKey(roomCode)) {
                return roomCode;
            }
        }
    }
}