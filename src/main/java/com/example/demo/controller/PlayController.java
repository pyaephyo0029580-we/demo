package com.example.demo.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.stereotype.Component; // ဒီ import ကို ထည့်ပါ

import java.io.IOException;

@Component // <--- ဒီနေရာမှာ @Component ထည့်ပေးပါ
public class PlayController {

    @FXML
    private void handleCreateRoom(ActionEvent event) {
        // Create Room လုပ်ဆောင်ချက်များ
        System.out.println("Create Room clicked!");
    }

    @FXML
    private void handleJoinRoom(ActionEvent event) {
        // Join Room လုပ်ဆောင်ချက်များ
        System.out.println("Join Room clicked!");
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        try {
            // Home Page (`homepage.fxml`) သို့ ပြန်သွားရန်
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/demo/homepage.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1000, 650);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}