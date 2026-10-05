package com.example.demo.controller.repository;

import com.example.demo.model.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    // Category အလိုက် (HAT သို့မဟုတ် SKIN) ပစ္စည်းများကို ရှာဖွေရန်
    List<ShopItem> findByCategory(String category);
}