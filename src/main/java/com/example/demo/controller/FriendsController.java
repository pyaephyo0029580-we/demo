package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class FriendsController {

    @FXML
    private Label friendStatusLabel;

    private String userEmail;

    public void setUserEmail(String email) {
        this.userEmail = email;
        System.out.println("Friends page loaded for user: " + userEmail);
        if (friendStatusLabel != null) {
            friendStatusLabel.setText("Logged in as: " + userEmail);
        }
    }

    @FXML
    public void initialize() {
        // Friends page စပွင့်လာသည့်အခါ လုပ်ဆောင်ရန် နေရာ
    }
}