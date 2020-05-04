package com.gcodebuilder.app;

import com.gcodebuilder.geometry.Drawing;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperations {
    private static final Logger log = LogManager.getLogger(FileOperations.class);

    private final Node root;
    private final FileChooser chooser;

    public FileOperations(Node root) {
        this.root = root;
        chooser = new FileChooser();
        FileChooser.ExtensionFilter jsonExtFilter =
                new FileChooser.ExtensionFilter("JSON", "*.json");
        chooser.getExtensionFilters().add(jsonExtFilter);
        chooser.setSelectedExtensionFilter(jsonExtFilter);
        String homeEnvVal = System.getenv("HOME");
        File initialDirectory = File.listRoots()[0];
        if (homeEnvVal != null) {
            Path homePath = Paths.get(homeEnvVal);
            File homeFile = homePath.toFile();
            if (homeFile.isDirectory()) {
                Path documentsPath = homePath.resolve("Documents");
                File documentsFile = documentsPath.toFile();
                if (documentsFile.isDirectory()) {
                    initialDirectory = documentsFile;
                } else {
                    initialDirectory = homeFile;
                }
            }
        }
        chooser.setInitialDirectory(initialDirectory);
        chooser.setInitialFileName("drawing.json");
    }

    private void showError(String title, String message, Exception exception) {
        log.error(String.format("Showing error: title=%s message=%s", title, message), exception);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();
        alert.setContentText(exceptionText);

        alert.showAndWait();
    }

    public Drawing open() {
        chooser.setTitle("Open Drawing");
        File openFile = chooser.showOpenDialog(root.getScene().getWindow());
        if (openFile != null) {
            try {
                try (FileInputStream in = new FileInputStream(openFile)) {
                    return Drawing.load(in);
                }
            } catch (IOException ex) {
                showError("Open Failed", String.format("Failed to open file: %s", openFile), ex);
            }
        }
        return null;
    }

    public void save(Drawing drawing) {
        chooser.setTitle("Save Drawing");
        File saveFile = chooser.showSaveDialog(root.getScene().getWindow());
        if (saveFile != null) {
            try {
                try (FileOutputStream out = new FileOutputStream(saveFile)) {
                    drawing.save(out);
                }
            } catch (IOException ex) {
                showError("Save Failed", String.format("Failed to save file: %s", saveFile), ex);
            }
        }
    }
}
