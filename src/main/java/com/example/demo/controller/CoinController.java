package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/coin")
public class CoinController {

    @Autowired
    private UserRepository userRepository;

    // ⭐ User ရဲ့ Coin ကို ပြန်ပေးမယ့် API
    @GetMapping("/balance")
    public Map<String, Object> getUserCoins(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        Map<String, Object> result = new HashMap<>();

        if (userOpt.isPresent()) {
            result.put("coins", userOpt.get().getCoins());
        } else {
            result.put("coins", 0);
        }

        return result;
    }
}