package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class VerificationController {

    private final UserRepository userRepository;

    public VerificationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestBody Map<String, String> request
    ) {

        Map<String, Object> response =
                new HashMap<>();

        String email =
                request.get("email");

        String code =
                request.get("code");


        if (email == null || email.isBlank()) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Email is required."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        if (code == null || code.isBlank()) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Verification code is required."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        Optional<User> optionalUser =
                userRepository.findByEmail(email);


        if (optionalUser.isEmpty()) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "User not found."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        User user =
                optionalUser.get();


        if (user.getVerificationCode() == null) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Verification code not found."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        if (!user.getVerificationCode().equals(code)) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Invalid verification code."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        user.setVerified(true);

        /*
         * မင်းအရင်ပြောထားသလို
         * verification code ကို DB မှာ
         * မဖျက်ဘူး။
         */

        userRepository.save(user);


        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Email verified successfully."
        );


        return ResponseEntity.ok(
                response
        );
    }
}