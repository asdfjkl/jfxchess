/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;
import java.awt.*;
import java.util.ArrayList;

import static java.lang.Math.min;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.EMPTY;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.FRINGE;

public class EnterPosBoard extends Canvas {

    // No one can mess with this Board reference
    // from the outside anymore. ;-)
    private final Board board;

    private final ArrayList<EnterPosBoardListener> enterPosBoardListeners = new ArrayList<>();
    BoardStyle boardStyle;
    final double outputScaleX;
    boolean flipBoard = false;

    int innerXOffset;
    int innerYOffset;
    int squareSize;
    int xOffset;
    final int borderMargin = 18;
    int outerMargin;

    final PieceImageProvider pieceImageProvider;

    // The dragNDropMode for setting up pieces is intended to work like this:
    // You can, of course, drag pieces around on the board, or drag pieces
    // from the piece-selector to the board, (by using the left mouse-button).
    // To remove a piece from the board, just drag it outside the board.
    // It's also possible to just click a piece and then click the destination-
    // square (or click outside the board to remove the selected piece).
    // If you wish to copy the most recently grabbed piece to some other squares,
    // just right-click those squares. Deleting many pieces can be done more
    // quickly by right-clicking outside the board when nothing is selected,
    // then right-clicking the pieces to be removed.

    // setting the boolean above to false will make everything work as before.
    private final GrabbedPiece grabbedPiece;
    private final Color grabbedSquareColor = Color.rgb(200, 200, 0, 0.4);
    private final GrabbedPiece.GrabbedFrom BOARD = GrabbedPiece.GrabbedFrom.Board;
    private final GrabbedPiece.GrabbedFrom PIECESELECTOR = GrabbedPiece.GrabbedFrom.PieceSelector;

    final int[][] pickupPieces = {
        { CONSTANTS.WHITE_PAWN, CONSTANTS.BLACK_PAWN },
        { CONSTANTS.WHITE_KNIGHT, CONSTANTS.BLACK_KNIGHT },
        { CONSTANTS.WHITE_BISHOP, CONSTANTS.BLACK_BISHOP },
        { CONSTANTS.WHITE_ROOK, CONSTANTS.BLACK_ROOK },
        { CONSTANTS.WHITE_QUEEN, CONSTANTS.BLACK_QUEEN },
        { CONSTANTS.WHITE_KING, CONSTANTS.BLACK_KING }
    };

    int selectedPiece = CONSTANTS.WHITE_PAWN;

    public EnterPosBoard(Board board) {

        // No initial "default-piece"
        selectedPiece = CONSTANTS.EMPTY;

        this.boardStyle = new BoardStyle();
        this.board = board.makeCopy(); // this.board will refer to a new value-copied instance.
        this.grabbedPiece = new GrabbedPiece();
        this.pieceImageProvider = new PieceImageProvider();
        this.outputScaleX = Screen.getPrimary().getOutputScaleX();

        setOnMousePressed(event -> {
            handleMousePress(event);
        });

        setOnMouseDragged(event -> {
            handleMouseDragged(event);
        });

        setOnMouseReleased(event -> {
            handleMouseRelease(event);
        });
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
        return Math.max(this.getHeight() * 1.5,150D);
    }

    @Override
    public double minHeight(double width) {
        return 80D;
    }

    @Override
    public void resize(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);
        updateCanvas();
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

