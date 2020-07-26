package com.gcodebuilder.app;

import com.google.common.base.Preconditions;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FileOperations<T> {
    private static final Logger log = LogManager.getLogger(FileOperations.class);

    @FunctionalInterface
    public interface LoadFunction<T> {
        T load(InputStream in) throws IOException;
    }

    public interface SaveFunction<T> {
        void save(T document, OutputStream out) throws IOException;
    }

    private final Node root;
    private final String documentType;
    private final LoadFunction<T> loadFunction;
    private final SaveFunction<T> saveFunction;

    private final FileChooser chooser;

    public FileOperations(Node root,
                          LoadFunction<T> loadFunction,
                          SaveFunction<T> saveFunction,
                          String documentType,
                          String initialFileName,
                          FileChooser.ExtensionFilter... extensionFilters) {
        Preconditions.checkNotNull(root);
        Preconditions.checkNotNull(loadFunction);
        Preconditions.checkNotNull(saveFunction);
        Preconditions.checkNotNull(documentType);
        Preconditions.checkNotNull(initialFileName);
        Preconditions.checkNotNull(extensionFilters);
        Preconditions.checkArgument(extensionFilters.length > 0);

        this.root = root;
        this.documentType = documentType;
        this.loadFunction = loadFunction;
        this.saveFunction = saveFunction;

        chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(extensionFilters);
        chooser.setSelectedExtensionFilter(extensionFilters[0]);
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
        chooser.setInitialFileName(initialFileName);
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

    public T open() {
        chooser.setTitle(String.format("Open %s", documentType));
        File openFile = chooser.showOpenDialog(root.getScene().getWindow());
        if (openFile != null) {
            try {
                try (FileInputStream in = new FileInputStream(openFile)) {
                    return loadFunction.load(in);
                }
            } catch (IOException ex) {
                showError("Open Failed", String.format("Failed to open file: %s", openFile), ex);
            }
        }
        return null;
    }

    public void save(T document) {
        chooser.setTitle(String.format("Save %s", documentType));
        File saveFile = chooser.showSaveDialog(root.getScene().getWindow());
        if (saveFile != null) {
            try {
                try (FileOutputStream out = new FileOutputStream(saveFile)) {
                    saveFunction.save(document, out);
                }
            } catch (IOException ex) {
                showError("Save Failed", String.format("Failed to save file: %s", saveFile), ex);
            }
        }
    }
}
