package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;

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


    // =========================================
    // CONSTRUCTOR
    // =========================================

    public AuthApiController(
            UserRepository userRepository,
            EmailService emailService
    ) {

        this.userRepository = userRepository;
        this.emailService = emailService;
    }


    // =========================================
    // SIGN UP
    // =========================================

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
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


        // -------------------------------------
        // Empty check
        // -------------------------------------

        if (username.isEmpty()
                || email.isEmpty()
                || password.isEmpty()) {

            return bad(
                    "Please fill all fields."
            );
        }


        // -------------------------------------
        // Email check
        // -------------------------------------

        if (!email.contains("@")
                || !email.contains(".")) {

            return bad(
                    "Please enter a valid email."
            );
        }


        // -------------------------------------
        // Password check
        // -------------------------------------

        if (password.length() < 6) {

            return bad(
                    "Password must be at least 6 characters."
            );
        }


        // -------------------------------------
        // Find email
        // -------------------------------------

        Optional<User> existingEmail =
                userRepository.findByEmail(email);


        /*
         * Email account ရှိပြီး
         * verified ဖြစ်ပြီးသားဆို signup
         * ထပ်လုပ်ခွင့်မပေးပါ။
         */

        if (existingEmail.isPresent()
                && existingEmail.get().isVerified()) {

            return bad(
                    "This email is already registered."
            );
        }


        // -------------------------------------
        // Username check
        // -------------------------------------

        Optional<User> usernameUser =
                userRepository.findByUsername(
                        username
                );


        if (usernameUser.isPresent()) {

            /*
             * Existing unverified email account
             * တစ်ခုတည်းဖြစ်ရင် update လုပ်လို့ရတယ်။
             */

            if (existingEmail.isEmpty()
                    || !usernameUser
                    .get()
                    .getId()
                    .equals(
                            existingEmail
                                    .get()
                                    .getId()
                    )) {return bad(
                    "Username already exists."
            );
            }
        }


        // -------------------------------------
        // Existing unverified user / new user
        // -------------------------------------

        User user;

        if (existingEmail.isPresent()) {

            user = existingEmail.get();

        } else {

            user = new User();
        }


        // -------------------------------------
        // Generate verification code
        // -------------------------------------

        String verificationCode =
                emailService.generateCode();


        // -------------------------------------
        // Encrypt password
        // -------------------------------------

        String hashedPassword =
                passwordEncoder.encode(
                        password
                );


        // -------------------------------------
        // Set user information
        // -------------------------------------

        user.setUsername(
                username
        );

        user.setEmail(
                email
        );

        user.setPassword(
                hashedPassword
        );

        user.setVerificationCode(
                verificationCode
        );

        user.setVerified(
                false
        );


        // -------------------------------------
        // Friend Code
        // -------------------------------------

        if (user.getFriendCode() == null
                || user.getFriendCode().isBlank()) {

            user.setFriendCode(
                    generateFriendCode()
            );
        }


        // -------------------------------------
        // Save user to database
        // -------------------------------------

        try {

            userRepository.save(user);

        } catch (Exception e) {

            e.printStackTrace();

            return bad(
                    "Cannot save account to database."
            );
        }


        // -------------------------------------
        // Send verification email
        // -------------------------------------

        try {

            emailService.sendVerificationCode(
                    email,
                    verificationCode
            );

        } catch (Exception e) {

            e.printStackTrace();

            /*
             * User ကို database မှာ save
             * လုပ်ပြီးသားဖြစ်နေမယ်။
             */

            return bad(
                    "Account saved but verification email could not be sent."
            );
        }


        System.out.println(
                "================================"
        );

        System.out.println(
                "New signup email: " + email
        );

        System.out.println(
                "Verification code: "
                        + verificationCode
        );

        System.out.println(
                "================================"
        );


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Verification code sent!"
        );

        response.put(
                "email",
                email
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================
    // VERIFY EMAIL
    // =========================================

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
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
                ).trim();// -------------------------------------
        // Empty check
        // -------------------------------------

        if (email.isEmpty()
                || code.isEmpty()) {

            return bad(
                    "Email and verification code are required."
            );
        }


        // -------------------------------------
        // Find user
        // -------------------------------------

        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return bad(
                    "User not found."
            );
        }


        User user =
                optionalUser.get();


        // -------------------------------------
        // Already verified
        // -------------------------------------

        if (user.isVerified()) {

            return ok(
                    "Email is already verified."
            );
        }


        // -------------------------------------
        // Get database verification code
        // -------------------------------------

        String savedCode =
                user.getVerificationCode();


        if (savedCode == null
                || savedCode.isBlank()) {

            return bad(
                    "Verification code not found."
            );
        }


        // -------------------------------------
        // Compare verification code
        // -------------------------------------

        if (!savedCode.equals(code)) {

            return bad(
                    "Invalid verification code."
            );
        }


        // -------------------------------------
        // Verification success
        // -------------------------------------

        user.setVerified(
                true
        );


        /*
         * IMPORTANT:
         *
         * ဒီမှာ
         *
         * user.setVerificationCode(null);
         *
         * မလုပ်ထားပါ။
         *
         * ဒါကြောင့် Verify လုပ်ပြီးသွားလည်း
         * verification_code က database
         * ထဲမှာ ဆက်ရှိနေပါမယ်။
         */


        try {

            userRepository.save(
                    user
            );

        } catch (Exception e) {

            e.printStackTrace();

            return bad(
                    "Cannot update verification status."
            );
        }


        System.out.println(
                "================================"
        );

        System.out.println(
                "Email verified: "
                        + user.getEmail()
        );

        System.out.println(
                "Saved verification code: "
                        + user.getVerificationCode()
        );

        System.out.println(
                "Verified: "
                        + user.isVerified()
        );

        System.out.println(
                "================================"
        );


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Email verified successfully!"
        );

        response.put(
                "email",
                user.getEmail()
        );

        response.put(
                "username",
                user.getUsername()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================
    // RESEND VERIFICATION CODE
    // =========================================

    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resend(
            @RequestBody Map<String, String> request
    ) {

        String email =
                request.getOrDefault(
                        "email",
                        ""
                ).trim();


        if (email.isEmpty()) {

            return bad(
                    "Email is required."
            );
        }Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return bad(
                    "User not found."
            );
        }


        User user =
                optionalUser.get();


        if (user.isVerified()) {

            return bad(
                    "Email is already verified."
            );
        }


        // -------------------------------------
        // Generate new code
        // -------------------------------------

        String newCode =
                emailService.generateCode();


        user.setVerificationCode(
                newCode
        );

        user.setVerified(
                false
        );


        // -------------------------------------
        // Save new code
        // -------------------------------------

        try {

            userRepository.save(
                    user
            );

        } catch (Exception e) {

            e.printStackTrace();

            return bad(
                    "Cannot save new verification code."
            );
        }


        // -------------------------------------
        // Send email
        // -------------------------------------

        try {

            emailService.sendVerificationCode(
                    email,
                    newCode
            );

        } catch (Exception e) {

            e.printStackTrace();

            return bad(
                    "Cannot send verification code."
            );
        }


        System.out.println(
                "New verification code: "
                        + newCode
        );


        return ok(
                "New verification code sent."
        );
    }


    // =========================================
    // LOGIN
    // =========================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request
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


        // -------------------------------------
        // Empty check
        // -------------------------------------

        if (email.isEmpty()
                || password.isEmpty()) {

            return bad(
                    "Please enter email and password."
            );
        }


        // -------------------------------------
        // Find account
        // -------------------------------------

        Optional<User> optionalUser =
                userRepository.findByEmail(
                        email
                );


        if (optionalUser.isEmpty()) {

            return bad(
                    "Account not found."
            );
        }


        User user =
                optionalUser.get();


        // -------------------------------------
        // Verified check
        // -------------------------------------

        if (!user.isVerified()) {

            return bad(
                    "Please verify your email first."
            );
        }


        // -------------------------------------
        // Password exists?
        // -------------------------------------

        String savedPassword =
                user.getPassword();


        if (savedPassword == null
                || savedPassword.isBlank()) {

            return bad(
                    "Password not found."
            );
        }


        // -------------------------------------
        // BCrypt password check
        // -------------------------------------

        boolean passwordCorrect;

        try {

            passwordCorrect =
                    passwordEncoder.matches(
                            password,
                            savedPassword
                    );} catch (IllegalArgumentException e) {

            passwordCorrect = false;
        }


        if (!passwordCorrect) {

            return bad(
                    "Incorrect password."
            );
        }


        // -------------------------------------
        // Make sure friend code exists
        // -------------------------------------

        if (user.getFriendCode() == null
                || user.getFriendCode().isBlank()) {

            user.setFriendCode(
                    generateFriendCode()
            );

            userRepository.save(
                    user
            );
        }


        // -------------------------------------
        // Login success response
        // -------------------------------------

        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Login successful!"
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
                "verificationCode",
                user.getVerificationCode()
        );


        System.out.println(
                "Login successful: "
                        + user.getUsername()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================
    // GENERATE UNIQUE FRIEND CODE
    // =========================================

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
                userRepository.existsByFriendCode(
                        code
                )
        );


        return code;
    }


    // =========================================
    // SUCCESS RESPONSE
    // =========================================

    private ResponseEntity<Map<String, Object>> ok(
            String message
    ) {

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


    // =========================================
    // ERROR RESPONSE
    // =========================================

    private ResponseEntity<Map<String, Object>> bad(
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
                .body(
                        response
                );
    }
}