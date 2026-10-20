//package com.example.demo.controller;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class GameRoom {
//
//    private final String roomCode;
//    private final String host;
//
//    // Username -> Username
//    private final Map<String, String> players =
//            new ConcurrentHashMap<>();
//
//    // 🟢 Username -> Player Color (Room ထဲရှိ Player တစ်ဦးချင်းစီ၏ အရောင်များကို မှတ်ရန်)
//    private final Map<String, String> playerColors =
//            new ConcurrentHashMap<>();
//
//    // 🟢 Username -> Role ("KILLER" သို့မဟုတ် "FINDER")
//    private final Map<String, String> playerRoles =
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
//    private volatile boolean gameStarted = false;
//
//    // HTML Mini-tasks ၂၀ စာရင်း
//    private static final List<String> ALL_TASKS = List.of(
//            "task_fix_wiring", "task_download_data", "task_calibrate_distributor",
//            "task_clean_filter", "task_unlock_manifold", "task_fuel_engines",
//            "task_prime_shields", "task_chart_course", "task_align_engine_output",
//            "task_swipe_card", "task_stabilize_steering", "task_store_artifacts",
//            "task_run_diagnostics", "task_clear_asteroids", "task_measure_weather",
//            "task_inspect_sample", "task_start_reactor", "task_reset_breakers",
//            "task_sort_samples", "task_enter_id_code"
//    );
//
//    // Map ထဲရှိ သတ်မှတ်ထားသော နေရာ ၄ ခုအတွက် Random Task များ ရွေးထုတ်ပေးမည့် Method
//    public List<Map<String, Object>> getRandomTasksForLocations() {
//        List<String> shuffledTasks = new ArrayList<>(ALL_TASKS);
//        Collections.shuffle(shuffledTasks);
//
//        List<Map<String, Object>> assignedTasks = new ArrayList<>();
//
//        for (int i = 1; i <= 4; i++) {
//            Map<String, Object> taskData = new HashMap<>();
//            taskData.put("locationId", "TaskLocation_" + i);
//            taskData.put("taskName", shuffledTasks.get(i - 1));
//            assignedTasks.add(taskData);
//        }
//
//        return assignedTasks;
//    }
//
//    // =====================================================
//    // 🟢 Killer နှင့် Finder Role များကို အလိုအလျောက် သတ်မှတ်ပေးသည့် Method
//    // =====================================================
//    public void assignRoles() {
//        List<String> playerList = new ArrayList<>(players.keySet());
//        if (playerList.isEmpty()) {
//            return;
//        }
//
//        // ကစားသမားစာရင်းကို ကျပန်း ရောနှောရန် (Shuffle)
//        Collections.shuffle(playerList);
//        int totalPlayers = playerList.size();
//
//        // ကစားသမား ၇ ယောက်မှ ၁၀ ယောက်ကြားဆိုလျှင် Killer ၂ ယောက်၊ ကျန်လျှင် ၁ ယောက်
//        int killerCount = (totalPlayers >= 7 && totalPlayers <= 10) ? 2 : 1;
//
//        playerRoles.clear();
//        for (int i = 0; i < totalPlayers; i++) {
//            String username = playerList.get(i);
//            if (i < killerCount) {
//                playerRoles.put(username, "KILLER");
//            } else {
//                playerRoles.put(username, "FINDER");
//            }
//        }
//    }
//
//    public Map<String, String> getPlayerRoles() {
//        return playerRoles;
//    }
//
//    public String getPlayerRole(String username) {
//        return playerRoles.getOrDefault(username, "FINDER");
//    }
//
//    public GameRoom(String roomCode, String host) {
//        this.roomCode = roomCode;
//        this.host = host;
//
//        // Host ကို Room ထဲ အလိုအလျောက်ထည့်
//        players.put(host, host);
//        playerColors.put(host, "#C51111"); // Default host color
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
//    // 🟢 Player တစ်ဦးချင်းစီ၏ အရောင်များကို ရယူရန်
//    public Map<String, String> getPlayerColors() {
//        return playerColors;
//    }
//
//    // 🟢 Player ၏ အရောင်ကို သတ်မှတ်ရန် (သို့) ပြောင်းလဲရန်
//    public void setPlayerColor(String username, String color) {
//        if (username != null && color != null) {
//            playerColors.put(username, color);
//        }
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
//    public boolean addPlayer(String username) {
//        if (username == null || username.isBlank()) {
//            return false;
//        }
//
//        boolean added = players.putIfAbsent(username, username) == null;
//        if (added) {
//            // Default color when joining
//            playerColors.putIfAbsent(username, "#FFFFFF");
//        }
//        return added;
//    }
//
//    public void removePlayer(String username) {
//        players.remove(username);
//        playerColors.remove(username); // 🟢 Player ထွက်သွားပါက အရောင်ကိုပါ ဖယ်ရှားမည်
//        playerRoles.remove(username);
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private final String roomCode;
    private final String host;

    // Username -> Username
    private final Map<String, String> players =
            new ConcurrentHashMap<>();

    // 🟢 Username -> Player Color (Room ထဲရှိ Player တစ်ဦးချင်းစီ၏ အရောင်များကို မှတ်ရန်)
    private final Map<String, String> playerColors =
            new ConcurrentHashMap<>();

    // 🟢 Username -> Role ("KILLER" သို့မဟုတ် "FINDER")
    private final Map<String, String> playerRoles =
            new ConcurrentHashMap<>();

    // Eliminate ဖြစ်ပြီးသား Player များ
    private final Set<String> eliminatedPlayers =
            ConcurrentHashMap.newKeySet();

    // 🟢 Voting State: Voter Username -> Target Username (သို့မဟုတ် "skip")
    private final Map<String, String> votes = new ConcurrentHashMap<>();

    // Meeting / Voting state
    private volatile boolean meetingActive = false;

    // Voting ပြီးဆုံးပြီးနောက် Cooldown
    private volatile long emergencyCooldownUntil = 0L;

    private volatile boolean gameStarted = false;

    // HTML Mini-tasks ၂၀ စာရင်း
    private static final List<String> ALL_TASKS = List.of(
            "task_fix_wiring", "task_download_data", "task_calibrate_distributor",
            "task_clean_filter", "task_unlock_manifold", "task_fuel_engines",
            "task_prime_shields", "task_chart_course", "task_align_engine_output",
            "task_swipe_card", "task_stabilize_steering", "task_store_artifacts",
            "task_run_diagnostics", "task_clear_asteroids", "task_measure_weather",
            "task_inspect_sample", "task_start_reactor", "task_reset_breakers",
            "task_sort_samples", "task_enter_id_code"
    );

    // Map ထဲရှိ သတ်မှတ်ထားသော နေရာ ၄ ခုအတွက် Random Task များ ရွေးထုတ်ပေးမည့် Method
    public List<Map<String, Object>> getRandomTasksForLocations() {
        List<String> shuffledTasks = new ArrayList<>(ALL_TASKS);
        Collections.shuffle(shuffledTasks);

        List<Map<String, Object>> assignedTasks = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("locationId", "TaskLocation_" + i);
            taskData.put("taskName", shuffledTasks.get(i - 1));
            assignedTasks.add(taskData);
        }

        return assignedTasks;
    }

    // =====================================================
    // 🟢 Killer နှင့် Finder Role များကို အလိုအလျောက် သတ်မှတ်ပေးသည့် Method
    // =====================================================
    public void assignRoles() {
        List<String> playerList = new ArrayList<>(players.keySet());
        if (playerList.isEmpty()) {
            return;
        }

        // ကစားသမားစာရင်းကို ကျပန်း ရောနှောရန် (Shuffle)
        Collections.shuffle(playerList);
        int totalPlayers = playerList.size();

        // ကစားသမား ၇ ယောက်မှ ၁၀ ယောက်ကြားဆိုလျှင် Killer ၂ ယောက်၊ ကျန်လျှင် ၁ ယောက်
        int killerCount = (totalPlayers >= 7 && totalPlayers <= 10) ? 2 : 1;

        playerRoles.clear();
        for (int i = 0; i < totalPlayers; i++) {
            String username = playerList.get(i);
            if (i < killerCount) {
                playerRoles.put(username, "KILLER");
            } else {
                playerRoles.put(username, "FINDER");
            }
        }
    }

    public Map<String, String> getPlayerRoles() {
        return playerRoles;
    }

    public String getPlayerRole(String username) {
        return playerRoles.getOrDefault(username, "FINDER");
    }

    public GameRoom(String roomCode, String host) {
        this.roomCode = roomCode;
        this.host = host;

        // Host ကို Room ထဲ အလိုအလျောက်ထည့်
        players.put(host, host);
        playerColors.put(host, "#C51111"); // Default host color
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

    // 🟢 Player တစ်ဦးချင်းစီ၏ အရောင်များကို ရယူရန်
    public Map<String, String> getPlayerColors() {
        return playerColors;
    }

    // 🟢 Player ၏ အရောင်ကို သတ်မှတ်ရန် (သို့) ပြောင်းလဲရန်
    public void setPlayerColor(String username, String color) {
        if (username != null && color != null) {
            playerColors.put(username, color);
        }
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

        boolean added = players.putIfAbsent(username, username) == null;
        if (added) {
            // Default color when joining
            playerColors.putIfAbsent(username, "#FFFFFF");
        }
        return added;
    }

    public void removePlayer(String username) {
        players.remove(username);
        playerColors.remove(username); // 🟢 Player ထွက်သွားပါက အရောင်ကိုပါ ဖယ်ရှားမည်
        playerRoles.remove(username);
        eliminatedPlayers.remove(username);
        votes.remove(username);
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
        if (!meetingActive) {
            clearVotes(); // Meeting ပြီးသွားရင် မဲများကို ရှင်းလင်းမည်
        }
    }

    // =====================================================
    // 🟢 VOTING METHODS (မဲပေးခြင်းနှင့် ဆုံးဖြတ်ခြင်းဆိုင်ရာများ)
    // =====================================================

    public synchronized boolean castVote(String voter, String target) {
        if (!meetingActive || voter == null || target == null) {
            return false;
        }
        // ထွက်သွားပြီးသားလူ (Eliminated) များ မဲမပေးရ
        if (eliminatedPlayers.contains(voter)) {
            return false;
        }
        // ကိုယ့်ကိုကိုယ် (Self-Vote) မဲပေးခွင့်မရှိပါ
        if (voter.equals(target)) {
            return false;
        }
        // Target သည် "skip" ဖြစ်ရမည် သို့မဟုတ် Room ထဲတွင် ရှိနေရမည်
        if (!target.equals("skip") && !players.containsKey(target)) {
            return false;
        }

        votes.put(voter, target);
        return true;
    }

    public Map<String, String> getVotes() {
        return votes;
    }

    public void clearVotes() {
        votes.clear();
    }

    // မဲများကိုရေတွက်ပြီး အကျိုးအကြောင်းဆုံးဖြတ်ပေးမည့် Method
    public Map<String, Object> resolveVotes() {
        Map<String, Integer> voteCounts = new HashMap<>();
        int skipCount = 0;

        for (String target : votes.values()) {
            if ("skip".equals(target)) {
                skipCount++;
            } else {
                voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
            }
        }

        int maxVotes = 0;
        String ejectedCandidate = null;
        boolean isTie = false;

        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            int count = entry.getValue();
            if (count > maxVotes) {
                maxVotes = count;
                ejectedCandidate = entry.getKey();
                isTie = false;
            } else if (count == maxVotes) {
                isTie = true; // မဲတူနေခြင်း (Tie)
            }
        }

        Map<String, Object> result = new HashMap<>();

        // Skip မဲက အများဆုံး သို့မဟုတ် မဲတူနေရင် (Tie) ဘယ်သူမှ မထွက်ရပါ
        if (skipCount > maxVotes || isTie || ejectedCandidate == null) {
            result.put("status", "safe");
            result.put("message", isTie ? "Votes were tied! No one was ejected." : "Skipped! No one was ejected.");
            result.put("ejected", null);
        } else {
            // အများစုမဲရသူ Out ဖြစ်မည်
            eliminatePlayer(ejectedCandidate);
            result.put("status", "ejected");
            result.put("ejected", ejectedCandidate);
            result.put("message", ejectedCandidate + " was ejected.");
        }

        return result;
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