    public void updateCanvas() {

        GraphicsContext gc = this.getGraphicsContext2D();

        // fill background
        gc.beginPath();
        //gc.setFill(Color.rgb(152, 152, 152));
        //gc.setFill(boardStyle.getLightSquareColor());
        gc.setFill(boardStyle.getDarkSquareColor());
        gc.rect(0, 0, this.getWidth(), this.getHeight());
        gc.fill();

        // size of real board incl. corner
        double height = this.getHeight();
        double width = this.getWidth();
        double minWidthHeight = min(height, width);

        // spare 2 percent left and right
        outerMargin = (int) (minWidthHeight * 0.05);
        int boardSize = (int) (minWidthHeight - (2*outerMargin));

        xOffset = outerMargin;

        squareSize = ((boardSize - (2* borderMargin)) / 8);

        if(width > height) {
            int widthBoardincPieceSel = (((boardSize - (4* borderMargin)) / 8))*12;
            int surplus = (int) (widthBoardincPieceSel - height);
            xOffset += (surplus/2)+1;
        }

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
                    if(!flipBoard) {
                        fieldColor = boardStyle.getLightSquareColor();
                    } else {
                        fieldColor = boardStyle.getDarkSquareColor();
                    }
                } else {
                    if(!flipBoard) {
                        fieldColor = boardStyle.getDarkSquareColor();
                    } else {
                        fieldColor = boardStyle.getLightSquareColor();
                    }
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

                // Mark the grabbedPiece's source-square on the board
                if (grabbedPiece.grabbedFrom == BOARD) {
                    boolean markField = false;
                    if (!flipBoard) {
                        if (grabbedPiece.sourceSquareX() == i && grabbedPiece.sourceSquareY() == j) {
                            markField = true;
                        }
                    } else {
                        // Board is flipped.
                        if (grabbedPiece.sourceSquareX() == i && grabbedPiece.sourceSquareY() == 7 - j) {
                            markField = true;
                        }
                    }
                    if (markField) {
                        gc.beginPath();
                        gc.setFill(grabbedSquareColor);
                        gc.rect(x, y, squareSize, squareSize);
                        gc.fill();
                    }
                }
            } // for j
        } // for i

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
                        if (!(grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() &&
			      j == grabbedPiece.sourceSquareY())) {
                            javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece,
                                                                                              (int) (squareSize * this.outputScaleX),
											      boardStyle.getPieceStyle());
                            gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                        }
                    } else if (!(grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() &&
				 (7 - j) == grabbedPiece.sourceSquareY())) {
                        javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece,
                                                                                          (int) (squareSize * this.outputScaleX),
											  boardStyle.getPieceStyle());
                        gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                    }
                }
            }
        }

        // draw rect for piece selection. reset to border color
        gc.beginPath();
        gc.setFill(boardStyle.getBorderColor());
        gc.rect((xOffset + 9*squareSize) + (borderMargin*2),
                outerMargin,
                (squareSize*2)+(borderMargin*2),
                (squareSize*6)+(borderMargin*2));
        gc.fill();

        // Mark the selected piece in the pieceSelector-area with DarkSquareColor.
        // In dragNDropMode we only want to do this if the user actually pressed
        // or clicked the mouse-button in that area when the piece was grabbed.
        for(int i=0;i<6;i++) {
            for(int j=0;j<2;j++) {
                // draw pickup squares
                gc.beginPath();
                gc.setFill(boardStyle.getLightSquareColor());
                gc.rect((xOffset + (9+j)*squareSize) + (borderMargin*3),
                        outerMargin + (i*squareSize) + borderMargin,
                        squareSize, squareSize);
                gc.fill();

                // draw pickup piece image
                int pieceType =  pickupPieces[i][j];
                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(pieceType, (int) (squareSize * this.outputScaleX),
                        boardStyle.getPieceStyle());
                int x = (xOffset + (9+j)*squareSize) + (borderMargin*3);
                int y = outerMargin + (i*squareSize) + borderMargin;
                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
            }
        }

        // draw grabbed piece
        if (grabbedPiece.getDrawImage()) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(),
                    (int) (squareSize * this.outputScaleX), boardStyle.getPieceStyle());
            gc.drawImage(pieceImage, grabbedPiece.getCurrentXLocation() - offset,
                    grabbedPiece.getCurrentYLocation() - offset,squareSize, squareSize);
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

    void handleMousePress(MouseEvent e) {

        // Inside this scope we only grab a piece from the board or from
        // the pieceSelector or simply ignore the mouse-press,
        // depending on the circumstances.
        MouseButton mouseButton = e.getButton();
        if (mouseButton != MouseButton.PRIMARY) {
            return;
        }
        // (clickedOnBoard, in this case, means "mouse-button pressed on board").
        if (clickedOnBoard(e.getX(), e.getY())) {
            Point boardPos = getBoardPosition(e.getX(), e.getY());
            if (!(grabbedPiece.isGrabbed())) {
                // Select a piece from the board, if there is one on the square.
                if (board.isPieceAt(boardPos.x, boardPos.y)) {
                    grabbedPiece.grab(boardPos.x,
                            boardPos.y,
                            e.getX(),
                            e.getY(),
                            board.getPieceAt(boardPos.x, boardPos.y));
                }
                // Empty square and No grabbed piece -> do nothing.
            }
        } else if (clickedOnPieceSelector(e.getX(), e.getY())) {
            // Grab a piece from the pieceSelector
            grabbedPiece.grab(e.getX(),
                    e.getY(),
                    getSelectedPiece(e.getX(), e.getY()));
        }
        // Outside board and outside the pieceSelector.
        // Do nothing.
    }

    void handleMouseDragged(MouseEvent e) {
        if (grabbedPiece.isGrabbed() && grabbedPiece.getDrawImage()) {
            grabbedPiece.setCurrentXLocation(e.getX());
            grabbedPiece.setCurrentYLocation(e.getY());
            updateCanvas();
        }
    }

    void updateBoard() {
        grabbedPiece.reset();
        updateCanvas();
        notifyBoardChange();
    }

    void handleMouseRelease(MouseEvent e) {
        MouseButton mouseButton = e.getButton();
        if (mouseButton != MouseButton.PRIMARY) {
            // This is a Right click event. Copy the most
            // recently grabbedPiece to the new square.
            if (clickedOnBoard(e.getX(), e.getY())) {
                Point boardPos = getBoardPosition(e.getX(), e.getY());
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

        // We don't have to draw the grabbed piece any more,
        // if we have been doing that.
        grabbedPiece.setDrawImage(false);

        // (clickedOnBoard, in this case, means "mouse-button released on board").
        if (clickedOnBoard(e.getX(), e.getY())) {
            Point boardPos = getBoardPosition(e.getX(), e.getY());
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECESELECTOR) {
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
                    updateCanvas(); // no board-change
                }
            }
            // Mouse released on the board, but no piece has been grabbed -> Do nothing.
        } else if (clickedOnPieceSelector(e.getX(), e.getY())) {
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECESELECTOR) {
                    // The piece was both grabbed and released in the pieceSelector.
                    if (grabbedPiece.getPiece() != getSelectedPiece(e.getX(), e.getY())) {
                        // Grabbed at one piece in the pieceSelctor and released on another
                        // piece in the pieceSelector.
                        grabbedPiece.reset();
                    }
                    // Remove possible dragged piece-image from the canvas.
                    updateCanvas();
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
                if (grabbedPiece.grabbedFrom == PIECESELECTOR) {
                    // The piece was grabbed in the pieceSelector and released outside.
                    grabbedPiece.reset();
                    // Remove possible dragged piece-image from the canvas.
                    updateCanvas();
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

    public void addListener(EnterPosBoardListener toAdd) {
        enterPosBoardListeners.add(toAdd);
    }

    public void notifyBoardChange() {
        for (EnterPosBoardListener epbl : enterPosBoardListeners)
            epbl.boardChanged();
    }

    /////////////////////////////////////////////////////////////
    // Some wrapper methods around the internal Board instance.//
    /////////////////////////////////////////////////////////////
        
    public void resetToStartingPosition() {
        board.resetToStartingPosition();
        grabbedPiece.full_reset();
        updateCanvas();
    }

    public void clearBoard() {
        board.clear();
        grabbedPiece.full_reset();              
        updateCanvas();
    }

    public void copyBoard(Board b) {
        board.copy(b);
        grabbedPiece.full_reset();              
        updateCanvas();
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

    public void setTurn(boolean b) {
        board.turn = b;
    }

    public Board makeBoardCopy() {
        return board.makeCopy();
    }
}
