package com.example.demo.controller;

import com.example.demo.DemoApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class homepage {

    @FXML
    private Label coinLabel;

    private String userEmail;
    private int coins = 0;

    @FXML
    public void initialize() {
        coins = 0;
        updateCoinLabel();
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        System.out.println("Logged in user = " + userEmail);
    }

    public String getUserEmail() {
        return userEmail;
    }

    @FXML
    private void openPlay() {
        showMessage("PLAY", "Create Room / Join Room");
    }

    @FXML
    private void openInventory() {
        showMessage("INVENTORY", "Inventory page will open here.");
    }

    @FXML
    private void openShop() {
        showMessage("SHOP", "Shop page will open here.");
    }

    @FXML
    private void openMyAccount() {
        String email = (userEmail == null) ? "Unknown" : userEmail;
        showMessage("MY ACCOUNT", "Email: " + email);
    }

    @FXML
    private void openSetting() {
        try {
            FXMLLoader loader = DemoApplication.createFXMLLoader("/fxml/setting.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("ERROR", "Cannot open Settings page.");
        }
    }

    @FXML
    private void openFriends() {
        try {
            FXMLLoader loader = DemoApplication.createFXMLLoader("/fxml/friend.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("MyGaming - Friends");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("ERROR", "Cannot open Friends page.");
        }
    }

    private void updateCoinLabel() {
        if (coinLabel != null) {
            coinLabel.setText(String.valueOf(coins));
        }
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}