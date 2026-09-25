package com.example.demo.controller;

import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * InventoryController - Browser Launcher
 *
 * JavaFX FXML ကို လုံးဝ မသုံးတော့ဘူး။
 * Browser မှာ inventory.html ကို ဖွင့်ပေးတယ်။
 */
@Component
public class InventoryController {

    private static final String BASE_URL = "http://localhost:8081";

    // =========================================================
    // Helper Methods
    // =========================================================
    private String encode(String text) {
        if (text == null) return "";
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private void openBrowser(String path) {
        try {
            String url = BASE_URL + path;
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("win")) {
                rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                rt.exec(new String[]{"open", url});
            } else {
                rt.exec(new String[]{"xdg-open", url});
            }

            System.out.println("🌐 Opened: " + url);

        } catch (Exception e) {
            System.err.println("❌ Browser ဖွင့်လို့မရပါ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // Show Method - Browser မှာ inventory.html ဖွင့်ရန်
    // =========================================================
    public void show() {
        show(null, null);
    }

    public void show(String roomCode, String username) {
        if (roomCode == null) {
            roomCode = "ROOM" + (int)(Math.random() * 9000 + 1000);
        }
        if (username == null) {
            username = "Player";
        }

        String path = "/inventory.html?room=" + encode(roomCode) +
                "&user=" + encode(username);

        System.out.println("🎨 Opening Inventory: " + path);
        openBrowser(path);
    }

    // =========================================================
    // Optional - Room Code နဲ့ User ကို သတ်မှတ်ပြီး ဖွင့်ရန်
    // =========================================================
    public void showWithUser(String roomCode, String userEmail, int coins) {
        if (roomCode == null) {
            roomCode = "ROOM" + (int)(Math.random() * 9000 + 1000);
        }

        String path = "/inventory.html?room=" + encode(roomCode) +
                "&user=" + encode(userEmail) +
                "&coins=" + coins;

        System.out.println("🎨 Opening Inventory: " + path);
        openBrowser(path);
    }
}