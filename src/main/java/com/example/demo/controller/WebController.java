//package com.example.demo.controller;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class WebController {
//
//    @GetMapping("/")
//    public String index() {
//
//        return "index";
//    }
//
//    @GetMapping("/controller")
//    public String controller() {
//        return "controller";
//    }
//
//}

package com.example.demo.controller;
import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.GetMapping;
@Controller public class WebController {
    // 1. Home Page သို့ သွားရန် (http://localhost:8081/)
    @GetMapping("/")
    public String index() {
        return "forward:/homepage.html";
    }

    // 2. Controller Page (လိုအပ်ပါက ခေါ်ရန်)
    @GetMapping("/controller")
    public String controller() {
        return "controller";
    }

    // 3. Play Button နှိပ်လိုက်ရင် Map နဲ့ Character ပေါ်မယ့် Game Page သို့ သွားရန်
    @GetMapping("/player.html")
    public String playerPage() {
        return "forward:/player.html";
    }

    // 4. Game Page သို့ URL အတိုနမူနာ (/game) ဖြင့် သွားလိုပါက
    @GetMapping("/game")
    public String gamePage() {
        return "forward:/player.html";
    }}