package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DemoApplication extends Application {

    private static ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Spring Boot ကို စတင် Run ပေးခြင်း[cite: 6]
        springContext = SpringApplication.run(DemoApplication.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ဖိုင်တည်နေရာ အမှန် (`src/main/resources/com/example/demo/homepage.fxml`) ကို ခေါ်ယူရန်[cite: 5, 6]
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/homepage.fxml"));

        // Spring @Component တွေ အလုပ်လုပ်စေရန် Controller Factory ချိတ်ဆက်ပေးခြင်း[cite: 6]
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        primaryStage.setTitle("My Online Game - Home Page");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static FXMLLoader createFXMLLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(DemoApplication.class.getResource(fxmlPath));
        loader.setControllerFactory(springContext::getBean);
        return loader;
    }
}