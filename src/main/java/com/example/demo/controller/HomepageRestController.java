package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/homepage")
public class HomepageRestController {

    private String userEmail = "player@example.com";
    private int coins = 250; // ယာယီ Coin ပမာဏ

    // Home Page ထဲကို Data တွေ လှမ်းပေးမည့် API
    @GetMapping("/data")
    public Map<String, Object> getHomepageData() {
        Map<String, Object> response = new HashMap<>();
        response.put("email", userEmail);
        response.put("coins", coins);
        return response;
    }

    // Coin များကို တိုးပေးရန် API
    @PostMapping("/add-coins")
    public Map<String, Object> addCoins(@RequestParam int amount) {
        coins += amount;
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCoins", coins);
        return response;
    }
}