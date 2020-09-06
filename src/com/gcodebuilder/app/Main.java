package com.gcodebuilder.app;

import com.gcodebuilder.app.images.ImageResources;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {
    private static final Logger log = LogManager.getLogger(Main.class);

    private void showFaceBuilder(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("faceBuilder.fxml"));
        primaryStage.setTitle("GCode Facing Program Builder");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void showPathBuilder(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResourceAsStream("pathBuilder.fxml"));
        primaryStage.setTitle("GCode Path Builder");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(true);
        primaryStage.show();
        PathBuilderController controller = loader.getController();
        controller.bindProperties();
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
        primaryStage.getIcons().addAll(ImageResources.loadIcons());

        log.info("named parameters: " + getParameters().getNamed());
        log.info("unnamed parameters: " + getParameters().getUnnamed());
        String ui = getParameters().getNamed().getOrDefault("ui", "DrawingWindow");
        switch (ui) {
            case "FaceBuilder":
                showFaceBuilder(primaryStage);
                break;
            case "PathBuilder":
                showPathBuilder(primaryStage);
                break;
            case "DrawingWindow":
            default:
                showDrawingWindow(primaryStage);
                break;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
