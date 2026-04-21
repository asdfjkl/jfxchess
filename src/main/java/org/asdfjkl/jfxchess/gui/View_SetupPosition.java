/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2026 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static java.awt.event.MouseEvent.BUTTON1;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

public class View_SetupPosition extends JPanel {

    private final GrabbedPiece.GrabbedFrom PIECE_SELECTOR = GrabbedPiece.GrabbedFrom.PieceSelector;
    Model_JFXChess model;
    private final Board board;
    private final GrabbedPiece grabbedPiece = new GrabbedPiece();
    PieceImageProvider pieceImageProvider = new PieceImageProvider();
    final double outputScaleX = HighDPIHelper.getUIScaleFactor();
    final int[][] pickupPieces = {
            { CONSTANTS.WHITE_PAWN, CONSTANTS.BLACK_PAWN },
            { CONSTANTS.WHITE_KNIGHT, CONSTANTS.BLACK_KNIGHT },
            { CONSTANTS.WHITE_BISHOP, CONSTANTS.BLACK_BISHOP },
            { CONSTANTS.WHITE_ROOK, CONSTANTS.BLACK_ROOK },
            { CONSTANTS.WHITE_QUEEN, CONSTANTS.BLACK_QUEEN },
            { CONSTANTS.WHITE_KING, CONSTANTS.BLACK_KING }
    };
    int xOffset = 0;
    int innerXOffset = 0;
    int innerYOffset = 0;
    int borderMargin = 18;
    int squareSize = 0;
    int outerMargin = 0;
    public boolean flipBoard;

    // to notify the dialog class about board changes, so the dialog can update the
    // state of the GUI elements. Slightly hacky, no full MVC (but that would be overkill
    // for this simple dialog)
    private final ArrayList<SetupPositionListener> setupPositionListeners = new ArrayList<>();

