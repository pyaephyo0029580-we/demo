package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;
import java.util.concurrent.*;

@Controller
public class GameController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Map<String, String> sessionUsers = new ConcurrentHashMap<>(); // sessionId -> username
    private static final Map<String, String> userVotes = new ConcurrentHashMap<>();    // sessionId -> votedTarget
    private static final Map<String, Integer> voteCounts = new ConcurrentHashMap<>();

    private static String currentPhase = "discussion";
    private static int timeLeft = 30;
    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> timerTask;
    private static boolean isTimerRunning = false;

    // 1. User Join လုပ်သောအခါ
    @MessageMapping("/join")
    public void handleJoin(StompHeaderAccessor accessor, Map<String, String> payload) {
        String sessionId = accessor.getSessionId();
        String voter = payload.get("voter");

        if (voter != null && !voter.trim().isEmpty() && !voter.equals("User")) {
            sessionUsers.put(sessionId, voter);
        }
        broadcastUserList();
        sendTimerUpdate();
    }

    // 2. User ထွက်သွားလျှင် (Disconnect ဖြစ်လျှင်)
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            sessionUsers.remove(sessionId);
            userVotes.remove(sessionId);
            recalculateVotes();
            broadcastUserList();
        }
    }

    private void broadcastUserList() {
        Set<String> uniqueUsers = new HashSet<>(sessionUsers.values());
        List<String> userList = new ArrayList<>(uniqueUsers);
        messagingTemplate.convertAndSend("/topic/userlist", userList);
        messagingTemplate.convertAndSend("/topic/users", uniqueUsers.size());
    }

    // 3. Hub (Chat & Voting) ကို ဖွင့်လိုက်သောအခါ Timer စတင်ရန်
    @MessageMapping("/request-timer")
    public synchronized void requestTimer() {
        if (!isTimerRunning) {
            resetGameRound("discussion", 30);
            isTimerRunning = true;
            startScheduler();
        } else {
            sendTimerUpdate();
        }
    }

    private void resetGameRound(String phase, int time) {
        currentPhase = phase;
        timeLeft = time;
        userVotes.clear(); // မဲပေးထားသည်များကို ရှင်းလင်းသည်
        voteCounts.clear();
        broadcastVotes();
    }

    private void startScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        if (timerTask != null) {
            timerTask.cancel(true);
        }

        timerTask = scheduler.scheduleAtFixedRate(() -> {
            if (timeLeft > 0) {
                timeLeft--;
            } else {
                if (currentPhase.equals("discussion")) {
                    currentPhase = "voting";
                    timeLeft = 60; // Voting 60s
                } else if (currentPhase.equals("voting")) {
                    currentPhase = "ended";
                    timeLeft = 0;
                    isTimerRunning = false;
                    if (timerTask != null) {
                        timerTask.cancel(true);
                    }
                }
            }
            sendTimerUpdate();
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void sendTimerUpdate() {
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("phase", currentPhase);
        timerData.put("timeLeft", timeLeft);
        messagingTemplate.convertAndSend("/topic/timer", timerData);
    }

    // 4. မဲပေးခြင်းအတွက် (1 User = 1 Vote)
    @MessageMapping("/vote")
    public void handleVote(StompHeaderAccessor accessor, Map<String, String> voteData) {
        if (!currentPhase.equals("voting")) {
            return; // မဲပေးချိန်မဟုတ်လျှင် လက်မခံပါ
        }

        String sessionId = accessor.getSessionId();
        String target = voteData.get("target");

        if (target != null && sessionId != null) {
            // ယခင်မဲပေးဖူးလျှင် အဟောင်းကို ဖယ်မည်
            userVotes.put(sessionId, target);
            recalculateVotes();
            broadcastVotes();
        }
    }

    private void recalculateVotes() {
        voteCounts.clear();
        for (String target : userVotes.values()) {
            voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        }
    }

    private void broadcastVotes() {
        Map<String, Object> response = new HashMap<>();
        response.put("voteCounts", voteCounts);
        messagingTemplate.convertAndSend("/topic/votes", response);
    }
}