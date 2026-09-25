package com.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DemoApplication extends Application {

    private static ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(DemoApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        // JavaFX UI - FXML မလိုဘဲ ကိုယ်တိုင် ဆောက်မည်
        Label title = new Label("AMONG US");
        title.setStyle("-fx-text-fill: #8ce0dc; -fx-font-size: 42px; -fx-font-weight: bold;");

        Label info = new Label("Server: http://localhost:8081");
        info.setStyle("-fx-text-fill: #5a7a7a; -fx-font-size: 14px;");

        Button openBrowser = new Button("🌐 Open in Browser");
        openBrowser.setStyle("-fx-background-color: #2a7d78; -fx-text-fill: white; " +
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
                "-fx-cursor: hand;");
        openBrowser.setOnAction(e -> openInBrowser("http://localhost:8081/homepage.html"));

        Button exitBtn = new Button("✕ Exit");
        exitBtn.setStyle("-fx-background-color: #ff5555; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8px 20px; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        VBox root = new VBox(20, title, info, openBrowser, exitBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: radial-gradient(circle, #1a2f38, #0a1317);");

        Scene scene = new Scene(root, 500, 400);

        primaryStage.setTitle("Among Us - Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("===========================================");
        System.out.println("🎮 Server Started!");
        System.out.println("📱 Open: http://localhost:8081/homepage.html");
        System.out.println("===========================================");
    }

    private void openInBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            if (os.contains("win")) {
                rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                rt.exec(new String[]{"open", url});
            } else {
                rt.exec(new String[]{"xdg-open", url});
            }
            System.out.println("🌐 Opened: " + url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}