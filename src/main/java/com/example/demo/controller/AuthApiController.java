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


        // =========================================
        // CHECK EMAIL
        // =========================================

        Optional<User> emailUser =
                userRepository.findByEmail(
                        email
                );


        if (emailUser.isPresent()
                && emailUser.get().isVerified()) {

            return error(
                    "Email already registered."
            );
        }


        // =========================================
        // CHECK USERNAME
        // =========================================

        Optional<User> usernameUser =
                userRepository.findByUsername(
                        username
                );


        if (usernameUser.isPresent()) {

            if (emailUser.isEmpty()
                    || !usernameUser
                    .get()
                    .getId()
                    .equals(
                            emailUser
                                    .get()
                                    .getId()
                    )) {

                return error(
                        "Username already exists."
                );
            }
        }


        User user;


        // Existing unverified email can signup again
        if (emailUser.isPresent()) {

            user =
                    emailUser.get();

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
                || user.getFriendCode().isBlank()) {user.setFriendCode(
                generateFriendCode()
        );
        }


        // =========================================
        // SAVE DATABASE FIRST
        // =========================================

        user =
                userRepository.save(
                        user
                );


        System.out.println(
                "===================================="
        );

        System.out.println(
                "SIGNUP SUCCESS"
        );

        System.out.println(
                "DATABASE USER ID = "
                        + user.getId()
        );

        System.out.println(
                "USERNAME = "
                        + user.getUsername()
        );

        System.out.println(
                "EMAIL = "
                        + user.getEmail()
        );

        System.out.println(
                "VERIFICATION CODE = "
                        + verificationCode
        );

        System.out.println(
                "===================================="
        );


        // =========================================
        // TRY EMAIL
        // =========================================

        boolean emailSent =
                true;


        try {

            emailService.sendVerificationCode(
                    email,
                    verificationCode
            );

        } catch (Exception e) {

            emailSent =
                    false;


            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "EMAIL SEND FAILED"
            );

            System.out.println(
                    e.getMessage()
            );

            System.out.println(
                    "USE THIS CODE:"
            );

            System.out.println(
                    verificationCode
            );

            System.out.println(
                    "===================================="
            );
        }


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );


        response.put(
                "email",
                email
        );


        response.put(
                "id",
                user.getId()
        );


        response.put(
                "emailSent",
                emailSent
        );


        if (emailSent) {

            response.put(
                    "message",
                    "Verification code sent to your email."
            );

        } else {

            response.put(
                    "message",
                    "Account created. Check IntelliJ Console for verification code."
            );
        }


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


        if (email.isEmpty()
                || code.isEmpty()) {

            return error(
                    "Email and verification code are required."
            );
        }


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
                || !user
                .getVerificationCode()
                .equals(code)) {return error(
                "Wrong verification code."
        );
        }


        user.setVerified(
                true
        );


        // Keep verification code in DB
        userRepository.save(
                user
        );


        System.out.println(
                "EMAIL VERIFIED: "
                        + email
        );


        return success(
                "Email verified successfully."
        );
    }


    // =====================================================
    // RESEND VERIFICATION CODE
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


        if (email.isEmpty()) {

            return error(
                    "Email is required."
            );
        }


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


        System.out.println(
                "NEW VERIFICATION CODE = "
                        + verificationCode
        );


        boolean emailSent =
                true;


        try {

            emailService.sendVerificationCode(
                    email,
                    verificationCode
            );

        } catch (Exception e) {

            emailSent =
                    false;


            System.out.println(
                    "EMAIL SEND FAILED"
            );


            System.out.println(
                    "USE CODE = "
                            + verificationCode
            );
        }


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );


        response.put(
                "emailSent",
                emailSent
        );


        if (emailSent) {

            response.put(
                    "message",
                    "Verification code sent."
            );

        } else {

            response.put(
                    "message",
                    "Check IntelliJ Console for verification code."
            );
        }


        return ResponseEntity.ok(
                response
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
        }if (user.getPassword() == null
                || user.getPassword().isBlank()) {

            return error(
                    "Password not found."
            );
        }


        if (!passwordEncoder.matches(
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


        // =========================================
        // SAVE LOGIN USER ID IN SESSION
        // =========================================

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


        response.put(
                "message",
                "Login successful."
        );


        System.out.println(
                "===================================="
        );

        System.out.println(
                "LOGIN SUCCESS"
        );

        System.out.println(
                "USER ID = "
                        + user.getId()
        );

        System.out.println(
                "USERNAME = "
                        + user.getUsername()
        );

        System.out.println(
                "===================================="
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // CURRENT SESSION
    // =====================================================

    @GetMapping("/session")
    public ResponseEntity<?> getSession(
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
        );response.put(
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
    // GENERATE FRIEND CODE
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
    // SUCCESS RESPONSE
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


    // =====================================================
    // ERROR RESPONSE
    // =====================================================

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