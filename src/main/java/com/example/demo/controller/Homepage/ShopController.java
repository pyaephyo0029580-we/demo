package com.example.demo.controller.Homepage;

import com.example.demo.model.ShopItem;
import com.example.demo.controller.repository.ShopItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class ShopController {

    @Autowired
    private ShopItemRepository shopItemRepository;


    // =========================================================
    // Shop API — database ကနေ items ပြ
    // =========================================================

    @GetMapping("/shop-api")
    public List<ShopItem> getShopItems(
            @RequestParam(name = "category", defaultValue = "HAT") String category) {

        return shopItemRepository.findByCategory(category);
    }
}