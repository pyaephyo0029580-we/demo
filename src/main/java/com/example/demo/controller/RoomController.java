//package com.example.demo.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.stereotype.Controller;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Controller
//public class RoomController {
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    // Unity native WebSocket handler
//    @Autowired
//    private UnityWebSocketHandler unityWebSocketHandler;
//
//    // Room Code -> GameRoom
//    private static final Map<String, GameRoom> rooms =
//            new ConcurrentHashMap<>();
//
//    // UnityWebSocketHandler က အသုံးပြုရန်
//    public static GameRoom getRoom(String roomCode) {
//        if (roomCode == null) {
//            return null;
//        }
//
//        return rooms.get(roomCode.trim().toUpperCase());
//    }
//
//    // =========================================================
//    // 1. CREATE ROOM
//    // =========================================================
//
//    @MessageMapping("/create-room")
//    public void createRoom(
//            StompHeaderAccessor accessor,
//            Map<String, String> payload) {
//
//        String username = payload.get("username");
//
//        if (username == null || username.trim().isEmpty()) {
//            return;
//        }
//
//        username = username.trim();
//
//        String roomCode = generateRoomCode();
//
//        GameRoom room = new GameRoom(roomCode, username);
//        rooms.put(roomCode, room);
//
//        System.out.println(
//                "Room Created: " + roomCode + " | Host: " + username
//        );
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "ROOM_CREATED");
//        response.put("roomCode", roomCode);
//        response.put("host", username);
//
//        messagingTemplate.convertAndSend(
//                "/topic/room-created/" + username,
//                response
//        );
//
//        broadcastRoomPlayers(roomCode);
//    }
//
//    // =========================================================
//    // 2. JOIN ROOM
//    // =========================================================
//
//    @MessageMapping("/join-room")
//    public void joinRoom(
//            StompHeaderAccessor accessor,
//            Map<String, String> payload) {
//
//        String username = payload.get("username");
//        String roomCode = payload.get("roomCode");
//
//        if (username == null || username.trim().isEmpty()
//                || roomCode == null || roomCode.trim().isEmpty()) {
//            return;
//        }
//
//        username = username.trim();
//        roomCode = roomCode.trim().toUpperCase();
//
//        GameRoom room = rooms.get(roomCode);
//
//        if (room == null) {
//            sendRoomError(username, "ROOM_NOT_FOUND",
//                    "Room not found");
//            return;
//        }
//
//        if (room.isGameStarted()) {
//            sendRoomError(username, "GAME_ALREADY_STARTED",
//                    "Game has already started");
//            return;
//        }
//
//        boolean added = room.addPlayer(username);
//
//        if (!added) {
//            sendRoomError(username, "NAME_ALREADY_USED",
//                    "Username already exists in this room");
//            return;
//        }
//
//        System.out.println(
//                username + " joined room " + roomCode
//        );
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "ROOM_JOINED");
//        response.put("roomCode", roomCode);
//        response.put("host", room.getHost());
//
//        messagingTemplate.convertAndSend(
//                "/topic/room-joined/" + username,
//                response
//        );
//
//        broadcastRoomPlayers(roomCode);
//    }
//
//    private void sendRoomError(
//            String username,
//            String type,
//            String message) {
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", type);
//        response.put("message", message);
//
//        messagingTemplate.convertAndSend(
//                "/topic/room-error/" + username,
//                response
//        );
//    }
//
//    // =========================================================
//    // 3. ROOM PLAYER LIST
//    // =========================================================
//
//    private void broadcastRoomPlayers(String roomCode) {
//
//        GameRoom room = rooms.get(roomCode);
//
//        if (room == null) {
//            return;
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("roomCode", roomCode);
//        response.put("host", room.getHost());
//        response.put(
//                "players",
//                new ArrayList<>(room.getPlayers().keySet())
//        );
//
//        messagingTemplate.convertAndSend(
//                "/topic/room/" + roomCode,
//                response
//        );
//    }
//
//    // =========================================================
//    // 4. START GAME
//    // =========================================================
//
//    @MessageMapping("/start-game")
//    public void startGame(
//            StompHeaderAccessor accessor,
//            Map<String, String> payload) {
//
//        String username = payload.get("username");
//        String roomCode = payload.get("roomCode");
//
//        if (username == null || username.trim().isEmpty()
//                || roomCode == null || roomCode.trim().isEmpty()) {
//            return;
//        }
//
//        username = username.trim();
//        roomCode = roomCode.trim().toUpperCase();
//
//        GameRoom room = rooms.get(roomCode);
//
//        if (room == null) {
//            System.out.println("Cannot start: room not found.");
//            return;
//        }
//
//        if (!room.getHost().equals(username)) {
//            System.out.println("Only host can start the game.");
//            return;
//        }
//
//        if (room.isGameStarted()) {
//            System.out.println("Game already started: " + roomCode);
//            return;
//        }
//
//        room.setGameStarted(true);
//
//        System.out.println("GAME STARTED: " + roomCode);
//
//        // Lobby ထဲရှိ STOMP clients များထံ ပို့ခြင်း
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "GAME_START");
//        response.put("roomCode", roomCode);
//
//        // SampleScene သည် Unity Build Settings ထဲတွင်
//        // ထည့်ထားသော Scene အမည်ဖြစ်ရမည်။
//        response.put("scene", "SampleScene");
//
//        messagingTemplate.convertAndSend(
//                "/topic/room/" + roomCode + "/game-start",
//                response
//        );
//
//        // Unity native WebSocket clients များထံ ပို့ခြင်း
//        unityWebSocketHandler.broadcastGameStart(roomCode);
//    }
//
//    // =========================================================
//    // 5. PLAYER MOVEMENT (STOMP)
//    // =========================================================
//
//    @MessageMapping("/player-move")
//    public void playerMove(
//            StompHeaderAccessor accessor,
//            Map<String, Object> payload) {
//
//        String roomCode = getStringValue(payload.get("roomCode"));
//        String username = getStringValue(payload.get("username"));
//
//        if (roomCode == null || username == null
//                || roomCode.trim().isEmpty()
//                || username.trim().isEmpty()) {
//            return;
//        }
//
//        roomCode = roomCode.trim().toUpperCase();
//        username = username.trim();
//
//        GameRoom room = rooms.get(roomCode);
//
//        if (room == null || !room.isGameStarted()) {
//            return;
//        }
//
//        if (!room.getPlayers().containsKey(username)) {
//            return;
//        }
//
//        Object x = payload.get("x");
//        Object y = payload.get("y");
//
//        if (!(x instanceof Number) || !(y instanceof Number)) {
//            return;
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "PLAYER_MOVE");
//        response.put("roomCode", roomCode);
//        response.put("username", username);
//        response.put("x", ((Number) x).doubleValue());
//        response.put("y", ((Number) y).doubleValue());
//
//        messagingTemplate.convertAndSend(
//                "/topic/room/" + roomCode + "/player-move",
//                response
//        );
//    }
//
//    // =========================================================
//    // 6. HELPERS
//    // =========================================================
//
//    private String getStringValue(Object value) {
//        return value == null ? null : String.valueOf(value);
//    }
//
//    private String generateRoomCode() {
//
//        final String characters =
//                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//
//        Random random = new Random();
//
//        while (true) {
//            StringBuilder builder = new StringBuilder();
//
//            for (int i = 0; i < 6; i++) {
//                builder.append(
//                        characters.charAt(
//                                random.nextInt(characters.length())
//                        )
//                );
//            }
//
//            String roomCode = builder.toString();
//
//            if (!rooms.containsKey(roomCode)) {
//                return roomCode;
//            }
//        }
//    }
//}

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

