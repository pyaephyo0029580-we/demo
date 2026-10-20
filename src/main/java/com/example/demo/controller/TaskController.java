package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/complete-task")
    public ResponseEntity<?> completeTask(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username is required"));
        }

        // Database မှ username ဖြင့် ရှာဖွေခြင်း
        Optional<User> optionalUser = userRepository.findByUsername(username.trim());

        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        User user = optionalUser.get();

        // Coins 20 ပေါင်းထည့်ခြင်း
        int currentCoins = user.getCoins();
        user.setCoins(currentCoins + 20);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "20 coins added successfully",
                "newCoins", user.getCoins()
        ));
    }
}