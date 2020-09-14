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
