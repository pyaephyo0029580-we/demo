package com.example.demo.controller;

import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api/friends")
public class FriendApiController {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    private final SecureRandom random = new SecureRandom();

    public FriendApiController(
            UserRepository userRepository,
            FriendRepository friendRepository
    ) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
    }

    // ==========================================
    // GET CURRENT USER
    // ==========================================

    @GetMapping("/me")
    public ResponseEntity<?> getMyPlayer(
            @RequestParam String username
    ) {

        Optional<User> optionalUser =
                userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(Map.of(
                            "success", false,
                            "message", "User not found."
                    ));
        }

        User user = optionalUser.get();

        // Friend code မရှိသေးရင် generate
        if (user.getFriendCode() == null ||
                user.getFriendCode().isBlank()) {

            user.setFriendCode(generateFriendCode());

            user = userRepository.save(user);
        }

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("friendCode", user.getFriendCode());

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // SEARCH PLAYER
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<?> searchPlayer(
            @RequestParam String playerId
    ) {

        if (playerId == null || playerId.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Enter Player ID."
                    )
            );
        }

        int hashPosition = playerId.lastIndexOf("#");

        if (hashPosition <= 0 ||
                hashPosition == playerId.length() - 1) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Use Player ID like Marsuki#123456"
                    )
            );
        }

        String username =
                playerId.substring(0, hashPosition).trim();

        String friendCode =
                playerId.substring(hashPosition + 1).trim();

        if (!friendCode.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Friend code must be 6 digits."
                    )
            );
        }

        Optional<User> optionalUser =
                userRepository.findByUsernameAndFriendCode(
                        username,
                        friendCode
                );

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(Map.of(
                            "success", false,
                            "message", "Player not found."
                    ));
        }

        User user = optionalUser.get();Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("friendCode", user.getFriendCode());

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // SEND FRIEND REQUEST
    // ==========================================

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(
            @RequestParam Long requesterId,
            @RequestParam Long receiverId
    ) {

        if (requesterId.equals(receiverId)) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "You cannot add yourself."
                    )
            );
        }

        if (!userRepository.existsById(requesterId) ||
                !userRepository.existsById(receiverId)) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "User not found."
                    )
            );
        }

        Optional<Friend> forward =
                friendRepository.findByRequesterIdAndReceiverId(
                        requesterId,
                        receiverId
                );

        if (forward.isPresent()) {

            Friend existing = forward.get();

            if ("ACCEPTED".equalsIgnoreCase(existing.getStatus())) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Already friends."
                        )
                );
            }

            if ("PENDING".equalsIgnoreCase(existing.getStatus())) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Friend request already sent."
                        )
                );
            }
        }

        Optional<Friend> reverse =
                friendRepository.findByRequesterIdAndReceiverId(
                        receiverId,
                        requesterId
                );

        if (reverse.isPresent()) {

            Friend existing = reverse.get();

            if ("ACCEPTED".equalsIgnoreCase(existing.getStatus())) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Already friends."
                        )
                );
            }

            if ("PENDING".equalsIgnoreCase(existing.getStatus())) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "This player already sent you a request."
                        )
                );
            }
        }

        Friend request = new Friend(
                requesterId,
                receiverId,
                "PENDING"
        );

        friendRepository.save(request);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Friend request sent."
                )
        );
    }

    // ==========================================
    // GET INCOMING REQUESTS
    // ==========================================

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(
            @RequestParam Long userId
    ) {

        List<Friend> requests =
                friendRepository.findByReceiverIdAndStatus(
                        userId,
                        "PENDING"
                );

        List<Map<String, Object>> result =
                new ArrayList<>();for (Friend request : requests) {

            Optional<User> optionalRequester =
                    userRepository.findById(
                            request.getRequesterId()
                    );

            if (optionalRequester.isEmpty()) {
                continue;
            }

            User requester = optionalRequester.get();

            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "friendId",
                    request.getFriendId()
            );

            item.put(
                    "userId",
                    requester.getId()
            );

            item.put(
                    "username",
                    requester.getUsername()
            );

            item.put(
                    "friendCode",
                    requester.getFriendCode()
            );

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // ACCEPT REQUEST
    // ==========================================

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<?> acceptRequest(
            @PathVariable Long friendId,
            @RequestParam Long userId
    ) {

        Optional<Friend> optionalFriend =
                friendRepository.findById(friendId);

        if (optionalFriend.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Friend request not found."
                    ));
        }

        Friend friend = optionalFriend.get();

        if (!friend.getReceiverId().equals(userId)) {
            return ResponseEntity
                    .status(403)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "You cannot accept this request."
                    ));
        }

        if (!"PENDING".equalsIgnoreCase(friend.getStatus())) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Request is no longer pending."
                    )
            );
        }

        friend.setStatus("ACCEPTED");

        friendRepository.save(friend);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Friend request accepted."
                )
        );
    }

    // ==========================================
    // REJECT REQUEST
    // ==========================================

    @DeleteMapping("/{friendId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long friendId,
            @RequestParam Long userId
    ) {

        Optional<Friend> optionalFriend =
                friendRepository.findById(friendId);

        if (optionalFriend.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Friend request not found."
                    ));
        }

        Friend friend = optionalFriend.get();

        if (!friend.getReceiverId().equals(userId)) {
            return ResponseEntity
                    .status(403)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "You cannot reject this request."
                    ));
        }

        friendRepository.delete(friend);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Friend request rejected."
                )
        );
    }// ==========================================
    // GET FRIEND LIST
    // ==========================================

    @GetMapping("/list")
    public ResponseEntity<?> getFriends(
            @RequestParam Long userId
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        List<Friend> sent =
                friendRepository.findByRequesterIdAndStatus(
                        userId,
                        "ACCEPTED"
                );

        for (Friend friend : sent) {
            addFriendToResult(
                    result,
                    friend.getReceiverId()
            );
        }

        List<Friend> received =
                friendRepository.findByReceiverIdAndStatus(
                        userId,
                        "ACCEPTED"
                );

        for (Friend friend : received) {
            addFriendToResult(
                    result,
                    friend.getRequesterId()
            );
        }

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void addFriendToResult(
            List<Map<String, Object>> result,
            Long userId
    ) {

        Optional<User> optionalUser =
                userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        Map<String, Object> item =
                new HashMap<>();

        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("friendCode", user.getFriendCode());

        result.add(item);
    }

    private String generateFriendCode() {

        String code;

        do {
            int number =
                    100000 + random.nextInt(900000);

            code = String.valueOf(number);

        } while (userRepository.existsByFriendCode(code));

        return code;
    }
}