package com.example.demo.service;

import com.example.demo.controller.GameRoom;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VotingService {

    private static final long COOLDOWN_MILLIS = 10_000L;

    // Room တစ်ခုချင်းစီအတွက် Voting Session သီးခြားထားမယ်။
    private final Map<String, VotingSession> sessions =
            new ConcurrentHashMap<>();

    /**
     * Emergency Meeting စတင်ခြင်း
     */
    public synchronized boolean startMeeting(GameRoom room) {

        if (room == null
                || !room.isGameStarted()
                || room.getPlayers().size() < 6
                || room.getPlayers().size() > 10
                || !room.canCallEmergencyMeeting()
                || room.isMeetingActive()) {

            return false;
        }

        Set<String> eligiblePlayers = new HashSet<>(
                room.getPlayers().keySet()
        );

        // Eliminate ဖြစ်ပြီးသားသူတွေ မဲပေးခွင့်မရှိ။
        eligiblePlayers.removeAll(room.getEliminatedPlayers());

        if (eligiblePlayers.size() < 2) {
            return false;
        }

        String roomCode = room.getRoomCode();

        VotingSession session =
                new VotingSession(eligiblePlayers);

        // ဒီ Room မှာ Session ရှိပြီးသားဆို Meeting အသစ်မစရ။
        if (sessions.putIfAbsent(roomCode, session) != null) {
            return false;
        }

        room.setMeetingActive(true);

        return true;
    }

    /**
     * မဲပေးခြင်း
     *
     * target = "SKIP" ဆိုရင် မဲမပေးဘဲ Skip လုပ်ခြင်း။
     */
    public synchronized boolean castVote(
            GameRoom room,
            String voter,
            String target) {

        if (room == null
                || voter == null
                || target == null
                || voter.isBlank()
                || target.isBlank()) {

            return false;
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        if (session == null || session.finished) {
            return false;
        }

        // Meeting မှာ ပါဝင်ခွင့်ရှိတဲ့သူဖြစ်ရမယ်။
        if (!session.eligiblePlayers.contains(voter)) {
            return false;
        }

        // တစ်ကြိမ်ပဲ မဲပေးခွင့်ရှိမယ်။
        if (session.votes.containsKey(voter)) {
            return false;
        }

        boolean isSkip = "SKIP".equalsIgnoreCase(target);

        if (!isSkip) {

            // မဲပေးမယ့်သူဟာ အသက်ရှင်နေတဲ့ Eligible Player ဖြစ်ရမယ်။
            if (!session.eligiblePlayers.contains(target)) {
                return false;
            }

            // ကိုယ့်ကိုယ်ကို မဲပေးခွင့်မပြု။
            if (voter.equals(target)) {
                return false;
            }
        }

        session.votes.put(
                voter,
                isSkip ? "SKIP" : target
        );

        return true;
    }

    /**
     * Eligible Player အားလုံး မဲပေးပြီးပြီလား စစ်ခြင်း။
     */
    public synchronized boolean allPlayersVoted(GameRoom room) {

        if (room == null) {
            return false;
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        if (session == null || session.finished) {
            return false;
        }

        return session.votes.keySet()
                .containsAll(session.eligiblePlayers);
    }

    /**
     * လက်ရှိ Voting မှာ မဲပေးထားတဲ့အရေအတွက်။
     */
    public synchronized int getVotesCast(GameRoom room) {

        if (room == null) {
            return 0;
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        if (session == null || session.finished) {
            return 0;
        }

        return session.votes.size();
    }

    /**
     * မဲပေးခွင့်ရှိသူ အရေအတွက်။
     */
    public synchronized int getEligiblePlayerCount(GameRoom room) {

        if (room == null) {
            return 0;
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        if (session == null || session.finished) {
            return 0;
        }

        return session.eligiblePlayers.size();
    }

    /**
     * Voting ပြီးဆုံးခြင်း။
     *
     * အရေးကြီးချက်:
     * - မဲအများဆုံးရသူ တစ်ယောက်တည်းရှိမှ Eliminate လုပ်မယ်။
     * - မဲအများဆုံးရသူ များနေပြီး မဲတူရင် ဘယ်သူ့ကိုမှ မဖယ်ရှားဘူး။
     * - မဲအားလုံး Skip ဖြစ်ရင် ဘယ်သူ့ကိုမှ မဖယ်ရှားဘူး။
     * - Voting ပြီးဆုံးပြီးမှ ၁၀ စက္ကန့် Cooldown စမယ်။
     */
    public synchronized Map<String, Object> finishVoting(
            GameRoom room) {

        if (room == null) {
            return resultError(
                    "ROOM_NOT_FOUND",
                    "Room not found"
            );
        }

        String roomCode = room.getRoomCode();

        VotingSession session = sessions.get(roomCode);

        if (session == null || session.finished) {
            return resultError(
                    "NO_ACTIVE_VOTING",
                    "No active voting session"
            );
        }


        // မဲမပေးရသေးသူ ရှိနေသေးရင် Voting ကို အလိုအလျောက်
        // ပြီးဆုံးခွင့်မပေးဘူး။ Timeout ကို Handler က သတ်မှတ်နိုင်တယ်။
        //
        // Timeout အတွက် finishVotingAfterTimeout() ကို သုံးမယ်။

        return finishVotingInternal(room, session);
    }

    /**
     * Voting အချိန်ကုန်သွားတဲ့အခါ ခေါ်ရန်။
     *
     * မဲပေးပြီးသားသူတွေရဲ့ မဲကိုသာ ရေတွက်မယ်။
     */
    public synchronized Map<String, Object> finishVotingAfterTimeout(
            GameRoom room) {

        if (room == null) {
            return resultError("ROOM_NOT_FOUND",
                    "Room not found");
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        if (session == null || session.finished) {
            return resultError("NO_ACTIVE_VOTING",
                    "No active voting session");
        }

        return finishVotingInternal(room, session);
    }

    private Map<String, Object> finishVotingInternal(
            GameRoom room,
            VotingSession session) {

        String roomCode = room.getRoomCode();

        // မဲအရေအတွက်
        Map<String, Integer> voteCounts = new HashMap<>();

        for (String target : session.votes.values()) {

            if (!"SKIP".equals(target)) {
                voteCounts.merge(target, 1, Integer::sum);
            }
        }

        String eliminatedUsername = null;
        boolean tie = false;

        if (!voteCounts.isEmpty()) {

            int maxVotes = Collections.max(
                    voteCounts.values()
            );

            int leaders = 0;
            String leader = null;

            for (Map.Entry<String, Integer> entry
                    : voteCounts.entrySet()) {

                if (entry.getValue() == maxVotes) {
                    leaders++;
                    leader = entry.getKey();
                }
            }

            if (leaders == 1) {
                eliminatedUsername = leader;
            } else {
                tie = true;
            }
        }

        // အနိုင်ရသူတစ်ယောက်တည်းရှိမှ Eliminate လုပ်မယ်။
        if (eliminatedUsername != null) {
            room.eliminatePlayer(eliminatedUsername);
        }

        session.finished = true;

        sessions.remove(roomCode, session);

        // Voting ပြီးမှ Meeting ကို ပိတ်မယ်။
        room.setMeetingActive(false);

        // Cooldown ကို အခုမှ စမယ်။
        room.startEmergencyCooldown(COOLDOWN_MILLIS);

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("roomCode", roomCode);
        result.put("voteCounts", voteCounts);
        result.put("eliminatedUsername", eliminatedUsername);
        result.put("tie", tie);
        result.put("votesCast", session.votes.size());
        result.put("eligiblePlayers",
                session.eligiblePlayers.size());
        result.put("cooldownMillis", COOLDOWN_MILLIS);

        return result;
    }

    /**
     * Meeting ကို ဖျက်သိမ်းခြင်း။
     *
     * Cooldown မစပါ။
     */
    public synchronized void cancelMeeting(GameRoom room) {

        if (room == null) {
            return;
        }

        String roomCode = room.getRoomCode();

        VotingSession session = sessions.remove(roomCode);

        if (session != null) {
            session.finished = true;
        }

        room.setMeetingActive(false);
    }

    /**
     * လက်ရှိ Room မှာ Voting Session ရှိမရှိ။
     */
    public synchronized boolean hasActiveVoting(GameRoom room) {

        if (room == null) {
            return false;
        }

        VotingSession session =
                sessions.get(room.getRoomCode());

        return session != null && !session.finished;
    }

    private Map<String, Object> resultError(
            String code,
            String message) {

        Map<String, Object> result = new HashMap<>();

        result.put("success", false);
        result.put("code", code);
        result.put("message", message);

        return result;
    }

    private static class VotingSession {

        private final Set<String> eligiblePlayers;

        private final Map<String, String> votes =
                new HashMap<>();

        private boolean finished = false;

        private VotingSession(Set<String> eligiblePlayers) {
            this.eligiblePlayers =
                    new HashSet<>(eligiblePlayers);
        }
    }
}