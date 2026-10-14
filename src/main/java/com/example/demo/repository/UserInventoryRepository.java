package com.example.demo.repository;

import com.example.demo.entity.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {

    // User ရဲ့ Item တွေကို ရှာဖို့
    List<UserInventory> findByUsername(String username);

    // User ဒီ Item ဝယ်ပြီးသားလား စစ်ဖို့
    boolean existsByUsernameAndItemId(String username, Long itemId);
}