    public View_SetupPosition(Model_JFXChess model) {

        this.model = model;
        flipBoard = model.getFlipBoard();
        board = model.getGame().getCurrentNode().getBoard().makeCopy();

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                handleMousePress(me);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent me) {
                handleMouseReleased(me);
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent me) {
                handleMouseDragged(me);
                repaint();
            }
        });

    }

    void updateBoard() {
        grabbedPiece.reset();
        repaint();
        notifyBoardChange();
    }

    private void handleMouseDragged(MouseEvent me) {
        if (grabbedPiece.isGrabbed() && grabbedPiece.getDrawImage()) {
            grabbedPiece.setCurrentXLocation(me.getX());
            grabbedPiece.setCurrentYLocation(me.getY());
            repaint();
        }
    }

    private void handleMouseReleased(MouseEvent me) {

        int mouseButton = me.getButton();
        if (mouseButton !=BUTTON1) {
            // This is a Right click event. Copy the most
            // recently grabbedPiece to the new square.
            if (clickedOnBoard(me.getX(), me.getY())) {
                Point boardPos = getBoardPosition(me.getX(), me.getY());
                board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.rightClickPiece());
                updateBoard();
            }
            else
            {
                // You can set the rightClickPiece to an empty square
                // by right-clicking outside the board.
                grabbedPiece.setRightClickPiece(CONSTANTS.EMPTY);
            }
            return;
        }

        // Left mouse-button has been released.
        // Here we perform an action (Moving a piece on the board, placing
        // a piece on the board, deleting a piece from the board, resetting
        // a grabbed piece or ignoring the mouse-release) according to the
        // circumstances.

        // We don't have to draw the grabbed piece anymore,
        // if we have been doing that.
        grabbedPiece.setDrawImage(false);

        // (clickedOnBoard, in this case, means "mouse-button released on board").
        if (clickedOnBoard(me.getX(), me.getY())) {
            Point boardPos = getBoardPosition(me.getX(), me.getY());
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.getPiece());
                    updateBoard();
                } else if ((boardPos.x != grabbedPiece.sourceSquareX() || boardPos.y != grabbedPiece.sourceSquareY())) {
                    // The piece was grabbed in the board, and the mouse was
                    // released at another square -> Move the piece on the board.
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.getPiece());
                    updateBoard();
                } else {
                    // Mouse released on the same board-square where it was pressed ->
                    // Do nothing. (This represents a single click on the piece, with which
                    // the purpose was just to grab the piece.) Just remove possible dragged
                    // piece-image from the canvas. (There is a case where we press the
                    // mouse-button on a piece, drag it around, return to the original square
                    // and release the mouse-button.)
                    // we still need to reset the grabbed Piece
                    grabbedPiece.reset();
                    repaint();
                }
            }
            // Mouse released on the board, but no piece has been grabbed -> Do nothing.
        } else if (clickedOnPieceSelector(me.getX(), me.getY())) {
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    // The piece was both grabbed and released in the pieceSelector.
                    if (grabbedPiece.getPiece() != getSelectedPiece(me.getX(), me.getY())) {
                        // Grabbed at one piece in the pieceSelctor and released on another
                        // piece in the pieceSelector.
                        grabbedPiece.reset();
                    }
                    // Remove possible dragged piece-image from the canvas.
                    repaint();
                } else {
                    // The piece was grabbed in the board and released in
                    // the pieceSelector -> Remove piece from board.
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    updateBoard();
                }
            }
            // Mouse released in pieceSelector , but no piece has been grabbed ->
            // Do nothing.
        } else {
            // Mouse released both outside the board and outside the pieceSelector.
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    // The piece was grabbed in the pieceSelector and released outside.
                    grabbedPiece.reset();
                    // Remove possible dragged piece-image from the canvas.
                    repaint();
                } else {
                    // The piece was grabbed in the board and released outside
                    // -> Remove piece from board.
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    updateBoard();
                }
            }
            // Mouse released outside, but no piece has been grabbed -> Do nothing.
        }
    }

    private void handleMousePress(MouseEvent me) {
        // Inside this scope we only grab a piece from the board or from
        // the pieceSelector or simply ignore the mouse-press,
        // depending on the circumstances.
        int mouseButton = me.getButton();
        if (mouseButton !=BUTTON1) {
            return;
        }
        // (clickedOnBoard, in this case, means "mouse-button pressed on board").
        if (clickedOnBoard(me.getX(), me.getY())) {
            Point boardPos = getBoardPosition(me.getX(), me.getY());
            if (!(grabbedPiece.isGrabbed())) {
                // Select a piece from the board, if there is one on the square.
                if (board.isPieceAt(boardPos.x, boardPos.y)) {
                    grabbedPiece.grab(boardPos.x,
                            boardPos.y,
                            me.getX(),
                            me.getY(),
                            board.getPieceAt(boardPos.x, boardPos.y));
                }
                // Empty square and No grabbed piece -> do nothing.
            }
        } else if (clickedOnPieceSelector(me.getX(), me.getY())) {
            // Grab a piece from the pieceSelector
            grabbedPiece.grab(me.getX(),
                    me.getY(),
                    getSelectedPiece(me.getX(), me.getY()));
        }
        // Outside board and outside the pieceSelector.
        // Do nothing.
    }

    boolean clickedOnBoard(double x, double y) {

        if(x > innerXOffset && y > innerYOffset &&
                x < innerXOffset + (8*squareSize) &&
                y < innerYOffset + (8*squareSize)) {
            return true;
        } else {
            return false;
        }
    }

    boolean clickedOnPieceSelector(double x, double y) {
        int leftOffset = xOffset + 9*squareSize + 3*borderMargin;
        int downOffset = outerMargin + borderMargin;
        if(x > leftOffset && y > downOffset &&
                x < leftOffset + 2*squareSize &&
                y < downOffset + 6*squareSize) {
            return true;
        } else {
            return false;
        }
    }

    Point getBoardPosition(double x, double y) {

        if(x > innerXOffset && y > innerYOffset
                && x < (innerXOffset + 8*squareSize)
                && y < (innerYOffset + 8*squareSize)) {

            int i = (int) x - innerXOffset;
            int j = (int) y - innerYOffset;

            if(flipBoard) {
                i = 7 - (i / squareSize);
                j = j / squareSize;
            } else {
                i = i / squareSize;
                j = 7 - (j / squareSize);
            }
            return new Point(i,j);
        }
        return null;
    }

    int getSelectedPiece(double x, double y) {
        int leftOffset = xOffset + 9*squareSize + 3*borderMargin;
        int downOffset = outerMargin + borderMargin;

        int xIdx = (int) (x - leftOffset);
        int yIdx = (int) (y - downOffset);
        xIdx = xIdx / squareSize;
        yIdx = yIdx / squareSize;
        return pickupPieces[yIdx][xIdx];
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);

        // fill background
        Graphics2D g2 = (Graphics2D) g;
        //g2.scale(outputScaleX, outputScaleX);
        g2.setColor(model.getBoardStyle().getDarkSquareColor());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // compute possible square width and height and take the minimum
        // for the width, we take into account that we need to paint
        // on the right next to board the pieces that can be picked up/dragged

        outerMargin = (int) (this.getWidth() * 0.05);
        double squareWidth = (this.getWidth() - (4 * borderMargin) - (2* outerMargin)) / 11.0;
        double squareHeight = (this.getHeight() - (2 * borderMargin) - (2* outerMargin)) / 8.0;

        squareSize = (int) Math.min(squareWidth, squareHeight);

        xOffset = outerMargin;
        innerXOffset = (xOffset + borderMargin);
        innerYOffset = (outerMargin + borderMargin);

        // paint board inc. margin with letters & numbers
        g2.setColor(model.getBoardStyle().getBorderColor());
        g2.fillRect(xOffset, outerMargin, (squareSize*8)+(borderMargin*2), (squareSize*8)+(borderMargin*2));

        // paint squares
        Color fieldColor;
        for(int i=0;i<8;i++) {
            for(int j=0;j<8;j++) {
                if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                    if(!flipBoard) {
                        fieldColor = model.getBoardStyle().getLightSquareColor();
                    } else {
                        fieldColor = model.getBoardStyle().getDarkSquareColor();
                    }
                } else {
                    if(!flipBoard) {
                        fieldColor = model.getBoardStyle().getDarkSquareColor();
                    } else {
                        fieldColor = model.getBoardStyle().getLightSquareColor();
                    }
                }
                int x = (innerXOffset) + (i*squareSize);
                if(flipBoard) {
                    x = innerXOffset +((7-i)*squareSize);
                }
                int y = (innerYOffset) + ((7-j)*squareSize);

                g2.setColor(fieldColor);
                g2.fillRect(x,y,squareSize,squareSize);
            } // for j
        } // for i

        // draw the board coordinates
        g2.setColor(model.getBoardStyle().getCoordinateColor());
        for(int i=0;i<8;i++) {
            if(flipBoard){
                char ch = (char) (65 + (7 - i));
                String idx = Character.toString(ch);
                String num = Integer.toString(i + 1);
                g2.drawString(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8)));
                g2.drawString(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            } else{
                char ch = (char) (65 + i);
                String idx = Character.toString(ch);
                String num = Integer.toString(8 - i);
                g2.drawString(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8)));
                g2.drawString(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            }
        }

        // draw pieces
        for(int i=0;i<8;i++) {
            for (int j = 0; j < 8; j++) {
                int x;
                if(flipBoard) {
                    x = innerXOffset +((7-i)*squareSize);
                } else {
                    x = innerXOffset +(i*squareSize);
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
                        if (!(grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() &&
                                j == grabbedPiece.sourceSquareY())) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    model.getBoardStyle().getPieceStyle());
                            g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);

                        }
                    } else if (!(grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() &&
                            (7 - j) == grabbedPiece.sourceSquareY())) {
                        Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                model.getBoardStyle().getPieceStyle());
                        g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                    }
                }
            }
        }

        // draw rect for piece selection. reset to border color
        g2.setColor(model.getBoardStyle().getBorderColor());
        g2.fillRect((xOffset + 9*squareSize) + (borderMargin*2),
                outerMargin,
                (squareSize*2)+(borderMargin*2),
                (squareSize*6)+(borderMargin*2));

        // Mark the selected piece in the pieceSelector-area with DarkSquareColor.
        // In dragNDropMode we only want to do this if the user actually pressed
        // or clicked the mouse-button in that area when the piece was grabbed.
        for(int i=0;i<6;i++) {
            for(int j=0;j<2;j++) {
                // draw pickup squares
                g2.setColor(model.getBoardStyle().getLightSquareColor());
                g2.fillRect((xOffset + (9+j)*squareSize) + (borderMargin*3),
                        outerMargin + (i*squareSize) + borderMargin,
                        squareSize, squareSize);


                // draw pickup piece image
                int pieceType =  pickupPieces[i][j];
                Image pieceImage = pieceImageProvider.getImage(pieceType, (int) (squareSize * this.outputScaleX),
                        model.getBoardStyle().getPieceStyle());
                int x = (xOffset + (9+j)*squareSize) + (borderMargin*3);
                int y = outerMargin + (i*squareSize) + borderMargin;
                g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
            }
        }

        // draw grabbed piece
        if (grabbedPiece.getDrawImage()) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(), (int) (squareSize * this.outputScaleX),
                    model.getBoardStyle().getPieceStyle());
            g2.drawImage(pieceImage, (int) (grabbedPiece.getCurrentXLocation() - offset),
                    (int) (grabbedPiece.getCurrentYLocation() - offset), squareSize, squareSize, null);
        }

    }


    public void resetToStartingPosition() {
        board.resetToStartingPosition();
        grabbedPiece.full_reset();
        repaint();
    }

    public void clearBoard() {
        board.clear();
        grabbedPiece.full_reset();
        repaint();
    }

    public void copyBoard(Board b) {
        board.copy(b);
        grabbedPiece.full_reset();
        repaint();
    }

    public boolean isConsistent() {
        return board.isConsistent();
    }

    public boolean turn() {
        return board.turn;
    }

    public boolean canCastleWhiteKing() {
        return board.canCastleWhiteKing();
    }

    public boolean canCastleWhiteQueen() {
        return board.canCastleWhiteQueen();
    }

    public boolean canCastleBlackKing() {
        return board.canCastleBlackKing();
    }

    public boolean canCastleBlackQueen() {
        return board.canCastleBlackQueen();
    }

    public void setCastleWKing(boolean b) {
        board.setCastleWKing(b);
    }

    public void setCastleWQueen(boolean b) {
        board.setCastleWQueen(b);
    }

    public void setCastleBKing(boolean b) {
        board.setCastleBKing(b);

    }

    public void setCastleBQueen(boolean b) {
        board.setCastleBQueen(b);

    }

    /*
     expect parameter of type "-", "a3", "b3", ... "g6", "h6"
     */
    public void setEnPassantSquare(String epSquare) {
        board.setEnPassantSquare(epSquare);
    }

    public String getEnPassantSquare() {
        return board.getEnPassantSquare();
    }

    public void setTurn(boolean b) {
        board.turn = b;
    }

    public Board makeBoardCopy() {
        return board.makeCopy();
    }

    public void addListener(SetupPositionListener listener) {
        setupPositionListeners.add(listener);
    }

    public void removeListener(SetupPositionListener listener) {
        setupPositionListeners.remove(listener);
    }

    public void notifyBoardChange() {
        for (SetupPositionListener listener : setupPositionListeners)
            listener.boardChanged();
    }

    public boolean isBoardConsistent() {
        return board.isConsistent();
    }
}
