package org.asdfjkl.jfxchess.gui;

import javafx.stage.Stage;

public final class PlatformUtils {
    private PlatformUtils() {}

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static void applyDialogSizeFix(Stage stage, double baseWidth, double baseHeight) {
        if (isLinux()) {
            stage.setMinWidth(baseWidth);
            stage.setMinHeight(baseHeight);
        }
    }

    public static void forceHeight(Stage stage, double value) {
        if (isLinux()) {
            stage.setHeight(value);
        }
    }
}