package com.gcodebuilder.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private void showFaceBuilder(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("faceBuilder.fxml"));
        primaryStage.setTitle("GCode Facing Program Builder");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void showPathBuilder(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("pathBuilder.fxml"));
        primaryStage.setTitle("GCode Path Builder");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void showDrawingWindow(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResourceAsStream("drawingWindow.fxml"));
        primaryStage.setTitle("Drawing Window");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(true);
        primaryStage.show();
        DrawingWindowController controller = loader.getController();
        controller.bindProperties();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        showDrawingWindow(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
