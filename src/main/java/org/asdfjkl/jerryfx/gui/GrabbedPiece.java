package org.asdfjkl.jerryfx.gui;

public class GrabbedPiece {

    private int piece;
    private double currentXLocation;
    private double currentYLocation;

    public void setCurrentXLocation(double currentXLocation) {
        this.currentXLocation = currentXLocation;
    }

    public void setCurrentYLocation(double currentYLocation) {
        this.currentYLocation = currentYLocation;
    }

    public void setPiece(int piece) {
        this.piece = piece;
    }

    public int getPiece() { return this.piece; };

    public double getCurrentXLocation() { return this.currentXLocation; };

    public double getCurrentYLocation() { return this.currentYLocation; };

}
