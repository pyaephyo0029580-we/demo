package com.example.demo.controller;

//package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketEventListener {

    // လက်ရှိ ချိတ်ဆက်နေသူ အရေအတွက်ကို ဘေးကင်းကင်းနဲ့ တိုး/လျော့ လုပ်ပေးမယ့် Variable
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        int count = activeUsers.incrementAndGet(); // တစ်ယောက် ဝင်လာရင် ၁ တိုးမည်
        broadcastActiveUsers(count);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        int count = activeUsers.decrementAndGet(); // တစ်ယောက် ထွက်သွားရင် ၁ လျော့မည်
        if (count < 0) count = 0;
        broadcastActiveUsers(count);
    }

    // ချိတ်ဆက်ထားသူ အရေအတွက်ကို Subscribe လုပ်ထားသူအားလုံးဆီ ပို့ပေးခြင်း
    private void broadcastActiveUsers(int count) {
        messagingTemplate.convertAndSend("/topic/users", count);
        System.out.println("Active Users Count: " + count);
    }
}
