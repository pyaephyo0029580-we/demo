package com.example.demo.controller;

import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


@RestController
@RequestMapping("/api/friends")
public class FriendApiController {

    private final FriendRepository friendRepository;

    private final UserRepository userRepository;

    private final SecureRandom random =
            new SecureRandom();


    public FriendApiController(
            FriendRepository friendRepository,
            UserRepository userRepository
    ) {

        this.friendRepository =
                friendRepository;

        this.userRepository =
                userRepository;
    }


    // =====================================================
    // GET LOGIN USER FROM SESSION
    // =====================================================

    private Optional<User> getLoggedInUser(
            HttpSession session
    ) {

        Long userId =
                (Long) session.getAttribute(
                        "userId"
                );


        if (userId == null) {

            return Optional.empty();
        }


        return userRepository.findById(
                userId
        );
    }


    // =====================================================
    // CURRENT LOGIN USER
    // =====================================================

    @GetMapping("/me")
    public ResponseEntity<?> getMe(
            HttpSession session
    ) {

        Optional<User> optionalUser =
                getLoggedInUser(
                        session
                );


        if (optionalUser.isEmpty()) {

            return notLoggedIn();
        }


        User user =
                ensureFriendCode(
                        optionalUser.get()
                );


        Map<String, Object> response =
                userData(
                        user
                );


        response.put(
                "success",
                true
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // SEARCH FRIEND
    // =====================================================

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            HttpSession session
    ) {

        Optional<User> currentOptional =
                getLoggedInUser(
                        session
                );


        if (currentOptional.isEmpty()) {

            return notLoggedIn();
        }


        String searchValue =
                query.trim();


        if (searchValue.isEmpty()) {

            return error(
                    "Please enter Username, Code or Player ID."
            );
        }


        Optional<User> targetOptional =
                findTargetUser(
                        searchValue
                );


        if (targetOptional.isEmpty()) {

            return error(
                    "Player not found."
            );
        }


        User currentUser =
                currentOptional.get();


        User targetUser =
                ensureFriendCode(
                        targetOptional.get()
                );


        if (currentUser.getId()
                .equals(targetUser.getId())) {

            return error(
                    "You cannot add yourself."
            );
        }


        Map<String, Object> response =
                userData(
                        targetUser
                );


        response.put(
                "success",
                true
        );return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // SEND FRIEND REQUEST
    // =====================================================

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {

        Optional<User> requesterOptional =
                getLoggedInUser(
                        session
                );


        if (requesterOptional.isEmpty()) {

            return notLoggedIn();
        }


        String targetValue =
                request.getOrDefault(
                        "target",
                        ""
                ).trim();


        if (targetValue.isEmpty()) {

            return error(
                    "Target player is required."
            );
        }


        Optional<User> receiverOptional =
                findTargetUser(
                        targetValue
                );


        if (receiverOptional.isEmpty()) {

            return error(
                    "Player not found."
            );
        }


        User requester =
                requesterOptional.get();


        User receiver =
                receiverOptional.get();


        if (requester.getId()
                .equals(receiver.getId())) {

            return error(
                    "You cannot add yourself."
            );
        }


        Optional<Friend> forward =
                friendRepository
                        .findByRequesterIdAndReceiverId(
                                requester.getId(),
                                receiver.getId()
                        );


        Optional<Friend> reverse =
                friendRepository
                        .findByRequesterIdAndReceiverId(
                                receiver.getId(),
                                requester.getId()
                        );


        if (forward.isPresent()) {

            String status =
                    forward.get()
                            .getStatus();


            if ("PENDING".equalsIgnoreCase(status)) {

                return error(
                        "Friend request already sent."
                );
            }


            if ("ACCEPTED".equalsIgnoreCase(status)) {

                return error(
                        "This player is already your friend."
                );
            }
        }


        if (reverse.isPresent()) {

            String status =
                    reverse.get()
                            .getStatus();


            if ("PENDING".equalsIgnoreCase(status)) {

                return error(
                        "This player already sent you a friend request."
                );
            }


            if ("ACCEPTED".equalsIgnoreCase(status)) {

                return error(
                        "This player is already your friend."
                );
            }
        }


        Friend friend =
                new Friend();


        friend.setRequesterId(
                requester.getId()
        );


        friend.setReceiverId(
                receiver.getId()
        );


        friend.setStatus(
                "PENDING"
        );


        Friend saved =
                friendRepository.save(
                        friend
                );


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Friend request sent!"
        );

        response.put(
                "friendId",
                saved.getFriendId()
        );


        return ResponseEntity.ok(
                response
        );
    }


// =====================================================
// INCOMING REQUESTS
// =====================================================
@GetMapping("/requests")
public ResponseEntity<?> getRequests(
        HttpSession session
) {

    Optional<User> optionalUser =
            getLoggedInUser(
                    session
            );


    if (optionalUser.isEmpty()) {

        return notLoggedIn();
    }


    Long currentUserId =
            optionalUser
                    .get()
                    .getId();


    List<Friend> requests =
            friendRepository
                    .findByReceiverIdAndStatus(
                            currentUserId,
                            "PENDING"
                    );


    List<Map<String, Object>> result =
            new ArrayList<>();


    for (Friend friend : requests) {

        Optional<User> senderOptional =
                userRepository.findById(
                        friend.getRequesterId()
                );


        if (senderOptional.isPresent()) {

            User sender =
                    ensureFriendCode(
                            senderOptional.get()
                    );


            Map<String, Object> item =
                    userData(
                            sender
                    );


            item.put(
                    "friendId",
                    friend.getFriendId()
            );


            result.add(
                    item
            );
        }
    }


    return ResponseEntity.ok(
            result
    );
}


    // =====================================================
    // ACCEPT
    // =====================================================

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<?> accept(
            @PathVariable Long friendId,
            HttpSession session
    ) {

        Optional<User> optionalUser =
                getLoggedInUser(
                        session
                );


        if (optionalUser.isEmpty()) {

            return notLoggedIn();
        }


        Long currentUserId =
                optionalUser
                        .get()
                        .getId();


        Optional<Friend> friendOptional =
                friendRepository.findById(
                        friendId
                );


        if (friendOptional.isEmpty()) {

            return error(
                    "Friend request not found."
            );
        }


        Friend friend =
                friendOptional.get();


        // Request က login ဝင်ထားတဲ့ user ဆီပို့ထားတာ
        // ဟုတ်မဟုတ်စစ်
        if (!friend.getReceiverId()
                .equals(currentUserId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "This request is not for you."
                            )
                    );
        }


        if (!"PENDING".equalsIgnoreCase(
                friend.getStatus()
        )) {

            return error(
                    "Request is not pending."
            );
        }


        friend.setStatus(
                "ACCEPTED"
        );


        friendRepository.save(
                friend
        );


        return success(
                "Friend request accepted."
        );
    }


