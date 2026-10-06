package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // 1. Home Page (http://localhost:8081/)
    @GetMapping("/")
    public String index() {
        return "forward:/homepage.html";
    }

    // 2. Signup Page
    @GetMapping("/signup")
    public String signupPage() {
        return "redirect:/signup.html";
    }

    // 3. Login Page
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/login.html";
    }

    // 4. Homepage
    @GetMapping("/home")
    public String home() {
        return "redirect:/homepage.html";
    }

    // 5. Friends Page
    @GetMapping("/friends")
    public String friends() {
        return "redirect:/friends.html";
    }

    // 6. Controller Page
    @GetMapping("/controller")
    public String controller() {
        return "controller";
    }

    // 7. Play/Game Page (Player.html သို့ သွားရန်)
    @GetMapping("/player.html")
    public String playerPage() {
        return "forward:/player.html";
    }

    // 8. Game Page (URL အတို /game ဖြင့် Player.html သို့ သွားရန်)
    @GetMapping("/game")
    public String gamePage() {
        return "forward:/player.html";
    }
}