package com.example.demo.controller.Homepage;

import com.example.demo.model.ShopItem;
import com.example.demo.controller.repository.ShopItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
@Controller
public class ShopController {

    @Autowired
    private ShopItemRepository shopItemRepository;

    // ❌ ဒီအပိုင်းကို ဖျက်ပစ်လိုက်ပါ (သို့မဟုတ် comment လုပ်ပါ)
    /*
    @GetMapping({"/", "/homepage", "/homepage.html"})
    public String homePage() {
        return "redirect:/homepage.html";
    }
    */

    // 2. Inventory / Shop စာမျက်နှာအတွက်
    @GetMapping({"/inventory", "/shop", "/inventory.html"})
    public String showInventory(@RequestParam(name = "category", defaultValue = "HAT") String category, Model model) {
        List<ShopItem> items = shopItemRepository.findByCategory(category);
        model.addAttribute("items", items);
        model.addAttribute("category", category);

        return "inventory";
    }

    // 3. API အတွက်
    @GetMapping("/shop-api")
    @ResponseBody
    public List<ShopItem> getShopItems(@RequestParam(name = "category", defaultValue = "HAT") String category) {
        return shopItemRepository.findByCategory(category);
    }
}