<<<<<<< HEAD
=======
    // Unity native WebSocket handler
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
    @Autowired
    private UnityWebSocketHandler unityWebSocketHandler;

    // Room Code -> GameRoom
    private static final Map<String, GameRoom> rooms =
            new ConcurrentHashMap<>();

<<<<<<< HEAD
=======
    // UnityWebSocketHandler က အသုံးပြုရန်
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
    public static GameRoom getRoom(String roomCode) {
        if (roomCode == null) {
            return null;
        }
<<<<<<< HEAD
        return rooms.get(roomCode.trim().toUpperCase());
    }

    // 🟢 Unity Native WebSocket ကနေ JOIN လာတာကို လက်ခံရန် Method အသစ်
    public static boolean handleUnityJoin(String roomCode, String username) {
        if (roomCode == null || username == null) return false;
        roomCode = roomCode.trim().toUpperCase();
        username = username.trim();
=======

        return rooms.get(roomCode.trim().toUpperCase());
    }

    // =========================================================
    // 1. CREATE ROOM
    // =========================================================
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65

        GameRoom room = rooms.get(roomCode);
        if (room == null) {
            // Room မရှိသေးရင် အလိုအလျောက် အသစ်ဖန်တီးပေးမည် (Error မတက်စေရန်)
            room = new GameRoom(roomCode, username);
            rooms.put(roomCode, room);
        }

        return room.addPlayer(username);
    }

    // =========================================================
    // 1. CREATE ROOM (STOMP)
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
<<<<<<< HEAD
=======

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
        String roomCode = generateRoomCode();

        GameRoom room = new GameRoom(roomCode, username);
        rooms.put(roomCode, room);

