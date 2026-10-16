package com.example.demo.service;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
public class NgrKillService {

    private static final double KILL_RANGE = 90.0;
    private static final long KILL_COOLDOWN = 10000L;
    private static final int MAX_PLAYERS = 12;

    private final SimpMessagingTemplate messagingTemplate;
    private final SecureRandom random = new SecureRandom();

    private final Map<String, GameRoom> rooms =
            new HashMap<>();

    private final Map<String, String> sessionRooms =
            new HashMap<>();

    public NgrKillService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ====================================
    // PLAYER DATA
    // ====================================

    private static class GamePlayer {

        String sessionId;
        String playerId;

        double x;
        double y;

        boolean alive = true;
        boolean impostor = false;
        boolean positionKnown = false;

        long lastKillTime = 0;
        long lastMoveTime = 0;

        GamePlayer(String sessionId, String playerId) {
            this.sessionId = sessionId;
            this.playerId = playerId;
        }
    }

    // ====================================
    // GAME ROOM
    // ====================================

    private static class GameRoom {

        String roomId;

        Map<String, GamePlayer> players =
                new LinkedHashMap<>();

        List<Map<String, Object>> deadBodies =
                new ArrayList<>();

        String phase = "WAITING";
        String hostSessionId;

        GameRoom(String roomId) {
            this.roomId = roomId;
        }
    }

    // ====================================
    // 1. PLAYER JOIN
    // ====================================

    public synchronized void join(
            String roomId,
            String sessionId,
            String playerId) {

        if (sessionId == null ||
                roomId == null ||
                playerId == null ||
                !roomId.matches("[a-zA-Z0-9_-]{1,32}") ||
                !playerId.matches("[a-zA-Z0-9_-]{1,48}")) {

            sendError(sessionId, "Invalid player or room");
            return;
        }

        if (sessionRooms.containsKey(sessionId)) {

            String oldRoomId = sessionRooms.get(sessionId);

            GameRoom oldRoom = rooms.get(oldRoomId);

            GamePlayer oldPlayer = oldRoom == null
                    ? null
                    : oldRoom.players.get(sessionId);

            if (oldRoomId.equals(roomId) &&
                    oldPlayer != null &&
                    oldPlayer.playerId.equals(playerId)) {

                sendPrivate(sessionId, Map.of(
                        "type", "JOINED",
                        "playerId", playerId
                ));

                broadcastState(oldRoom);

            } else {
                sendError(sessionId, "Already joined a room");
            }

            return;
        }

        GameRoom room = rooms.computeIfAbsent(
                roomId,
                GameRoom::new
        );

        if (!room.phase.equals("WAITING")) {
            sendError(sessionId, "Game already started");
            return;
        }

        if (room.players.size() >= MAX_PLAYERS) {
            sendError(sessionId, "Room is full");
            return;
        }

        for (GamePlayer p : room.players.values()) {

            if (p.playerId.equals(playerId)) {
                sendError(sessionId, "Duplicate Player ID");
                return;
            }
        }

        GamePlayer player =
                new GamePlayer(sessionId, playerId);

        room.players.put(sessionId, player);

        sessionRooms.put(sessionId, roomId);if (room.hostSessionId == null) {
            room.hostSessionId = sessionId;
        }

        sendPrivate(sessionId, Map.of(
                "type", "JOINED",
                "playerId", playerId
        ));

        broadcastState(room);
    }

    // ====================================
    // 2. SYSTEM RANDOM ROLE ASSIGNMENT
    // ====================================

