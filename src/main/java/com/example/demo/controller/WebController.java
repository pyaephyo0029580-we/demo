package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // Login Page
    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    // Signup Page
    @GetMapping("/signup")
    public String signup() {
        return "redirect:/signup.html";
    }

    // Homepage
    @GetMapping("/home")
    public String home() {
        return "redirect:/homepage.html";
    }

    // Friends Page
    @GetMapping("/friends")
    public String friends() {
        return "redirect:/friends.html";
    }

    // Game Page
    @GetMapping("/game")
    public String game() {
        return "redirect:/game.html";
    }
}