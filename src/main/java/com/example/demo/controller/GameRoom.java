package com.example.demo.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private final String roomCode;
    private final String host;

    private final Map<String, String> players = new ConcurrentHashMap<>();

    private boolean gameStarted = false;

    public GameRoom(String roomCode, String host) {
        this.roomCode = roomCode;
        this.host = host;

        // Host ကို room ထဲ အလိုအလျောက်ထည့်
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
        if (players.containsKey(username)) {
            return false;
        }

        players.put(username, username);
        return true;
    }

    public void removePlayer(String username) {
        players.remove(username);
    }
}