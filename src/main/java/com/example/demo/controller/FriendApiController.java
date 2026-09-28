package com.example.demo.controller;

import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
public class FriendApiController {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    public FriendApiController(
            UserRepository userRepository,
            FriendRepository friendRepository
    ) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
    }


    // ==========================================
    // SEARCH PLAYER
    // Example:
    // /api/friends/search?playerId=Jake#670455
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<?> searchPlayer(
            @RequestParam String playerId
    ) {

        try {

            int hashPosition = playerId.lastIndexOf("#");

            if (hashPosition <= 0 ||
                    hashPosition == playerId.length() - 1) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Use Name#Code"
                        ));
            }

            String username =
                    playerId.substring(0, hashPosition).trim();

            String friendCode =
                    playerId.substring(hashPosition + 1).trim();


            Optional<User> optionalUser =
                    userRepository.findByUsernameAndFriendCode(
                            username,
                            friendCode
                    );


            if (optionalUser.isEmpty()) {

                return ResponseEntity.ok(
                        Map.of(
                                "success", false,
                                "message", "Player not found"
                        )
                );
            }


            User user = optionalUser.get();


            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("friendCode", user.getFriendCode());

            return ResponseEntity.ok(result);


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Search failed"
                    ));
        }
    }


    // ==========================================
    // SEND FRIEND REQUEST
    // ==========================================

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestParam Long requesterId,
            @RequestParam Long receiverId
    ) {

        if (requesterId.equals(receiverId)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "You cannot add yourself"
                    ));
        }


        Optional<Friend> first =
                friendRepository
                        .findByRequesterIdAndReceiverId(
                                requesterId,
                                receiverId
                        );


        Optional<Friend> second =
                friendRepository
                        .findByRequesterIdAndReceiverId(
                                receiverId,
                                requesterId
                        );


        if (first.isPresent() || second.isPresent()) {return ResponseEntity.ok(
                Map.of(
                        "success", false,
                        "message",
                        "Friend request already exists"
                )
        );
        }


        Friend friend =
                new Friend(
                        requesterId,
                        receiverId,
                        "PENDING"
                );


        friendRepository.save(friend);


        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Friend request sent"
                )
        );
    }
}