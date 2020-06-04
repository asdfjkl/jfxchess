package org.asdfjkl.jerryfx.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Chessboard extends Canvas {

    Color background;

    public Chessboard() {
        background = Color.rgb(225, 229, 111);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double maxHeight(double width) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double maxWidth(double height) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double minWidth(double height) {
        return 1D;
    }

    @Override
    public double minHeight(double width) {
        return 1D;
    }

    @Override
    public void resize(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);

        System.out.println("width "+width + "height "+height);
        updateCanvas();
    }

    public void updateCanvas() {

        GraphicsContext gc = this.getGraphicsContext2D();
        //gc.moveTo(0,0);
        gc.beginPath();
        gc.setFill(background);
        //gc.rect(0, 0, 10, 10);
        gc.rect(0, 0, this.getWidth(), this.getHeight());
        gc.fill();

    }

}
