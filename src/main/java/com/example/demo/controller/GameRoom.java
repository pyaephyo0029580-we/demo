////package com.example.demo.controller;
////
////import java.util.Map;
////import java.util.concurrent.ConcurrentHashMap;
////
////public class GameRoom {
////
////    private final String roomCode;
////    private final String host;
////
////    private final Map<String, String> players = new ConcurrentHashMap<>();
////
////    private boolean gameStarted = false;
////
////    public GameRoom(String roomCode, String host) {
////        this.roomCode = roomCode;
////        this.host = host;
////
////        // Host ကို room ထဲ အလိုအလျောက်ထည့်
////        players.put(host, host);
////    }
////
////    public String getRoomCode() {
////        return roomCode;
////    }
////
////    public String getHost() {
////        return host;
////    }
////
////    public Map<String, String> getPlayers() {
////        return players;
////    }
////
////    public boolean isGameStarted() {
////        return gameStarted;
////    }
////
////    public void setGameStarted(boolean gameStarted) {
////        this.gameStarted = gameStarted;
////    }
////
////    public boolean addPlayer(String username) {
////        if (players.containsKey(username)) {
////            return false;
////        }
////
////        players.put(username, username);
////        return true;
////    }
////
////    public void removePlayer(String username) {
////        players.remove(username);
////    }
////}
//
//package com.example.demo.controller;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class GameRoom {
//
//    private final String roomCode;
//    private final String host;
//
//    private final Map<String, String> players =
//            new ConcurrentHashMap<>();
//
//    // Eliminate ဖြစ်ပြီးသား Player များ
//    private final Set<String> eliminatedPlayers =
//            ConcurrentHashMap.newKeySet();
//
//    // Meeting / Voting state
//    private volatile boolean meetingActive = false;
//
//    // Voting ပြီးဆုံးပြီးနောက် Cooldown
//    private volatile long emergencyCooldownUntil = 0L;
//
//    public GameRoom(String roomCode, String host) {
//        this.roomCode = roomCode;
//        this.host = host;
//
//        // Host ကို Room ထဲ အလိုအလျောက်ထည့်
//        players.put(host, host);
//    }
//
//    public String getRoomCode() {
//        return roomCode;
//    }
//
//    public String getHost() {
//        return host;
//    }
//
//    public Map<String, String> getPlayers() {
//        return players;
//    }
//
//    public boolean isGameStarted() {
//        return gameStarted;
//    }
//
//    public void setGameStarted(boolean gameStarted) {
//        this.gameStarted = gameStarted;
//    }
//
//    private volatile boolean gameStarted = false;
//
//    public boolean addPlayer(String username) {
//        if (username == null || username.isBlank()) {
//            return false;
//        }
//
//        return players.putIfAbsent(username, username) == null;
//    }
//
//    public void removePlayer(String username) {
//        players.remove(username);
//        eliminatedPlayers.remove(username);
//    }
//
//    public Set<String> getEliminatedPlayers() {
//        return eliminatedPlayers;
//    }
//
//    public boolean isEliminated(String username) {
//        return eliminatedPlayers.contains(username);
//    }
//
//    public boolean eliminatePlayer(String username) {
//        if (username == null || !players.containsKey(username)) {
//            return false;
//        }
//
//        return eliminatedPlayers.add(username);
//    }
//
//    public boolean isMeetingActive() {
//        return meetingActive;
//    }
//
//    public void setMeetingActive(boolean meetingActive) {
//        this.meetingActive = meetingActive;
//    }
//
//    public synchronized boolean canCallEmergencyMeeting() {
//        return !meetingActive
//                && System.currentTimeMillis() >= emergencyCooldownUntil;
//    }
//
//    public synchronized void startEmergencyCooldown(long cooldownMillis) {
//        emergencyCooldownUntil =
//                System.currentTimeMillis() + cooldownMillis;
//    }
//
//    public long getEmergencyCooldownRemainingMillis() {
//        return Math.max(
//                0L,
//                emergencyCooldownUntil - System.currentTimeMillis()
//        );
//    }
//}

package com.example.demo.controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private final String roomCode;
    private final String host;

    private final Map<String, String> players =
            new ConcurrentHashMap<>();

    // Eliminate ဖြစ်ပြီးသား Player များ
    private final Set<String> eliminatedPlayers =
            ConcurrentHashMap.newKeySet();

    // Meeting / Voting state
    private volatile boolean meetingActive = false;

    // Voting ပြီးဆုံးပြီးနောက် Cooldown
    private volatile long emergencyCooldownUntil = 0L;

    private volatile boolean gameStarted = false;

    public GameRoom(String roomCode, String host) {
        this.roomCode = roomCode;
        this.host = host;

        // Host ကို Room ထဲ အလိုအလျောက်ထည့်
        players.put(host, host);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getHost() {
        return host;
    }

    public Map<String, String> getPlayers() {
        return players;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean addPlayer(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        return players.putIfAbsent(username, username) == null;
    }

    public void removePlayer(String username) {
        players.remove(username);
        eliminatedPlayers.remove(username);
    }

    public Set<String> getEliminatedPlayers() {
        return eliminatedPlayers;
    }

    public boolean isEliminated(String username) {
        return eliminatedPlayers.contains(username);
    }

    public boolean eliminatePlayer(String username) {
        if (username == null || !players.containsKey(username)) {
            return false;
        }

        return eliminatedPlayers.add(username);
    }

    public boolean isMeetingActive() {
        return meetingActive;
    }

    public void setMeetingActive(boolean meetingActive) {
        this.meetingActive = meetingActive;
    }

    public synchronized boolean canCallEmergencyMeeting() {
        return !meetingActive
                && System.currentTimeMillis() >= emergencyCooldownUntil;
    }

    public synchronized void startEmergencyCooldown(long cooldownMillis) {
        emergencyCooldownUntil =
                System.currentTimeMillis() + cooldownMillis;
    }

    public long getEmergencyCooldownRemainingMillis() {
        return Math.max(
                0L,
                emergencyCooldownUntil - System.currentTimeMillis()
        );
    }
}