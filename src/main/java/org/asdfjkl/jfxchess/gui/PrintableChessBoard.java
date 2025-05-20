package org.asdfjkl.jfxchess.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;


import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.BLACK;

public class PrintableChessBoard {

    public PrintableChessBoard() {
    }

    public Image getImage(Board board, int size, boolean flipBoard) {

        PieceImageProvider pieceImageProvider = new PieceImageProvider();
        final Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // fill background
        gc.beginPath();
        gc.setFill(Color.WHITE);
        gc.rect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.fill();

        // size of real board incl. corner
        double height = canvas.getHeight();
        double width = canvas.getWidth();
        double minWidthHeight = Math.min(height, width);

        // spare 2 percent left and right
        int outerMargin = (int) (minWidthHeight * 0.05);
        int boardSize = (int) (minWidthHeight - (2 * outerMargin));

        int xOffset = outerMargin;
        if (width > height) {
            int surplus = (int) (width - height);
            xOffset += surplus / 2;
        }

        int borderMargin = 18; // (int) (minWidthHeight * 0.03);
        int squareSize = ((boardSize - (2 * borderMargin)) / 8);
        int innerXOffset = (xOffset + borderMargin);
        int innerYOffset = (outerMargin + borderMargin);

        // paint board inc. margin with letters & numbers
        gc.beginPath();
        gc.setFill(Color.WHITE);
        gc.rect(xOffset, outerMargin, (squareSize * 8) + (borderMargin * 2), (squareSize * 8) + (borderMargin * 2));
        gc.fill();

        Color lightSquareColor = Color.WHITE;
        Color darkSquareColor = Color.LIGHTGRAY;

        // rect around squares, 2 pix wide
        gc.beginPath();
        gc.setFill(Color.BLACK);
        gc.rect(innerXOffset-2, innerYOffset-2, (squareSize * 8)+4, (squareSize * 8)+4);
        gc.fill();


        // paint squares
        Color fieldColor;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((j % 2 == 0 && i % 2 == 1) || (j % 2 == 1 && i % 2 == 0)) {
                    if (!flipBoard) {
                        fieldColor = lightSquareColor;
                    } else {
                        fieldColor = darkSquareColor;
                    }
                } else {
                    if (!flipBoard) {
                        fieldColor = darkSquareColor;
                    } else {
                        fieldColor = lightSquareColor;
                    }
                }
                int x = (innerXOffset) + (i * squareSize);
                if (flipBoard) {
                    x = innerXOffset + ((7 - i) * squareSize);
                }
                int y = (innerYOffset) + ((7 - j) * squareSize);

                gc.beginPath();
                gc.setFill(fieldColor);
                gc.rect(x, y, squareSize, squareSize);
                gc.fill();
            }
        }

        // draw the board coordinates
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("sans",FontWeight.BOLD, 30));
        for (int i = 0; i < 8; i++) {
            if (flipBoard) {
                char ch = (char) (65 + (7 - i));
                String idx = Character.toString(ch);
                String num = Integer.toString(i + 1);
                gc.beginPath();
                gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        innerYOffset + (8 * squareSize) + (borderMargin * 0.8) +20);
                gc.fillText(num, xOffset - 15, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            } else {
                char ch = (char) (65 + i);
                String idx = Character.toString(ch);
                String num = Integer.toString(8 - i);
                gc.beginPath();
                gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        innerYOffset + (8 * squareSize) + (borderMargin * 0.8)+20);
                gc.fillText(num, xOffset - 15, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
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
                        Image pieceImage = pieceImageProvider.getImage(piece, squareSize, BoardStyle.PIECE_STYLE_MERIDA);
                            gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                    } else {
                            Image pieceImage = pieceImageProvider.getImage(piece, squareSize, BoardStyle.PIECE_STYLE_MERIDA);
                            gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                    }
                }
            }
        }

        // mark side to move
        int x_side_to_move = innerXOffset + 8 * squareSize + 15;
        int y_side_to_move = (innerYOffset + 8 * squareSize)-20;
        if (board.turn == WHITE) {
            if (flipBoard) {
                //x_side_to_move = innerXOffset - 11;
                y_side_to_move = innerYOffset;
            }
        }
        if (board.turn == BLACK) {
            if (!flipBoard) {
                //x_side_to_move = innerXOffset - 11;
                y_side_to_move = innerYOffset;
            }
        }
        gc.beginPath();
        gc.setFill(Color.BLACK);
        gc.rect(x_side_to_move, y_side_to_move, 20, 20);
        gc.fill();

        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, image);
        return image;
    }
}