    public synchronized boolean start(
            String roomId,
            String sessionId) {

        GameRoom room = rooms.get(roomId);

        if (room == null ||
                sessionId == null ||
                !sessionId.equals(room.hostSessionId)) {

            sendError(sessionId, "Only host can start");
            return false;
        }

        if (room.phase.equals("PLAYING")) {
            sendError(sessionId, "Game already running");
            return false;
        }

        if (room.players.size() < 2) {
            sendError(sessionId, "Need at least 2 players");
            return false;
        }

        List<GamePlayer> players =
                new ArrayList<>(room.players.values());

        // System randomly chooses one killer
        int randomIndex = random.nextInt(players.size());

        GamePlayer selectedKiller =
                players.get(randomIndex);

        room.phase = "PLAYING";
        room.deadBodies.clear();

        for (GamePlayer p : players) {

            p.alive = true;
            p.impostor = (p == selectedKiller);
            p.lastKillTime = 0;

            String role = p.impostor
                    ? "IMPOSTOR"
                    : "CREW";

            // Private role notification
            sendPrivate(p.sessionId, Map.of(
                    "type", "ROLE",
                    "role", role,
                    "killRange", KILL_RANGE,
                    "cooldownMs", KILL_COOLDOWN
            ));
        }

        broadcastState(room);

        return true;
    }

    // ====================================
    // 3. TRACK PLAYER POSITION
    // ====================================

    public synchronized boolean acceptMovement(
            String roomId,
            String sessionId,
            Map<String, Object> data) {

        GameRoom room = rooms.get(roomId);

        GamePlayer player = room == null
                ? null
                : room.players.get(sessionId);

        if (player == null ||
                data == null ||
                !player.alive ||
                room.phase.equals("FINISHED")) {

            return false;
        }

        if (!player.playerId.equals(data.get("id"))) {
            return false;
        }

        Object rawX = data.get("x");
        Object rawY = data.get("y");

        if (!(rawX instanceof Number) ||
                !(rawY instanceof Number)) {
            return false;
        }

        double x = ((Number) rawX).doubleValue();
        double y = ((Number) rawY).doubleValue();

        if (!Double.isFinite(x) ||
                !Double.isFinite(y) ||
                x < 0 ||
                y < 0 ||
                x > 100000 ||
                y > 100000) {

            return false;
        }

        long now = System.nanoTime();

        boolean firstPosition = !player.positionKnown;

        if (!firstPosition) {

            double elapsed = Math.max(
                    0.0,
                    (now - player.lastMoveTime)
                            / 1_000_000_000.0
            );

            // Basic movement speed validation
            double maxDistance =
                    24 + Math.min(elapsed, 1.0) * 500;

            double moved = Math.hypot(
                    x - player.x,
                    y - player.y
            );

            if (moved > maxDistance) {
                return false;
            }
        }

        player.x = x;
        player.y = y;

        player.positionKnown = true;
        player.lastMoveTime = now;

        if (firstPosition) {
            broadcastState(room);
        }

        return true;
    }// ====================================
    // 4. KILL ACTION
    // ====================================

    public synchronized void kill(
            String roomId,
            String sessionId,
            String targetId) {

        GameRoom room = rooms.get(roomId);

        GamePlayer killer = room == null
                ? null
                : room.players.get(sessionId);

        if (room == null ||
                killer == null ||
                !room.phase.equals("PLAYING")) {

            sendError(sessionId, "Game not running");
            return;
        }

        // Killer role validation
        if (!killer.impostor || !killer.alive) {

            sendError(sessionId, "You cannot kill");
            return;
        }

        if (targetId == null) {
            sendError(sessionId, "Missing target");
            return;
        }

        GamePlayer target = null;

        for (GamePlayer p : room.players.values()) {

            if (p.playerId.equals(targetId)) {
                target = p;
                break;
            }
        }

        if (target == null ||
                target == killer ||
                !target.alive ||
                target.impostor) {

            sendError(sessionId, "Invalid target");
            return;
        }

        // Position validation
        if (!killer.positionKnown ||
                !target.positionKnown) {

            sendError(sessionId, "Positions not ready");
            return;
        }

        // Kill range validation
        double distance = Math.hypot(
                killer.x - target.x,
                killer.y - target.y
        );

        if (distance > KILL_RANGE) {

            sendError(sessionId, "Target too far");
            return;
        }

        // Kill cooldown validation
        long now = System.currentTimeMillis();

        if (killer.lastKillTime != 0 &&
                now - killer.lastKillTime
                        < KILL_COOLDOWN) {

            sendError(sessionId, "Kill cooldown active");
            return;
        }

        // Successful kill
        killer.lastKillTime = now;
        target.alive = false;

        // Dead body position
        room.deadBodies.add(Map.of(
                "playerId", target.playerId,
                "x", target.x,
                "y", target.y
        ));

        // Kill animation event
        Map<String, Object> killEvent = Map.of(
                "type", "KILL_EVENT",
                "killerId", killer.playerId,
                "targetId", target.playerId,
                "animationType", "RED_SLASH",
                "x", target.x,
                "y", target.y,
                "durationMs", 1400,
                "cooldownMs", KILL_COOLDOWN
        );

        // Only killer and victim receive animation
        sendPrivate(killer.sessionId, killEvent);
        sendPrivate(target.sessionId, killEvent);

        // Synchronize dead/alive state
        broadcastState(room);

        // Check winner
        checkWin(room);
    }

