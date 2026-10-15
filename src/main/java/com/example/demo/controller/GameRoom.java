package com.example.demo.controller;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public class GameRoom {
    private final String roomCode;
    private final String host;

    // Player တစ်ယောက်ချင်းစီရဲ့ အချက်အလက်များကို သိမ်းဆည်းရန် (Username -> PlayerInfo)
    private final Map<String, PlayerInfo> players = new ConcurrentHashMap<>();

    // ရနိုင်သော အရောင်များစာရင်း
    private final List<String> availableColors = new ArrayList<>(Arrays.asList(
            "Red", "Blue", "Green", "Pink", "Black", "Orange", "Yellow", "Purple"
    ));

    private boolean gameStarted = false;

    public GameRoom(String roomCode, String host) {
        this.roomCode = roomCode;
        this.host = host;

        // Host ကို room ထဲ အလိုအလျောက်ထည့်ပြီး အရောင်ပေးမည်
        addPlayer(host,"Red");
    }

    // Player အချက်အလက်များကို သိမ်းဆည်းမည့် Inner Class
    public static class PlayerInfo {
        private String username;
        private String color;
        private String role; // "crewmate" သို့မဟုတ် "killer"

        public PlayerInfo(String username, String color, String role) {
            this.username = username;
            this.color = color;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getHost() {
        return host;
    }

    public Map<String, PlayerInfo> getPlayers() {
        return players;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean addPlayer(String username, String preferredColor) {
        if (players.containsKey(username)) {
            return false;
        }

        // ကစားသမား ရွေးထားတဲ့ အရောင်ကို Room ထဲမှာ ဘယ်သူမှ မသုံးရသေးရင် အဲ့ဒီအရောင်ကိုပဲ ပေးမည်
        String assignedColor = preferredColor;

        boolean colorTaken = players.values().stream()
                .anyMatch(p -> p.getColor().equalsIgnoreCase(preferredColor));

        // တကယ်လို့ ရွေးထားတဲ့အရောင်က ယူထားပြီးသား ဖြစ်နေရင် (သို့မဟုတ် Null ဖြစ်နေရင်)
        if (colorTaken || preferredColor == null || preferredColor.trim().isEmpty()) {
            synchronized (availableColors) {
                if (!availableColors.isEmpty()) {
                    assignedColor = availableColors.remove(0); // အလွတ်ရှိတဲ့ အရောင်တစ်ခုကို ယူမည်
                } else {
                    assignedColor = "Gray"; // အရောင်ကုန်သွားရင် Default
                }
            }
        } else {
            // ရွေးထားတဲ့အရောင်ကို availableColors စာရင်းထဲကနေ ဖယ်ထုတ်မည်
            availableColors.remove(preferredColor);
        }

        PlayerInfo newPlayer = new PlayerInfo(username, assignedColor, "crewmate");
        players.put(username, newPlayer);
        return true;
    }

    public void removePlayer(String username) {
        PlayerInfo removed = players.remove(username);
        if (removed != null) {
            // ထွက်သွားသော Player ၏ အရောင်ကို ပြန်ထည့်ပေးမည် (နောက်လူတွေ သုံးလို့ရအောင်)
            synchronized (availableColors) {
                availableColors.add(removed.getColor());
            }
        }
    }

    // ဂိမ်းစတဲ့အခါ Killer တစ်ဦးကို Random ရွေးချယ်ပေးမည့် Method
    public void assignRolesRandomly() {
        List<PlayerInfo> playerList = new ArrayList<>(players.values());
        if (playerList.isEmpty()) return;

        // Random ဖြင့် Killer တစ်ယောက် ရွေးမည်
        Random random = new Random();
        int killerIndex = random.nextInt(playerList.size());

        for (int i = 0; i < playerList.size(); i++) {
            if (i == killerIndex) {
                playerList.get(i).setRole("killer");
            } else {
                playerList.get(i).setRole("crewmate");
            }
        }
    }
}