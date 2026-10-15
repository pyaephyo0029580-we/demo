package com.example.demo.model; // သင့်ပရောဂျက်ရဲ့ package နာမည်အတိုင်း ထည့်ပါ
public class Player {
    private String id;
    private String username;
    private String color;
    private String role;
    private int x;
    private int y;
    // WebSocket Session ID (သို့မဟုတ် Unique ID) private String username;  // ကစားသမားနာမည် (ခေါင်းပေါ်မှာပေါ်ရန်) private String color;     // ကစားသမားအရောင် (တစ်ကောင်နဲ့တစ်ကောင် မတူစေရန်) private String role;      // "killer" သို့မဟုတ် "crewmate" private int x;            // Map ပေါ်ရှိ X တည်နေရာ private int y;            // Map ပေါ်ရှိ Y တည်နေရာ
    // Constructor (Default)
    public Player() {
    }

    // Constructor (Parameters တွေနဲ့)
    public Player(String id, String username, String color, String role, int x, int y) {
        this.id = id;
        this.username = username;
        this.color = color;
        this.role = role;
        this.x = x;
        this.y = y;
    }

    // Getters and Setters (အချက်အလက်တွေကို ယူသုံးရန်နှင့် ပြောင်းလဲရန်)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}