    // ====================================
    // 5. GAME OVER CHECK
    // ====================================

    private void checkWin(GameRoom room) {

        if (!room.phase.equals("PLAYING")) {
            return;
        }

        int aliveCrew = 0;
        int aliveKillers = 0;

        for (GamePlayer p : room.players.values()) {

            if (!p.alive) continue;

            if (p.impostor) {
                aliveKillers++;
            } else {
                aliveCrew++;
            }
        }

        if (aliveKillers == 0 ||
                aliveCrew <= aliveKillers) {

            room.phase = "FINISHED";

            String winner = aliveKillers == 0
                    ? "CREW"
                    : "IMPOSTOR";

            messagingTemplate.convertAndSend(
                    "/topic/ngr/" + room.roomId + "/game-over",
                    Map.of(
                            "type", "GAME_OVER",
                            "winner", winner
                    )
            );broadcastState(room);
        }
    }

    // ====================================
    // 6. PLAYER DISCONNECT
    // ====================================

    public synchronized void disconnect(String sessionId) {

        if (sessionId == null) return;

        String roomId = sessionRooms.remove(sessionId);

        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);

        if (room == null) return;

        room.players.remove(sessionId);

        if (room.players.isEmpty()) {
            rooms.remove(roomId);
            return;
        }

        if (sessionId.equals(room.hostSessionId)) {

            room.hostSessionId =
                    room.players.keySet()
                            .iterator()
                            .next();
        }

        broadcastState(room);

        checkWin(room);
    }

    // ====================================
    // 7. BROADCAST GAME STATE
    // ====================================

    private void broadcastState(GameRoom room) {

        List<Map<String, Object>> playerList =
                new ArrayList<>();

        for (GamePlayer p : room.players.values()) {

            playerList.add(Map.of(
                    "id", p.playerId,
                    "x", p.x,
                    "y", p.y,
                    "alive", p.alive,
                    "positionKnown", p.positionKnown
            ));
        }

        GamePlayer host =
                room.players.get(room.hostSessionId);

        messagingTemplate.convertAndSend(
                "/topic/ngr/" + room.roomId + "/state",
                Map.of(
                        "type", "STATE",
                        "phase", room.phase,
                        "hostId", host == null
                                ? ""
                                : host.playerId,
                        "players", playerList,
                        "bodies",
                        new ArrayList<>(room.deadBodies)
                )
        );
    }

    // ====================================
    // 8. SEND ERROR
    // ====================================

    private void sendError(
            String sessionId,
            String message) {

        if (sessionId == null) return;

        sendPrivate(sessionId, Map.of(
                "type", "ERROR",
                "message", message
        ));
    }

    // ====================================
    // 9. PRIVATE WEBSOCKET MESSAGE
    // ====================================

    private void sendPrivate(
            String sessionId,
            Object data) {

        if (sessionId == null) return;

        SimpMessageHeaderAccessor headers =
                SimpMessageHeaderAccessor.create(
                        SimpMessageType.MESSAGE
                );

        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/ngr",
                data,
                headers.getMessageHeaders()
        );
    }
}