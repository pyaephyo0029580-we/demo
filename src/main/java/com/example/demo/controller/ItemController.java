package com.example.demo.controller;

import com.example.demo.entity.Item;
import com.example.demo.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    // ၁။ Shop အတွက် Item အားလုံးကို ပေးမယ့် API
    @GetMapping("/shop")
    public List<Item> getShopItems() {
        return itemRepository.findAll();
    }

    // ၂။ Inventory အတွက် Default Item တွေကို ပေးမယ့် API
    @GetMapping("/inventory")
    public List<Item> getInventoryItems() {
        return itemRepository.findByIsDefaultTrue();
    }
}