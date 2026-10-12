package com.example.demo.controller;

import com.example.demo.entity.Item;
import com.example.demo.entity.User;
import com.example.demo.entity.UserInventory;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/buy")
public class BuyController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInventoryRepository userInventoryRepository;

    @PostMapping
    public ResponseEntity<?> buyItem(
            @RequestParam String username,
            @RequestParam Long itemId) {

        // ၁။ User ကို ရှာမယ်
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found!");
        }
        User user = userOpt.get();

        // ၂။ Item ကို ရှာမယ်
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Item not found!");
        }
        Item item = itemOpt.get();

        // ၃။ Coin လုံလောက်ရဲ့လား စစ်မယ်
        if (user.getCoins() < item.getPrice()) {
            return ResponseEntity.badRequest().body("Not enough coins!");
        }

        // ၄။ ဝယ်ပြီးသားလား စစ်မယ်
        if (userInventoryRepository.existsByUsernameAndItemId(username, itemId)) {
            return ResponseEntity.badRequest().body("You already own this item!");
        }

        // ၅။ Coin နှုတ်မယ်
        user.setCoins(user.getCoins() - item.getPrice());
        userRepository.save(user);

        // ၆။ Inventory ထဲ ထည့်မယ်
        UserInventory inv = new UserInventory();
        inv.setUsername(username);
        inv.setItemId(itemId);
        inv.setEquipped(false);
        userInventoryRepository.save(inv);

        return ResponseEntity.ok("Purchased successfully!");
    }
}