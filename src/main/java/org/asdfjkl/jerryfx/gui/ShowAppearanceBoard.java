package org.asdfjkl.jerryfx.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jerryfx.lib.Board;
import java.awt.*;

import static org.asdfjkl.jerryfx.lib.CONSTANTS.EMPTY;
import static org.asdfjkl.jerryfx.lib.CONSTANTS.FRINGE;

public class ShowAppearanceBoard extends Canvas {

        BoardStyle boardStyle;
        double outputScaleX;
        GameModel gameModel;
        boolean flipBoard = true;

        int innerXOffset;
        int innerYOffset;
        int squareSize;

        PieceImageProvider pieceImageProvider;

        Point moveSource;
        GrabbedPiece grabbedPiece = new GrabbedPiece();
        boolean drawGrabbedPiece = false;
        Board board;

    public ShowAppearanceBoard() {

            this.boardStyle = new BoardStyle();
            this.pieceImageProvider = new PieceImageProvider();
            this.outputScaleX = Screen.getPrimary().getOutputScaleX();
            this.board = new Board(true);

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
            return 50D;
        }

        @Override
        public double minHeight(double width) {
            return 50D;
        }

        @Override
        public void resize(double width, double height) {
            this.setWidth(width);
            this.setHeight(height);

            updateCanvas();
        }

        public void updateCanvas() {

            System.out.println("CANVAS: "+boardStyle.getPieceStyle());

            GraphicsContext gc = this.getGraphicsContext2D();

            // fill background
            gc.beginPath();
            //gc.setFill(Color.rgb(152, 152, 152));
            gc.setFill(boardStyle.getLightSquareColor());
            gc.rect(0, 0, this.getWidth(), this.getHeight());
            gc.fill();

            // size of real board incl. corner
            double height = this.getHeight();
            double width = this.getWidth();
            double minWidthHeight = Math.min(height, width);

            // spare 2 percent left and right
            int outerMargin = (int) (minWidthHeight * 0.05);
            int boardSize = (int) (minWidthHeight - (2*outerMargin));

            int xOffset = outerMargin;
            if(width > height) {
                int surplus = (int) (width - height);
                xOffset += surplus/2;
            }

            int borderMargin = 18; // (int) (minWidthHeight * 0.03);
            squareSize = ((boardSize - (2* borderMargin)) / 8);
            innerXOffset = (xOffset + borderMargin);
            innerYOffset = (outerMargin + borderMargin);

            // paint board inc. margin with letters & numbers
            gc.beginPath();
            gc.setFill(boardStyle.getBorderColor());
            gc.rect(xOffset, outerMargin, (squareSize*8)+(borderMargin*2), (squareSize*8)+(borderMargin*2));
            gc.fill();

            // paint squares
            Color fieldColor;
            for(int i=0;i<8;i++) {
                for(int j=0;j<8;j++) {
                    if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                        fieldColor = boardStyle.getLightSquareColor();
                    } else {
                        fieldColor = boardStyle.getDarkSquareColor();
                    }
                    int x = (innerXOffset) + (i*squareSize);
                    if(flipBoard) {
                        x = innerXOffset+((7-i)*squareSize);
                    }
                    int y = (innerYOffset) + ((7-j)*squareSize);

                    gc.beginPath();
                    gc.setFill(fieldColor);
                    gc.rect(x,y,squareSize,squareSize);
                    gc.fill();
                }
            }

            // draw the board coordinates
            gc.setFill(boardStyle.getCoordinateColor());
            for(int i=0;i<8;i++) {
                if(flipBoard){
                    char ch = (char) (65 + (7 - i));
                    String idx = Character.toString(ch);
                    String num = Integer.toString(i + 1);
                    gc.beginPath();
                    gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                            innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                    gc.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
                } else{
                    char ch = (char) (65 + i);
                    String idx = Character.toString(ch);
                    String num = Integer.toString(8 - i);
                    gc.beginPath();
                    gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                            innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                    gc.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
                }
            }

            // draw pieces
            for(int i=0;i<8;i++) {
                for (int j = 0; j < 8; j++) {
                    int x;
                    if(flipBoard) {
                        x = innerXOffset+((7-i)*squareSize);
                    } else {
                        x = innerXOffset+(i*squareSize);
                    }
                    // drawing coordinates are from top left
                    // whereas chess coords are from bottom left
                    int y = innerYOffset+((7-j)*squareSize);
                    int piece = 0;
                    if(flipBoard) {
                        piece = board.getPieceAt(i, 7-j);
                    } else {
                        piece = board.getPieceAt(i, j);
                    }
                    if(piece != EMPTY && piece != FRINGE) {
                        if(!flipBoard) {
                            if (!(drawGrabbedPiece && i == moveSource.x && j == moveSource.y)) {
                                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                        boardStyle.getPieceStyle());
                                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                            }
                        } else {
                            if (!(drawGrabbedPiece && i == moveSource.x && (7-j) == moveSource.y)) {
                                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                        boardStyle.getPieceStyle());
                                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                            }
                        }
                    }
                }
            }
        }

}
