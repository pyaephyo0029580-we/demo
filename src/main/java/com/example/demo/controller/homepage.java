package com.example.demo.controller;

import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Homepage - Browser-based UI Launcher
 * JavaFX FXML ကို လုံးဝ မသုံးတော့ဘူး။
 * ခလုတ်တိုင်းက Browser မှာ HTML page ကို ဖွင့်ပေးတယ်။
 */
@Component
public class homepage {

    private static final String BASE_URL = "http://localhost:8081";
    private String userEmail = "Player";
    private int coins = 0;

    // =========================================================
    // User Info
    // =========================================================
    public void setUserEmail(String email) {
        this.userEmail = email;
        System.out.println("👤 Logged in: " + userEmail);
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    // =========================================================
    // Helper Methods - Encode & Browser Open
    // =========================================================

    /**
     * URL Encode - Exception မရှိဘဲ စာသားကို encode လုပ်ပေးတယ်
     */
    private String encode(String text) {
        if (text == null) return "";
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /**
     * Browser ဖွင့်ရန် Helper Method
     */
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
    // Button Actions
    // =========================================================

    /**
     * PLAY - Game Room ဖန်တီးပြီး game.html ကို ဖွင့်
     */
    public void openPlay() {
        String roomCode = "ROOM" + (int)(Math.random() * 9000 + 1000);
        String path = "/game.html?room=" + roomCode + "&user=" + encode(userEmail);

        System.out.println("🎮 Room created: " + roomCode);
        System.out.println("📢 Share link: " + BASE_URL + "/game.html?room=" + roomCode);

        openBrowser(path);
    }

    /**
     * INVENTORY - inventory.html ကို ဖွင့်
     */
    public void openInventory() {
        String roomCode = "ROOM" + (int)(Math.random() * 9000 + 1000);
        String path = "/inventory.html?room=" + roomCode +
                "&user=" + encode(userEmail) +
                "&coins=" + coins;

        openBrowser(path);
    }

    /**
     * SHOP - shop.html ကို ဖွင့်
     */
    public void openShop() {
        String path = "/shop.html?user=" + encode(userEmail) +
                "&coins=" + coins;
        openBrowser(path);
    }

    /**
     * MY ACCOUNT - account.html ကို ဖွင့်
     */
    public void openMyAccount() {
        String path = "/account.html?user=" + encode(userEmail) +
                "&coins=" + coins;
        openBrowser(path);
    }

    /**
     * SETTING - setting.html ကို ဖွင့်
     */
    public void openSetting() {
        String path = "/setting.html?user=" + encode(userEmail);
        openBrowser(path);
    }

    /**
     * FRIENDS - friends.html ကို ဖွင့်
     */
    public void openFriends() {
        String path = "/friends.html?user=" + encode(userEmail);
        openBrowser(path);
    }

    /**
     * HOMEPAGE - homepage.html ကို ဖွင့်
     */
    public void openHomepage() {
        String path = "/homepage.html?user=" + encode(userEmail) +
                "&coins=" + coins;
        openBrowser(path);
    }
}