<<<<<<< HEAD
        System.out.println("Room Created: " + roomCode + " | Host: " + username);
=======
        System.out.println(
                "Room Created: " + roomCode + " | Host: " + username
        );
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65

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
<<<<<<< HEAD
    // 2. JOIN ROOM (STOMP)
    // =========================================================
=======
    // 2. JOIN ROOM
    // =========================================================

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
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
<<<<<<< HEAD
            sendRoomError(username, "ROOM_NOT_FOUND", "Room not found");
=======
            sendRoomError(username, "ROOM_NOT_FOUND",
                    "Room not found");
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
            return;
        }

        if (room.isGameStarted()) {
<<<<<<< HEAD
            sendRoomError(username, "GAME_ALREADY_STARTED", "Game has already started");
=======
            sendRoomError(username, "GAME_ALREADY_STARTED",
                    "Game has already started");
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
            return;
        }

        boolean added = room.addPlayer(username);

        if (!added) {
<<<<<<< HEAD
            sendRoomError(username, "NAME_ALREADY_USED", "Username already exists in this room");
=======
            sendRoomError(username, "NAME_ALREADY_USED",
                    "Username already exists in this room");
>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
            return;
        }

        System.out.println(username + " joined room " + roomCode);

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

<<<<<<< HEAD
    private void sendRoomError(String username, String type, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);

=======
    private void sendRoomError(
            String username,
            String type,
            String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
        messagingTemplate.convertAndSend(
                "/topic/room-error/" + username,
                response
        );
    }

    // =========================================================
    // 3. ROOM PLAYER LIST
    // =========================================================
<<<<<<< HEAD
=======

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
    private void broadcastRoomPlayers(String roomCode) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", roomCode);
        response.put("host", room.getHost());
        response.put("players", new ArrayList<>(room.getPlayers().keySet()));

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                response
        );
    }

    // =========================================================
    // 4. START GAME
    // =========================================================
<<<<<<< HEAD
=======

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
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
<<<<<<< HEAD
=======

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
        System.out.println("GAME STARTED: " + roomCode);

        // Lobby ထဲရှိ STOMP clients များထံ ပို့ခြင်း
        Map<String, Object> response = new HashMap<>();
        response.put("type", "GAME_START");
        response.put("roomCode", roomCode);
<<<<<<< HEAD
        response.put("scene", "SampleScene");

=======

        // SampleScene သည် Unity Build Settings ထဲတွင်
        // ထည့်ထားသော Scene အမည်ဖြစ်ရမည်။
        response.put("scene", "SampleScene");

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
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
<<<<<<< HEAD
    @MessageMapping("/player-move")
    public void playerMove(
            StompHeaderAccessor accessor,
            Map<String, Object> payload) {

=======

    @MessageMapping("/player-move")
    public void playerMove(
            StompHeaderAccessor accessor,
            Map<String, Object> payload) {

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
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
<<<<<<< HEAD
=======

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
    private String getStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String generateRoomCode() {
<<<<<<< HEAD
        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
=======

        final String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
        Random random = new Random();

        while (true) {
            StringBuilder builder = new StringBuilder();
<<<<<<< HEAD
            for (int i = 0; i < 6; i++) {
                builder.append(characters.charAt(random.nextInt(characters.length())));
            }

            String roomCode = builder.toString();
=======

            for (int i = 0; i < 6; i++) {
                builder.append(
                        characters.charAt(
                                random.nextInt(characters.length())
                        )
                );
            }

            String roomCode = builder.toString();

>>>>>>> 5b278f711fd372da6f4cd2a37785e993fff3df65
            if (!rooms.containsKey(roomCode)) {
                return roomCode;
            }
        }
    }
}