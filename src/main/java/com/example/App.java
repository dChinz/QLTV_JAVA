package com.example;

import com.example.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("login"), 480, 600);
        stage.setTitle("Hệ Thống Quản Lý Thư Viện");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        Parent root = loadFXML(fxml);
        scene.setRoot(root);
        if ("main".equals(fxml)) {
            primaryStage.setWidth(1280);
            primaryStage.setHeight(800);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
        } else if ("login".equals(fxml)) {
            primaryStage.setWidth(480);
            primaryStage.setHeight(600);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void stop() {
        DatabaseConfig.closeConnection();
    }

    public static void main(String[] args) {
        launch();
    }
}
