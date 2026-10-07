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

    // Room Code -> GameRoom
    private static final Map<String, GameRoom> rooms =
            new ConcurrentHashMap<>();


    // =========================
    // 1. CREATE ROOM
    // =========================

    @MessageMapping("/create-room")
    public void createRoom(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");

        if (username == null || username.trim().isEmpty()) {
            return;
        }

        // Room Code 6 လုံး generate
        String roomCode = generateRoomCode();

        // Room အသစ်ဆောက်
        GameRoom room = new GameRoom(roomCode, username);

        rooms.put(roomCode, room);

        System.out.println(
                "Room Created: " + roomCode +
                        " | Host: " + username
        );

        // Create လုပ်တဲ့သူဆီပဲ ပြန်ပို့
        Map<String, Object> response = new HashMap<>();

        response.put("type", "ROOM_CREATED");
        response.put("roomCode", roomCode);
        response.put("host", username);

        messagingTemplate.convertAndSend(
                "/topic/room-created/" + username,
                response
        );

        // Lobby player list ပို့
        broadcastRoomPlayers(roomCode);
    }


    // =========================
    // 2. JOIN ROOM
    // =========================

    @MessageMapping("/join-room")
    public void joinRoom(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");
        String roomCode = payload.get("roomCode");

        if (username == null ||
                username.trim().isEmpty() ||
                roomCode == null ||
                roomCode.trim().isEmpty()) {

            return;
        }

        roomCode = roomCode.toUpperCase();

        GameRoom room = rooms.get(roomCode);

        // Room မရှိ
        if (room == null) {

            Map<String, Object> response = new HashMap<>();

            response.put("type", "ROOM_NOT_FOUND");
            response.put("message", "Room not found");

            messagingTemplate.convertAndSend(
                    "/topic/room-error/" + username,
                    response
            );

            return;
        }

        // Game စပြီးသားဆို Join မလုပ်ခိုင်း
        if (room.isGameStarted()) {

            Map<String, Object> response = new HashMap<>();

            response.put("type", "GAME_ALREADY_STARTED");
            response.put(
                    "message",
                    "Game has already started"
            );

            messagingTemplate.convertAndSend(
                    "/topic/room-error/" + username,
                    response
            );

            return;
        }

        // Player ထည့်
        boolean added = room.addPlayer(username);

        if (!added) {

            Map<String, Object> response = new HashMap<>();

            response.put("type", "NAME_ALREADY_USED");
            response.put(
                    "message",
                    "Username already exists in this room"
            );

            messagingTemplate.convertAndSend(
                    "/topic/room-error/" + username,
                    response
            );

            return;
        }

        System.out.println(
                username + " joined room " + roomCode
        );

        // Join လုပ်သူဆီ confirmation
        Map<String, Object> response = new HashMap<>();

        response.put("type", "ROOM_JOINED");
        response.put("roomCode", roomCode);
        response.put("host", room.getHost());

        messagingTemplate.convertAndSend(
                "/topic/room-joined/" + username,
                response
        );

        // Room ထဲက လူအားလုံးကို player list update
        broadcastRoomPlayers(roomCode);
    }


    // =========================
    // 3. ROOM PLAYER LIST
    // =========================

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


    // =========================
    // 4. START GAME
    // =========================

    @MessageMapping("/start-game")
    public void startGame(
            StompHeaderAccessor accessor,
            Map<String, String> payload) {

        String username = payload.get("username");
        String roomCode = payload.get("roomCode");

        if (username == null ||
                roomCode == null) {

            return;
        }

        roomCode = roomCode.toUpperCase();

        GameRoom room = rooms.get(roomCode);

        if (room == null) {
            return;
        }

        // Host ပဲ Start လုပ်ခွင့်ရှိ
        if (!room.getHost().equals(username)) {

            System.out.println(
                    "Only host can start the game."
            );

            return;
        }

        room.setGameStarted(true);

        System.out.println(
                "GAME STARTED: " + roomCode
        );

        Map<String, Object> response = new HashMap<>();

        response.put("type", "GAME_START");
        response.put("roomCode", roomCode);
        response.put("scene", "Room1");

        // Room ထဲက player အားလုံးဆီ ပို့
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game-start",
                response
        );
    }


    // =========================
    // 5. GENERATE ROOM CODE
    // =========================

    private String generateRoomCode() {

        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        Random random = new Random();

        String roomCode;

        do {

            StringBuilder builder =
                    new StringBuilder();

            for (int i = 0; i < 6; i++) {

                builder.append(
                        characters.charAt(
                                random.nextInt(
                                        characters.length()
                                )
                        )
                );
            }

            roomCode = builder.toString();

        } while (rooms.containsKey(roomCode));

        return roomCode;
    }
}