    // =====================================================
    // REJECT
    // =====================================================

    @PostMapping("/{friendId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long friendId,
            HttpSession session
    ) {

        Optional<User> optionalUser =
                getLoggedInUser(
                        session
                );


        if (optionalUser.isEmpty()) {

            return notLoggedIn();
        }Long currentUserId =
                optionalUser
                        .get()
                        .getId();


        Optional<Friend> friendOptional =
                friendRepository.findById(
                        friendId
                );


        if (friendOptional.isEmpty()) {

            return error(
                    "Friend request not found."
            );
        }


        Friend friend =
                friendOptional.get();


        if (!friend.getReceiverId()
                .equals(currentUserId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "This request is not for you."
                            )
                    );
        }


        friendRepository.delete(
                friend
        );


        return success(
                "Friend request rejected."
        );
    }


    // =====================================================
    // FRIEND LIST
    // =====================================================

    @GetMapping("/list")
    public ResponseEntity<?> getFriends(
            HttpSession session
    ) {

        Optional<User> optionalUser =
                getLoggedInUser(
                        session
                );


        if (optionalUser.isEmpty()) {

            return notLoggedIn();
        }


        Long currentUserId =
                optionalUser
                        .get()
                        .getId();


        Set<Long> friendUserIds =
                new LinkedHashSet<>();


        List<Friend> outgoing =
                friendRepository
                        .findByRequesterIdAndStatus(
                                currentUserId,
                                "ACCEPTED"
                        );


        for (Friend friend : outgoing) {

            friendUserIds.add(
                    friend.getReceiverId()
            );
        }


        List<Friend> incoming =
                friendRepository
                        .findByReceiverIdAndStatus(
                                currentUserId,
                                "ACCEPTED"
                        );


        for (Friend friend : incoming) {

            friendUserIds.add(
                    friend.getRequesterId()
            );
        }


        List<Map<String, Object>> result =
                new ArrayList<>();


        for (Long friendUserId : friendUserIds) {

            Optional<User> friendOptional =
                    userRepository.findById(
                            friendUserId
                    );


            if (friendOptional.isPresent()) {

                User friendUser =
                        ensureFriendCode(
                                friendOptional.get()
                        );


                result.add(
                        userData(
                                friendUser
                        )
                );
            }
        }


        return ResponseEntity.ok(
                result
        );
    }


    // =====================================================
    // SEARCH TARGET
    // =====================================================

    private Optional<User> findTargetUser(
            String value
    ) {

        String search =
                value.trim();


        // Example:
        // Shoon#274143
        if (search.contains("#")) {

            int index =
                    search.lastIndexOf(
                            "#"
                    );


            String name =
                    search.substring(
                            0,
                            index
                    ).trim();


            String code =
                    search.substring(
                            index + 1
                    ).trim();Optional<User> optionalUser =
                    userRepository
                            .findByFriendCode(
                                    code
                            );


            if (optionalUser.isEmpty()) {

                return Optional.empty();
            }


            User user =
                    optionalUser.get();


            if (user.getUsername() != null
                    && user.getUsername()
                    .equalsIgnoreCase(name)) {

                return optionalUser;
            }


            return Optional.empty();
        }


        // Search Username
        Optional<User> usernameResult =
                userRepository.findByUsername(
                        search
                );


        if (usernameResult.isPresent()) {

            return usernameResult;
        }


        // Search Friend Code
        return userRepository
                .findByFriendCode(
                        search
                );
    }


    // =====================================================
    // FRIEND CODE
    // =====================================================

    private User ensureFriendCode(
            User user
    ) {

        if (user.getFriendCode() == null
                || user.getFriendCode()
                .isBlank()) {

            user.setFriendCode(
                    generateFriendCode()
            );


            user =
                    userRepository.save(
                            user
                    );
        }


        return user;
    }


    private String generateFriendCode() {

        String code;


        do {

            int number =
                    100000
                            + random.nextInt(
                            900000
                    );


            code =
                    String.valueOf(
                            number
                    );


        } while (
                userRepository
                        .existsByFriendCode(
                                code
                        )
        );


        return code;
    }


    // =====================================================
    // USER JSON
    // =====================================================

    private Map<String, Object> userData(
            User user
    ) {

        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "id",
                user.getId()
        );

        data.put(
                "username",
                user.getUsername()
        );

        data.put(
                "friendCode",
                user.getFriendCode()
        );

        data.put(
                "playerId",
                user.getUsername()
                        + "#"
                        + user.getFriendCode()
        );


        return data;
    }


    // =====================================================
    // RESPONSES
    // =====================================================

    private ResponseEntity<Map<String, Object>>
    success(String message) {

        Map<String, Object> result =
                new HashMap<>();


        result.put(
                "success",
                true
        );

        result.put(
                "message",
                message
        );


        return ResponseEntity.ok(
                result
        );
    }


    private ResponseEntity<Map<String, Object>>
    error(String message) {

        Map<String, Object> result =
                new HashMap<>();


        result.put(
                "success",
                false
        );

        result.put(
                "message",
                message
        );


        return ResponseEntity
                .badRequest()
                .body(result);
    }


    private ResponseEntity<Map<String, Object>>
    notLoggedIn() {

        Map<String, Object> result =
                new HashMap<>();


        result.put(
                "success",
                false
        );result.put(
                "message",
                "Please login first."
        );


        return ResponseEntity
                .status(401)
                .body(result);
    }
}