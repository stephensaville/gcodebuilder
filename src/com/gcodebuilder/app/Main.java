package com.gcodebuilder.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Parent root = FXMLLoader.load(getClass().getResource("faceBuilder.fxml"));
        //primaryStage.setTitle("GCode Facing Program Builder");
        Parent root = FXMLLoader.load(getClass().getResource("pathBuilder.fxml"));
        primaryStage.setTitle("GCode Path Builder");
        primaryStage.setScene(new Scene(root));
        //primaryStage.setResizable(false);
        primaryStage.setResizable(true);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
