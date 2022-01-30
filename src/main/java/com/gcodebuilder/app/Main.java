/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
