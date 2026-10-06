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
public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom random =
            new SecureRandom();


    public AuthController(
            UserRepository userRepository,
            EmailService emailService
    ) {

        this.userRepository =
                userRepository;

        this.emailService =
                emailService;
    }


    // =====================================================
    // SIGN UP
    // =====================================================

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @RequestBody Map<String, String> request
    ) {

        String username =
                clean(request.get("username"));

        String email =
                clean(request.get("email"));

        String password =
                request.get("password");


        if (
                username == null ||
                        email == null ||
                        password == null
        ) {

            return badRequest(
                    "Please fill in all fields."
            );
        }


        if (username.length() < 3) {

            return badRequest(
                    "Username must be at least 3 characters."
            );
        }


        if (!isValidEmail(email)) {

            return badRequest(
                    "Please enter a valid email address."
            );
        }


        // STRONG PASSWORD CHECK
        if (!isStrongPassword(password)) {

            return badRequest(
                    "Password must be at least 8 characters and include uppercase, lowercase, number and special character."
            );
        }


        Optional<User> usernameOwner =
                userRepository.findByUsername(
                        username
                );


        if (usernameOwner.isPresent()) {

            User existingUsername =
                    usernameOwner.get();


            if (
                    existingUsername.getEmail() == null ||
                            !existingUsername
                                    .getEmail()
                                    .equalsIgnoreCase(email)
            ) {

                return badRequest(
                        "Username is already taken."
                );
            }
        }


        Optional<User> existingOptional =
                userRepository.findByEmail(
                        email
                );


        User user;


        if (existingOptional.isPresent()) {

            user =
                    existingOptional.get();


            if (user.isVerified()) {

                return badRequest(
                        "This email is already registered."
                );
            }


            user.setUsername(
                    username
            );

        } else {

            user = new User();

            user.setUsername(
                    username
            );

            user.setEmail(
                    email
            );

            user.setVerified(
                    false
            );
        }


        String verificationCode =
                emailService.generateCode();


        user.setVerificationCode(
                verificationCode
        );


        user.setPassword(
                passwordEncoder.encode(
                        password
                )
        );if (
                user.getFriendCode() == null ||
                        user.getFriendCode()
                                .isBlank()
        ) {

            user.setFriendCode(
                    generateUniqueFriendCode()
            );
        }


        User savedUser =
                userRepository.save(
                        user
                );


        try {

            emailService.sendVerificationCode(
                    email,
                    verificationCode
            );


        } catch (Exception e) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "emailSent",
                    false
            );

            response.put(
                    "message",
                    "Account was created, but verification email could not be sent. Please try Resend Code."
            );

            response.put(
                    "email",
                    email
            );

            return ResponseEntity
                    .status(500)
                    .body(response);
        }


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "emailSent",
                true
        );

        response.put(
                "message",
                "Verification code sent to your email."
        );

        response.put(
                "id",
                savedUser.getId()
        );

        response.put(
                "email",
                savedUser.getEmail()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, String> request
    ) {

        String email =
                clean(request.get("email"));

        String code =
                clean(request.get("code"));


        if (
                email == null ||
                        code == null
        ) {

            return badRequest(
                    "Email and verification code are required."
            );
        }


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return badRequest(
                    "Account not found."
            );
        }


        User user =
                optionalUser.get();


        if (user.isVerified()) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Email is already verified."
            );

            return ResponseEntity.ok(
                    response
            );
        }


        if (
                user.getVerificationCode() == null ||
                        !user.getVerificationCode()
                                .equals(code)
        ) {

            return badRequest(
                    "Invalid verification code."
            );
        }


        user.setVerified(
                true
        );


        /*
         * Keep verification_code in database
         * as requested.
         *
         * DO NOT:
         * user.setVerificationCode(null);
         */


        userRepository.save(
                user
        );


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Email verified successfully."
        );return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // RESEND VERIFICATION CODE
    // =====================================================

    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resend(
            @RequestBody Map<String, String> request
    ) {

        String email =
                clean(request.get("email"));


        if (email == null) {

            return badRequest(
                    "Email is required."
            );
        }


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return badRequest(
                    "Account not found."
            );
        }


        User user =
                optionalUser.get();


        if (user.isVerified()) {

            return badRequest(
                    "Email is already verified."
            );
        }


        String newCode =
                emailService.generateCode();


        user.setVerificationCode(
                newCode
        );


        userRepository.save(
                user
        );


        try {

            emailService.sendVerificationCode(
                    email,
                    newCode
            );


        } catch (Exception e) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Could not send verification email."
            );


            return ResponseEntity
                    .status(500)
                    .body(response);
        }


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "A new verification code has been sent."
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {

        String email =
                clean(request.get("email"));

        String password =
                request.get("password");


        if (
                email == null ||
                        password == null
        ) {

            return badRequest(
                    "Email and password are required."
            );
        }


        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return badRequest(
                    "Invalid email or password."
            );
        }


        User user =
                optionalUser.get();


        if (!user.isVerified()) {

            return badRequest(
                    "Please verify your email first."
            );
        }


        if (
                user.getPassword() == null ||
                        !passwordEncoder.matches(
                                password,
                                user.getPassword()
                        )
        ) {

            return badRequest(
                    "Invalid email or password."
            );
        }


        if (
                user.getFriendCode() == null ||
                        user.getFriendCode()
                                .isBlank()
        ) {

            user.setFriendCode(
                    generateUniqueFriendCode()
            );

            userRepository.save(
                    user
            );
        }


        session.setAttribute(
                "userId",
                user.getId()
        );session.setAttribute(
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
                "message",
                "Login successful."
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
    // CURRENT SESSION
    // =====================================================

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(
            HttpSession session
    ) {

        Object userId =
                session.getAttribute(
                        "userId"
                );


        if (userId == null) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "loggedIn",
                    false
            );


            return ResponseEntity
                    .status(401)
                    .body(response);
        }


        Long id =
                ((Number) userId)
                        .longValue();


        Optional<User> optionalUser =
                userRepository.findById(
                        id
                );


        if (optionalUser.isEmpty()) {

            session.invalidate();

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "loggedIn",
                    false
            );


            return ResponseEntity
                    .status(401)
                    .body(response);
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
                "loggedIn",
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
    public ResponseEntity<Map<String, Object>> logout(
            HttpSession session
    ) {

        session.invalidate();


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Logged out successfully."
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // STRONG PASSWORD
    // =====================================================

    private boolean isStrongPassword(
            String password
    ) {

        if (password == null) {
            return false;
        }


        if (password.length() < 8) {
            return false;
        }boolean hasUpper =
                password.matches(
                        ".*[A-Z].*"
                );

        boolean hasLower =
                password.matches(
                        ".*[a-z].*"
                );

        boolean hasNumber =
                password.matches(
                        ".*\\d.*"
                );

        boolean hasSpecial =
                password.matches(
                        ".*[@#$!%*?&].*"
                );


        return (
                hasUpper &&
                        hasLower &&
                        hasNumber &&
                        hasSpecial
        );
    }


    // =====================================================
    // EMAIL CHECK
    // =====================================================

    private boolean isValidEmail(
            String email
    ) {

        return (
                email != null &&
                        email.matches(
                                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                        )
        );
    }


    // =====================================================
    // UNIQUE FRIEND CODE
    // =====================================================

    private String generateUniqueFriendCode() {

        String code;


        do {

            int number =
                    100000 +
                            random.nextInt(
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
    // CLEAN STRING
    // =====================================================

    private String clean(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String result =
                value.trim();


        if (result.isEmpty()) {
            return null;
        }


        return result;
    }


    // =====================================================
    // ERROR RESPONSE
    // =====================================================

    private ResponseEntity<Map<String, Object>> badRequest(
            String message
    ) {

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