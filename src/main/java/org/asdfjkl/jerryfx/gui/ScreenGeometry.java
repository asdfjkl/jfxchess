package org.asdfjkl.jerryfx.gui;

public class ScreenGeometry {

    double xOffset;
    double yOffset;
    double width;
    double height;
    double moveDividerRatio;
    double mainDividerRatio;

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
