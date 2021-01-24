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

import com.google.common.base.Preconditions;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
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

public class FileOperations<T> {
    private static final Logger log = LogManager.getLogger(FileOperations.class);

    @FunctionalInterface
    public interface LoadFunction<T> {
        T load(InputStream in) throws IOException;
    }

    public interface SaveFunction<T> {
        void save(T document, OutputStream out) throws IOException;
    }

    public interface FileSaveFunction<T> {
        void save(T document, File file, FileChooser.ExtensionFilter ext) throws IOException;
    }

    private final Node root;
    private final String documentType;
    private final LoadFunction<T> loadFunction;
    private final SaveFunction<T> saveFunction;
    private final FileSaveFunction<T> fileSaveFunction;

    @Getter
    private final FileChooser chooser;

    @Getter
    @Setter
    private File currentFile;

    private FileOperations(Node root,
                          LoadFunction<T> loadFunction,
                          SaveFunction<T> saveFunction,
                          FileSaveFunction<T> fileSaveFunction,
                          String documentType,
                          String initialFileName,
                          FileChooser.ExtensionFilter... extensionFilters) {
        Preconditions.checkNotNull(root);
        Preconditions.checkNotNull(loadFunction);
        Preconditions.checkArgument((saveFunction == null) != (fileSaveFunction == null));
        Preconditions.checkNotNull(documentType);
        Preconditions.checkNotNull(initialFileName);
        Preconditions.checkNotNull(extensionFilters);
        Preconditions.checkArgument(extensionFilters.length > 0);

        this.root = root;
        this.documentType = documentType;
        this.loadFunction = loadFunction;
        this.saveFunction = saveFunction;
        this.fileSaveFunction = fileSaveFunction;

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

    public FileOperations(Node root,
                           LoadFunction<T> loadFunction,
                           SaveFunction<T> saveFunction,
                           String documentType,
                           String initialFileName,
                           FileChooser.ExtensionFilter... extensionFilters) {
        this(root, loadFunction, saveFunction, null, documentType, initialFileName, extensionFilters);
    }

    public FileOperations(Node root,
                           LoadFunction<T> loadFunction,
                           FileSaveFunction<T> fileSaveFunction,
                           String documentType,
                           String initialFileName,
                           FileChooser.ExtensionFilter... extensionFilters) {
        this(root, loadFunction, null, fileSaveFunction, documentType, initialFileName, extensionFilters);
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
                    T document = loadFunction.load(in);
                    currentFile = openFile;
                    return document;
                }
            } catch (IOException ex) {
                showError("Open Failed", String.format("Failed to open file: %s", openFile), ex);
            }
        }
        return null;
    }

    public void saveAs(T document) {
        chooser.setTitle(String.format("Save %s", documentType));
        File saveFile = chooser.showSaveDialog(root.getScene().getWindow());
        if (saveFile != null) {
            try {
                if (saveFunction != null) {
                    try (FileOutputStream out = new FileOutputStream(saveFile)) {
                        saveFunction.save(document, out);
                    }
                } else if (fileSaveFunction != null) {
                    fileSaveFunction.save(document, saveFile, chooser.getSelectedExtensionFilter());
                }
                currentFile = saveFile;
            } catch (IOException ex) {
                showError("Save Failed", String.format("Failed to save file: %s", saveFile), ex);
                currentFile = null;
            }
        }
    }

    public void save(T document) {
        if (currentFile != null) {
            try (FileOutputStream out = new FileOutputStream(currentFile)) {
                saveFunction.save(document, out);
            } catch (IOException ex) {
                showError("Save Failed", String.format("Failed to save file: %s", currentFile), ex);
            }
        } else {
            saveAs(document);
        }
    }
}
