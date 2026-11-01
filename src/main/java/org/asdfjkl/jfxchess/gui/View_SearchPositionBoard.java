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

import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.event.MouseEvent.BUTTON1;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

public class View_SearchPositionBoard extends JPanel {

    private final GrabbedPiece.GrabbedFrom PIECE_SELECTOR = GrabbedPiece.GrabbedFrom.PieceSelector;
    private final Model_JFXChess model;
    private final BoardStyle defaultBoardStyle = new BoardStyle();
    private final Board board;
    private final GrabbedPiece grabbedPiece = new GrabbedPiece();
    private final PieceImageProvider pieceImageProvider = new PieceImageProvider();
    private final double outputScaleX = HighDPIHelper.getUIScaleFactor();

    public final int[][] pickupPieces = {
            { CONSTANTS.WHITE_PAWN, CONSTANTS.BLACK_PAWN },
            { CONSTANTS.WHITE_KNIGHT, CONSTANTS.BLACK_KNIGHT },
            { CONSTANTS.WHITE_BISHOP, CONSTANTS.BLACK_BISHOP },
            { CONSTANTS.WHITE_ROOK, CONSTANTS.BLACK_ROOK },
            { CONSTANTS.WHITE_QUEEN, CONSTANTS.BLACK_QUEEN },
            { CONSTANTS.WHITE_KING, CONSTANTS.BLACK_KING },
            { CONSTANTS.WHITE_ANY_PIECE, CONSTANTS.BLACK_ANY_PIECE }
    };

    private int xOffset = 0;
    private int innerXOffset = 0;
    private int innerYOffset = 0;
    private final int borderMargin = 18;
    private int squareSize = 0;
    private int outerMargin = 0;
    public boolean flipBoard = false;

    public View_SearchPositionBoard() {
        this(null);
    }

