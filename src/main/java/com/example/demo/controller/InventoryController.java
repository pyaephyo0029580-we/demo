package com.example.demo.controller;

import com.example.demo.entity.Item;
import com.example.demo.entity.UserInventory;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private UserInventoryRepository userInventoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    // ⭐ User ဝယ်ထားတဲ့ Item တွေကို JSON အနေနဲ့ ပြန်ပေးမယ့် API
    @GetMapping
    public List<Map<String, Object>> getUserInventory(@RequestParam String username) {

        List<UserInventory> inventoryList = userInventoryRepository.findByUsername(username);
        List<Map<String, Object>> result = new ArrayList<>();

        for (UserInventory inv : inventoryList) {
            Optional<Item> itemOpt = itemRepository.findById(inv.getItemId());

            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId());
                map.put("name", item.getName());
                map.put("category", item.getCategory());
                map.put("imagePath", item.getImagePath());
                map.put("price", item.getPrice());
                map.put("equipped", inv.isEquipped());
                result.add(map);
            }
        }

        return result;
    }
}