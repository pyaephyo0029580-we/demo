package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserRepository userRepository;
    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom random =
            new SecureRandom();


    public AuthApiController(
            UserRepository userRepository,
            EmailService emailService
    ) {

        this.userRepository =
                userRepository;

        this.emailService =
                emailService;
    }


    // =====================================================
    // SIGNUP
    // =====================================================

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody Map<String, String> request
    ) {

        String username =
                request.getOrDefault(
                        "username",
                        ""
                ).trim();

        String email =
                request.getOrDefault(
                        "email",
                        ""
                ).trim();

        String password =
                request.getOrDefault(
                        "password",
                        ""
                );


        if (username.isEmpty()
                || email.isEmpty()
                || password.isEmpty()) {

            return error(
                    "Username, email and password are required."
            );
        }


        Optional<User> usernameOwner =
                userRepository.findByUsername(
                        username
                );


        Optional<User> emailOwner =
                userRepository.findByEmail(
                        email
                );


        if (usernameOwner.isPresent()) {

            User existing =
                    usernameOwner.get();


            if (emailOwner.isEmpty()
                    || !existing.getId().equals(
                    emailOwner.get().getId()
            )) {

                return error(
                        "Username already exists."
                );
            }
        }


        User user;


        if (emailOwner.isPresent()) {

            user =
                    emailOwner.get();


            if (user.isVerified()) {

                return error(
                        "Email already registered."
                );
            }

        } else {

            user =
                    new User();
        }


        String verificationCode =
                emailService.generateCode();


        user.setUsername(
                username
        );

        user.setEmail(
                email
        );

        user.setPassword(
                passwordEncoder.encode(
                        password
                )
        );

        user.setVerificationCode(
                verificationCode
        );

        user.setVerified(
                false
        );


        if (user.getFriendCode() == null
                || user.getFriendCode().isBlank()) {

            user.setFriendCode(
                    generateFriendCode()
            );
        }


        userRepository.save(
                user
        );


        emailService.sendVerificationCode(
                email,
                verificationCode
        );


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );response.put(
                "message",
                "Verification code sent."
        );

        response.put(
                "email",
                email
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody Map<String, String> request
    ) {

        String email =
                request.getOrDefault(
                        "email",
                        ""
                ).trim();

        String code =
                request.getOrDefault(
                        "code",
                        ""
                ).trim();


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return error(
                    "User not found."
            );
        }


        User user =
                optionalUser.get();


        if (user.getVerificationCode() == null
                || !user.getVerificationCode()
                .equals(code)) {

            return error(
                    "Wrong verification code."
            );
        }


        user.setVerified(
                true
        );


        // Verification code ကို database ထဲမှာထားမယ်
        userRepository.save(
                user
        );


        return success(
                "Email verified successfully."
        );
    }


    // =====================================================
    // RESEND CODE
    // =====================================================

    @PostMapping("/resend")
    public ResponseEntity<?> resend(
            @RequestBody Map<String, String> request
    ) {

        String email =
                request.getOrDefault(
                        "email",
                        ""
                ).trim();


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return error(
                    "User not found."
            );
        }


        User user =
                optionalUser.get();


        String verificationCode =
                emailService.generateCode();


        user.setVerificationCode(
                verificationCode
        );


        userRepository.save(
                user
        );


        emailService.sendVerificationCode(
                email,
                verificationCode
        );


        return success(
                "Verification code resent."
        );
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {

        String email =
                request.getOrDefault(
                        "email",
                        ""
                ).trim();

        String password =
                request.getOrDefault(
                        "password",
                        ""
                );


        if (email.isEmpty()
                || password.isEmpty()) {

            return error(
                    "Email and password are required."
            );
        }


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return error(
                    "Email not found."
            );
        }


        User user =
                optionalUser.get();


        if (!user.isVerified()) {

            return error(
                    "Please verify your email first."
            );
        }if (!passwordEncoder.matches(
                password,
                user.getPassword()
        )) {

            return error(
                    "Wrong password."
            );
        }


        if (user.getFriendCode() == null
                || user.getFriendCode().isBlank()) {

            user.setFriendCode(
                    generateFriendCode()
            );

            user =
                    userRepository.save(
                            user
                    );
        }


        // ==========================================
        // MOST IMPORTANT PART
        // LOGIN USER DATABASE ID SAVE IN SESSION
        // ==========================================

        session.setAttribute(
                "userId",
                user.getId()
        );


        session.setAttribute(
                "username",
                user.getUsername()
        );


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );

        response.put(
                "id",
                user.getId()
        );

        response.put(
                "username",
                user.getUsername()
        );

        response.put(
                "email",
                user.getEmail()
        );

        response.put(
                "friendCode",
                user.getFriendCode()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // CHECK LOGIN SESSION
    // =====================================================

    @GetMapping("/session")
    public ResponseEntity<?> session(
            HttpSession session
    ) {

        Long userId =
                (Long) session.getAttribute(
                        "userId"
                );


        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "Not logged in."
                            )
                    );
        }


        Optional<User> optionalUser =
                userRepository.findById(
                        userId
                );


        if (optionalUser.isEmpty()) {

            session.invalidate();

            return ResponseEntity
                    .status(401)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "User not found."
                            )
                    );
        }


        User user =
                optionalUser.get();


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );

        response.put(
                "id",
                user.getId()
        );

        response.put(
                "username",
                user.getUsername()
        );

        response.put(
                "email",
                user.getEmail()
        );

        response.put(
                "friendCode",
                user.getFriendCode()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // LOGOUT
    // =====================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpSession session
    ) {

        session.invalidate();

        return success(
                "Logged out."
        );
    }


// =====================================================
// FRIEND CODE
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
    // RESPONSES
    // =====================================================

    private ResponseEntity<Map<String, Object>>
    success(String message) {

        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );

        response.put(
                "message",
                message
        );


        return ResponseEntity.ok(
                response
        );
    }


    private ResponseEntity<Map<String, Object>>
    error(String message) {

        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                false
        );

        response.put(
                "message",
                message
        );


        return ResponseEntity
                .badRequest()
                .body(response);
    }
}