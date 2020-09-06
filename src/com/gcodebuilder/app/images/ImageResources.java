package com.gcodebuilder.app.images;

import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ImageResources {
    private static final Logger log = LogManager.getLogger(ImageResources.class);

    public static Image load(String resourceName, boolean backgroundLoading) {
        String url = ImageResources.class.getResource(resourceName).toExternalForm();
        log.info("Loading image resource: {}", url);
        return new Image(url, backgroundLoading);
    }

    public static Image load(String resourceName) {
        return load(resourceName, false);
    }

    public static final String ICON_RESOURCE_PREFIX = "gcode_builder_icon";
    public static final String ICON_RESOURCE_SUFFIX = ".png";
    public static final int[] ICON_RESOURCE_SIZES = {64, 128, 256, 512, 1024};

    public static List<Image> loadIcons(boolean backgroundLoading) {
        return Arrays.stream(ICON_RESOURCE_SIZES)
                .mapToObj(size -> String.format("%s_%dx%d%s", ICON_RESOURCE_PREFIX, size, size, ICON_RESOURCE_SUFFIX))
                .map(name -> load(name, backgroundLoading))
                .collect(Collectors.toList());
    }

    public static List<Image> loadIcons() {
        return loadIcons(false);
    }
}
