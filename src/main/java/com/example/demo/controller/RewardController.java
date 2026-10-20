package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class RewardController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/reward-winners")
    public ResponseEntity<?> rewardWinners(@RequestBody RewardRequest request) {
        try {
            List<String> winners = request.getWinners();
            String winningTeam = request.getWinningTeam();
            int coinsToAdd = request.getCoins(); // 100 coins

            if (winners == null || winners.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "No winners provided"));
            }

            // Winner တစ်ဦးချင်းစီ၏ အကောင့်ထဲသို့ coins ပေါင်းထည့်ခြင်း
            for (String username : winners) {
                if (username == null || username.trim().isEmpty()) continue;

                Optional<User> optionalUser = userRepository.findByUsername(username.trim());
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    user.setCoins(user.getCoins() + coinsToAdd);
                    userRepository.save(user);
                }
            }

            return ResponseEntity.ok(new ApiResponse(true, "Successfully rewarded 100 coins to winners!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    // Request Data Transfer Object (DTO)
    public static class RewardRequest {
        private List<String> winners;
        private String winningTeam;
        private int coins;

        public List<String> getWinners() { return winners; }
        public void setWinners(List<String> winners) { this.winners = winners; }
        public String getWinningTeam() { return winningTeam; }
        public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }
        public int getCoins() { return coins; }
        public void setCoins(int coins) { this.coins = coins; }
    }

    // Response Data Transfer Object (DTO)
    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}