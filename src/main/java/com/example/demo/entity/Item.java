package com.example.demo.entity;

import jakarta.persistence.*;
// (တကယ်လို့ Error တက်ရင် javax.persistence.* လို့ ပြောင်းရေးပါ။ သင့် Spring Boot version ပေါ်မူတည်ပါတယ်)

@Entity
@Table(name = "shop_items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Item နာမည်
    private String category;    // "HAT", "SKIN", "COLOR"
    private String imagePath;   // ပုံလမ်းကြောင်း
    private int price;          // ဈေးနှုန်း
    private boolean isDefault;  // Inventory မှာ အစကတည်းက ရှိမလား?

    // Getters and Setters (Alt + Insert နှိပ်ပြီး ဆောက်နိုင်ပါတယ်)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}