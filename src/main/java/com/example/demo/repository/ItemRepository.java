package com.example.demo.repository;

import com.example.demo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // Category အလိုက် ရှာဖွေဖို့ (ဥပမာ - "HAT", "SKIN")
    List<Item> findByCategory(String category);

    // Inventory မှာ အစကတည်းက ရှိမယ့် Item တွေအတွက်
    List<Item> findByIsDefaultTrue();
}