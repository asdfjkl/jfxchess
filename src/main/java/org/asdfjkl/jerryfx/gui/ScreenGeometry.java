package org.asdfjkl.jerryfx.gui;

public class ScreenGeometry {

    double xOffset;
    double yOffset;
    double width;
    double height;
    double moveDividerRatio;
    double mainDividerRatio;
    final static double DEFAULT_MOVE_DIVIDER_RATIO = 0.5;
    final static double DEFAULT_MAIN_DIVIDER_RATIO = 0.7;
    final static double DEFAULT_WIDTH_RATIO = 0.8;
    final static double DEFAULT_HEIGHT_RATIO = 0.8;

    public ScreenGeometry(double xOffset, double yOffset, double width, double height, double moveDividerRatio, double mainDividerRatio) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.moveDividerRatio = moveDividerRatio;
        this.mainDividerRatio = mainDividerRatio;
    }

    public boolean isValid() {
        return (xOffset >= 0.0 && yOffset >= 0.0 && width > 100 && height > 100 && moveDividerRatio > 0.1 && mainDividerRatio > 0.1);
    }

}