    public View_SearchPositionBoard(Model_JFXChess model) {
        this.model = model;
        if (model != null) {
            this.flipBoard = model.getFlipBoard();
            if (model.getGame() != null && model.getGame().getCurrentNode() != null) {
                this.board = model.getGame().getCurrentNode().getBoard().makeCopy();
            } else {
                this.board = new Board(true);
            }
        } else {
            this.board = new Board(true);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                handleMousePress(me);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                handleMouseReleased(me);
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent me) {
                handleMouseDragged(me);
                repaint();
            }
        });
    }

    private BoardStyle getBoardStyle() {
        return (model != null) ? model.getBoardStyle() : defaultBoardStyle;
    }

    public void clearBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board.setPieceAt(i, j, CONSTANTS.EMPTY);
            }
        }
        grabbedPiece.reset();
        repaint();
    }

    public void resetToStartingPosition() {
        Board init = new Board(true);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board.setPieceAt(i, j, init.getPieceAt(i, j));
            }
        }
        board.turn = CONSTANTS.WHITE;
        grabbedPiece.reset();
        repaint();
    }

    public void copyBoard(Board b) {
        if (b == null) return;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board.setPieceAt(i, j, b.getPieceAt(i, j));
            }
        }
        board.turn = b.turn;
        grabbedPiece.reset();
        repaint();
    }

    public Board makeBoardCopy() {
        Board copy = new Board(false);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                copy.setPieceAt(i, j, board.getPieceAt(i, j));
            }
        }
        copy.turn = board.turn;
        return copy;
    }

    public Board getBoard() {
        return board;
    }

    public void setTurn(boolean turn) {
        board.turn = turn;
    }

    public boolean getTurn() {
        return board.turn;
    }

    private void updateBoard() {
        grabbedPiece.reset();
        repaint();
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
        if (mouseButton != BUTTON1) {
            // Right click event: copy most recently grabbedPiece to the square
            if (clickedOnBoard(me.getX(), me.getY())) {
                Point boardPos = getBoardPosition(me.getX(), me.getY());
                if (boardPos != null) {
                    board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.rightClickPiece());
                    updateBoard();
                }
            } else {
                grabbedPiece.setRightClickPiece(CONSTANTS.EMPTY);
            }
            return;
        }

        grabbedPiece.setDrawImage(false);

        if (clickedOnBoard(me.getX(), me.getY())) {
            Point boardPos = getBoardPosition(me.getX(), me.getY());
            if (boardPos != null && grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.getPiece());
                    updateBoard();
                } else if (boardPos.x != grabbedPiece.sourceSquareX() || boardPos.y != grabbedPiece.sourceSquareY()) {
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    board.setPieceAt(boardPos.x, boardPos.y, grabbedPiece.getPiece());
                    updateBoard();
                } else {
                    grabbedPiece.reset();
                    repaint();
                }
            }
        } else if (clickedOnPieceSelector(me.getX(), me.getY())) {
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    if (grabbedPiece.getPiece() != getSelectedPiece(me.getX(), me.getY())) {
                        grabbedPiece.reset();
                    }
                    repaint();
                } else {
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    updateBoard();
                }
            }
        } else {
            if (grabbedPiece.isGrabbed()) {
                if (grabbedPiece.grabbedFrom == PIECE_SELECTOR) {
                    grabbedPiece.reset();
                    repaint();
                } else {
                    board.setPieceAt(grabbedPiece.sourceSquareX(), grabbedPiece.sourceSquareY(), CONSTANTS.EMPTY);
                    updateBoard();
                }
            }
        }
    }

    private void handleMousePress(MouseEvent me) {
        int mouseButton = me.getButton();
        if (mouseButton != BUTTON1) {
            return;
        }
        if (clickedOnBoard(me.getX(), me.getY())) {
            Point boardPos = getBoardPosition(me.getX(), me.getY());
            if (boardPos != null && !grabbedPiece.isGrabbed()) {
                if (board.isPieceAt(boardPos.x, boardPos.y) ||
                        board.getPieceAt(boardPos.x, boardPos.y) == WHITE_ANY_PIECE ||
                        board.getPieceAt(boardPos.x, boardPos.y) == BLACK_ANY_PIECE) {
                    grabbedPiece.grab(boardPos.x,
                            boardPos.y,
                            me.getX(),
                            me.getY(),
                            board.getPieceAt(boardPos.x, boardPos.y));
                }
            }
        } else if (clickedOnPieceSelector(me.getX(), me.getY())) {
            grabbedPiece.grab(me.getX(),
                    me.getY(),
                    getSelectedPiece(me.getX(), me.getY()));
        }
    }

    boolean clickedOnBoard(double x, double y) {
        return x > innerXOffset && y > innerYOffset &&
                x < innerXOffset + (8 * squareSize) &&
                y < innerYOffset + (8 * squareSize);
    }

    boolean clickedOnPieceSelector(double x, double y) {
        int leftOffset = xOffset + 9 * squareSize + 3 * borderMargin;
        int downOffset = outerMargin + borderMargin;
        return x > leftOffset && y > downOffset &&
                x < leftOffset + 2 * squareSize &&
                y < downOffset + 7 * squareSize;
    }

    Point getBoardPosition(double x, double y) {
        if (clickedOnBoard(x, y)) {
            int i = (int) x - innerXOffset;
            int j = (int) y - innerYOffset;
            if (flipBoard) {
                i = 7 - (i / squareSize);
                j = j / squareSize;
            } else {
                i = i / squareSize;
                j = 7 - (j / squareSize);
            }
            if (i >= 0 && i < 8 && j >= 0 && j < 8) {
                return new Point(i, j);
            }
        }
        return null;
    }

    int getSelectedPiece(double x, double y) {
        int leftOffset = xOffset + 9 * squareSize + 3 * borderMargin;
        int downOffset = outerMargin + borderMargin;
        int xIdx = (int) (x - leftOffset) / squareSize;
        int yIdx = (int) (y - downOffset) / squareSize;
        if (yIdx >= 0 && yIdx < 7 && xIdx >= 0 && xIdx < 2) {
            return pickupPieces[yIdx][xIdx];
        }
        return CONSTANTS.EMPTY;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        BoardStyle boardStyle = getBoardStyle();

        g2.setColor(boardStyle.getDarkSquareColor());
        g2.fillRect(0, 0, getWidth(), getHeight());

        outerMargin = (int) (this.getWidth() * 0.05);
        double squareWidth = (this.getWidth() - (4 * borderMargin) - (2 * outerMargin)) / 11.0;
        double squareHeight = (this.getHeight() - (2 * borderMargin) - (2 * outerMargin)) / 8.0;

        squareSize = (int) Math.min(squareWidth, squareHeight);

        xOffset = outerMargin;
        innerXOffset = (xOffset + borderMargin);
        innerYOffset = (outerMargin + borderMargin);

        // paint board border
        g2.setColor(boardStyle.getBorderColor());
        g2.fillRect(xOffset, outerMargin, (squareSize * 8) + (borderMargin * 2), (squareSize * 8) + (borderMargin * 2));

        // paint squares
        Color fieldColor;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((j % 2 == 0 && i % 2 == 1) || (j % 2 == 1 && i % 2 == 0)) {
                    fieldColor = (!flipBoard) ? boardStyle.getLightSquareColor() : boardStyle.getDarkSquareColor();
                } else {
                    fieldColor = (!flipBoard) ? boardStyle.getDarkSquareColor() : boardStyle.getLightSquareColor();
                }
                int x = innerXOffset + (i * squareSize);
                if (flipBoard) {
                    x = innerXOffset + ((7 - i) * squareSize);
                }
                int y = innerYOffset + ((7 - j) * squareSize);

                g2.setColor(fieldColor);
                g2.fillRect(x, y, squareSize, squareSize);
            }
        }

        // draw board coordinates
        g2.setColor(boardStyle.getCoordinateColor());
        for (int i = 0; i < 8; i++) {
            if (flipBoard) {
                char ch = (char) ('A' + (7 - i));
                String idx = Character.toString(ch);
                String num = Integer.toString(i + 1);
                g2.drawString(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8)));
                g2.drawString(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            } else {
                char ch = (char) ('A' + i);
                String idx = Character.toString(ch);
                String num = Integer.toString(8 - i);
                g2.drawString(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8)));
                g2.drawString(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            }
        }

        // draw pieces on board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x;
                if (flipBoard) {
                    x = innerXOffset + ((7 - i) * squareSize);
                } else {
                    x = innerXOffset + (i * squareSize);
                }
                int y = innerYOffset + ((7 - j) * squareSize);
                int piece;
                if (flipBoard) {
                    piece = board.getPieceAt(i, 7 - j);
                } else {
                    piece = board.getPieceAt(i, j);
                }
                if (piece != EMPTY && piece != FRINGE) {
                    boolean isSource = (!flipBoard)
                            ? (grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() && j == grabbedPiece.sourceSquareY())
                            : (grabbedPiece.getDrawImage() && i == grabbedPiece.sourceSquareX() && (7 - j) == grabbedPiece.sourceSquareY());
                    if (!isSource) {
                        Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                boardStyle.getPieceStyle());
                        if (pieceImage != null) {
                            g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                        }
                    }
                }
            }
        }

        // draw piece selector panel (7 rows x 2 columns)
        g2.setColor(boardStyle.getBorderColor());
        g2.fillRect((xOffset + 9 * squareSize) + (borderMargin * 2),
                outerMargin,
                (squareSize * 2) + (borderMargin * 2),
                (squareSize * 7) + (borderMargin * 2));

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                g2.setColor(boardStyle.getLightSquareColor());
                g2.fillRect((xOffset + (9 + j) * squareSize) + (borderMargin * 3),
                        outerMargin + (i * squareSize) + borderMargin,
                        squareSize, squareSize);

                int pieceType = pickupPieces[i][j];
                Image pieceImage = pieceImageProvider.getImage(pieceType, (int) (squareSize * this.outputScaleX),
                        boardStyle.getPieceStyle());
                int x = (xOffset + (9 + j) * squareSize) + (borderMargin * 3);
                int y = outerMargin + (i * squareSize) + borderMargin;
                if (pieceImage != null) {
                    g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                }
            }
        }

        // draw grabbed piece
        if (grabbedPiece.getDrawImage()) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(), (int) (squareSize * this.outputScaleX),
                    boardStyle.getPieceStyle());
            if (pieceImage != null) {
                g2.drawImage(pieceImage, (int) (grabbedPiece.getCurrentXLocation() - offset),
                        (int) (grabbedPiece.getCurrentYLocation() - offset), squareSize, squareSize, null);
            }
        }
    }
}
