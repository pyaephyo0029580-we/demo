package com.example.demo.controller;

import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.UserRepository;

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
    // CURRENT PLAYER
    // =====================================================

    @GetMapping("/me")
    public ResponseEntity<?> getMe(
            @RequestParam String username
    ) {

        Optional<User> optionalUser =
                userRepository.findByUsername(
                        username
                );


        if (optionalUser.isEmpty()) {

            return error(
                    "User not found."
            );
        }


        User user =
                ensureFriendCode(
                        optionalUser.get()
                );


        Map<String, Object> response =
                userData(user);


        response.put(
                "success",
                true
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // SEARCH
    //
    // Supports:
    // Boon
    // 823132
    // Boon#823132
    // =====================================================

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam String username
    ) {

        Optional<User> currentOptional =
                userRepository.findByUsername(
                        username
                );


        if (currentOptional.isEmpty()) {

            return error(
                    "Current user not found."
            );
        }


        String search =
                query.trim();


        if (search.isEmpty()) {

            return error(
                    "Please enter a player."
            );
        }


        Optional<User> targetOptional =
                findTargetUser(
                        search
                );


        if (targetOptional.isEmpty()) {

            return error(
                    "Player not found."
            );
        }


        User current =
                currentOptional.get();


        User target =
                ensureFriendCode(
                        targetOptional.get()
                );


        if (current.getId()
                .equals(target.getId())) {

            return error(
                    "You cannot add yourself."
            );
        }


        Map<String, Object> response =
                userData(target);


        response.put(
                "success",
                true
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // SEND FRIEND REQUEST
    // =====================================================

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(
            @RequestBody Map<String, String> request
    ) {

        String username =
                request.getOrDefault(
                        "username",
                        ""
                ).trim();String targetValue =
                request.getOrDefault(
                        "target",
                        ""
                ).trim();


        if (username.isEmpty()
                || targetValue.isEmpty()) {

            return error(
                    "Username and target are required."
            );
        }


        Optional<User> requesterOptional =
                userRepository.findByUsername(
                        username
                );


        if (requesterOptional.isEmpty()) {

            return error(
                    "Current user not found."
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
                ensureFriendCode(
                        requesterOptional.get()
                );


        User receiver =
                ensureFriendCode(
                        receiverOptional.get()
                );


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
                        "This player already sent you a request."
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
            @RequestParam String username
    ) {

        Optional<User> userOptional =
                userRepository.findByUsername(
                        username
                );if (userOptional.isEmpty()) {

            return error(
                    "User not found."
            );
        }


        User currentUser =
                userOptional.get();


        List<Friend> requests =
                friendRepository
                        .findByReceiverIdAndStatus(
                                currentUser.getId(),
                                "PENDING"
                        );


        List<Map<String, Object>> response =
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
                        userData(sender);


                item.put(
                        "friendId",
                        friend.getFriendId()
                );


                response.add(
                        item
                );
            }
        }


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // ACCEPT
    // =====================================================

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<?> accept(
            @PathVariable Long friendId,
            @RequestParam String username
    ) {

        Optional<User> userOptional =
                userRepository.findByUsername(
                        username
                );


        if (userOptional.isEmpty()) {

            return error(
                    "User not found."
            );
        }


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
                .equals(
                        userOptional
                                .get()
                                .getId()
                )) {

            return error(
                    "You cannot accept this request."
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
                "Friend request accepted!"
        );
    }


    // =====================================================
    // REJECT
    // =====================================================

    @PostMapping("/{friendId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long friendId,
            @RequestParam String username
    ) {

        Optional<User> userOptional =
                userRepository.findByUsername(
                        username
                );


        if (userOptional.isEmpty()) {

            return error(
                    "User not found."
            );
        }


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
                .equals(
                        userOptional
                                .get()
                                .getId()
                )) {return error(
                "You cannot reject this request."
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
            @RequestParam String username
    ) {

        Optional<User> userOptional =
                userRepository.findByUsername(
                        username
                );


        if (userOptional.isEmpty()) {

            return error(
                    "User not found."
            );
        }


        User current =
                userOptional.get();


        Set<Long> friendUserIds =
                new LinkedHashSet<>();


        List<Friend> outgoing =
                friendRepository
                        .findByRequesterIdAndStatus(
                                current.getId(),
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
                                current.getId(),
                                "ACCEPTED"
                        );


        for (Friend friend : incoming) {

            friendUserIds.add(
                    friend.getRequesterId()
            );
        }


        List<Map<String, Object>> response =
                new ArrayList<>();


        for (Long id : friendUserIds) {

            Optional<User> friendOptional =
                    userRepository.findById(id);


            if (friendOptional.isPresent()) {

                User friendUser =
                        ensureFriendCode(
                                friendOptional.get()
                        );


                response.add(
                        userData(friendUser)
                );
            }
        }


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // FIND TARGET
    // =====================================================

    private Optional<User> findTargetUser(
            String value
    ) {

        String search =
                value.trim();


        // Name#Code
        if (search.contains("#")) {

            int index =
                    search.lastIndexOf("#");


            String name =
                    search.substring(
                            0,
                            index
                    ).trim();


            String code =
                    search.substring(
                            index + 1
                    ).trim();


            Optional<User> byCode =
                    userRepository.findByFriendCode(
                            code
                    );


            if (byCode.isEmpty()) {

                return Optional.empty();
            }


            User user =
                    byCode.get();


            if (name.isEmpty()) {

                return byCode;
            }


            if (user.getUsername() != null
                    && user.getUsername()
                    .equalsIgnoreCase(name)) {

                return byCode;
            }


            return Optional.empty();
        }


        // Username
        Optional<User> byUsername =
                userRepository.findByUsername(
                        search
                );


        if (byUsername.isPresent()) {

            return byUsername;
        }


        // Friend code
        return userRepository
                .findByFriendCode(
                        search
                );
    }// =====================================================
    // ENSURE FRIEND CODE
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


    // =====================================================
    // USER DATA
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
    // GENERATE CODE
    // =====================================================

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
    // SUCCESS
    // =====================================================

    private ResponseEntity<Map<String, Object>>
    success(String message) {

        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "success",
                true
        );


        data.put(
                "message",
                message
        );


        return ResponseEntity.ok(
                data
        );
    }


    // =====================================================
    // ERROR
    // =====================================================

    private ResponseEntity<Map<String, Object>>
    error(String message) {

        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "success",
                false
        );


        data.put(
                "message",
                message
        );


        return ResponseEntity
                .badRequest()
                .body(data);
    }
}