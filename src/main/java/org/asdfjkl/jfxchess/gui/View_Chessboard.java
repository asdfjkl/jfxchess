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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import org.asdfjkl.jfxchess.lib.Arrow;
import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.ColoredField;
import org.asdfjkl.jfxchess.lib.Move;

import static java.awt.event.MouseEvent.*;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

public class View_Chessboard extends JPanel
        implements PropertyChangeListener {

    private final Model_JFXChess model;
    private final Controller_UI controller_UI;
    private final Controller_Board controller_Board;

    final double outputScaleX = HighDPIHelper.getUIScaleFactor();

    int innerXOffset;
    int innerYOffset;
    int squareSize;

    final PieceImageProvider pieceImageProvider = new PieceImageProvider();

    Point moveSource;
    final GrabbedPiece grabbedPiece = new GrabbedPiece();
    boolean drawGrabbedPiece = false;

    Point colorClickSource;

    Arrow grabbedArrow;
    boolean drawGrabbedArrow = false;

    Color lastMoveColor;
    Color arrowColor;
    Color arrowGrabColor;
    Color coloredFieldColor;
    boolean backgroundNeedsRefresh = true;

    private BufferedImage bufferedBackground;

    public View_Chessboard(Model_JFXChess model,
                           Controller_UI controller_UI,
                           Controller_Board controller_Board) {

        setPreferredSize(new Dimension(500, 500));

        this.model = model;
        this.model.addListener(this);

        this.controller_UI = controller_UI;
        this.controller_Board = controller_Board;

        grabbedPiece.setPiece(-1);
        moveSource = new Point(-1,-1);
        colorClickSource = new Point(-1,-1);
        grabbedArrow = new Arrow();
        grabbedArrow.xFrom = grabbedArrow.yFrom = grabbedArrow.xTo = grabbedArrow.yTo = -1;
        lastMoveColor = new Color(200,200,0,102);
        arrowColor = new Color(50,88,0,255);
        arrowGrabColor = new Color(70,130,0, 255);
        coloredFieldColor = new Color(200,0,0,102);
        moveSource = new Point(-1,-1);

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

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                backgroundNeedsRefresh = true;
                revalidate();
                repaint();
            }
        });
    }

    public void refreshBackground() {

        if(bufferedBackground != null && !backgroundNeedsRefresh) {
            return;
        }

        bufferedBackground = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = bufferedBackground.createGraphics();

        // fill background
        g2.setColor(model.getBoardStyle().getDarkSquareColor());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // setup font size
        //FontMetrics fm = g2.getFontMetrics();
        //int x = (getWidth() - fm.stringWidth(text)) / 2;
        //int y = (getHeight() + fm.getAscent()) / 2;

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
        g2.setColor(model.getBoardStyle().getBorderColor());
        g2.fillRect(xOffset, outerMargin, (squareSize*8)+(borderMargin*2), (squareSize*8)+(borderMargin*2));

        // get the from and to field of the last move
        // to highlight those squares
        Point lastMoveFrom = null;
        Point lastMoveTo = null;
        if(model.getGame().getCurrentNode().getMove() != null) {
            Move m = model.getGame().getCurrentNode().getMove();
            lastMoveFrom = Board.internalToXY(m.getMoveSourceSquare());
            lastMoveTo = Board.internalToXY(m.getMoveTargetSquare());
        }

        // paint squares
        Color fieldColor;
        for(int i=0;i<8;i++) {
            for(int j=0;j<8;j++) {
                if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                    if(!model.getFlipBoard()) {
                        fieldColor = model.getBoardStyle().getLightSquareColor();
                    } else {
                        fieldColor = model.getBoardStyle().getDarkSquareColor();
                    }
                } else {
                    if(!model.getFlipBoard()) {
                        fieldColor = model.getBoardStyle().getDarkSquareColor();
                    } else {
                        fieldColor = model.getBoardStyle().getLightSquareColor();
                    }
                }
                int x = (innerXOffset) + (i*squareSize);
                if(model.getFlipBoard()) {
                    x = innerXOffset+((7-i)*squareSize);
                }
                int y = (innerYOffset) + ((7-j)*squareSize);

                g2.setColor(fieldColor);
                g2.fillRect(x,y,squareSize,squareSize);

                if(lastMoveFrom != null && lastMoveTo != null) {
                    boolean markField = false;
                    if(!model.getFlipBoard()) {
                        if ((lastMoveFrom.getX() == i && lastMoveFrom.getY() == j) ||
                                (lastMoveTo.getX() == i && lastMoveTo.getY() == j)) {
                            markField = true;
                        }
                    }
                    if(model.getFlipBoard()) {
                        if ((lastMoveFrom.getX() == i && lastMoveFrom.getY() == 7 - j) ||
                                (lastMoveTo.getX() == i && lastMoveTo.getY() == 7 - j)) {
                            markField = true;
                        }
                    }
                    if(markField) {
                        g2.setColor(lastMoveColor);
                        g2.fillRect(x, y, squareSize, squareSize);
                    }
                }
            }
        }



        // draw the board coordinates
        g2.setColor(model.getBoardStyle().getCoordinateColor());
        for(int i=0;i<8;i++) {
            if(model.getFlipBoard()){
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
        Board b = model.getGame().getCurrentNode().getBoard();
        for(int i=0;i<8;i++) {
            for (int j = 0; j < 8; j++) {
                int x;
                if(model.getFlipBoard()) {
                    x = innerXOffset+((7-i)*squareSize);
                } else {
                    x = innerXOffset+(i*squareSize);
                }
                // drawing coordinates are from top left
                // whereas chess coords are from bottom left
                int y = innerYOffset+((7-j)*squareSize);
                int piece = 0;
                if(model.getFlipBoard()) {
                    piece = b.getPieceAt(i, 7-j);
                } else {
                    piece = b.getPieceAt(i, j);
                }
                if(piece != EMPTY && piece != FRINGE) {
                    if(!model.getFlipBoard()) {
                        if (!(drawGrabbedPiece && i == moveSource.x && j == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    model.getBoardStyle().getPieceStyle());
                            g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                        }
                    } else {
                        if (!(drawGrabbedPiece && i == moveSource.x && (7-j) == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    model.getBoardStyle().getPieceStyle());
                            g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                        }
                    }
                }
            }
        }

        // mark side to move
        int x_side_to_move = innerXOffset + 8 * squareSize + 6;
        int y_side_to_move = innerYOffset + 8 * squareSize + 6;
        if(b.turn == WHITE) {
            if(model.getFlipBoard()) {
                y_side_to_move = innerYOffset - 11;
            }
        }
        if(b.turn == BLACK) {
            if(!model.getFlipBoard()) {
                y_side_to_move = innerYOffset - 11;
            }
        }
        g2.setColor(model.getBoardStyle().getLightSquareColor());
        g2.fillRect(x_side_to_move, y_side_to_move, 4,4);
        // }

        g2.dispose();

        backgroundNeedsRefresh = false;
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);


        // fill background
        Graphics2D g2 = (Graphics2D) g;
        //g2.scale(outputScaleX, outputScaleX);
        refreshBackground();
        g2.drawImage(bufferedBackground, 0, 0, null);

        // paint colored fields
        for(ColoredField coloredField : model.getGame().getCurrentNode().getColoredFields()) {

            int i = coloredField.x;
            int j = coloredField.y;

            int x = (innerXOffset) + (i*squareSize);
            int y = (innerYOffset) + ((7-j)*squareSize);
            if(model.getFlipBoard()) {
                x = innerXOffset+((7-i)*squareSize);
                y = (innerYOffset) + (j*squareSize);
            }

            g2.setColor(coloredFieldColor);
            g2.fillRect(x,y,squareSize,squareSize);
        }

        // draw grabbed piece
        if(drawGrabbedPiece) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(),
                    (int) (squareSize * this.outputScaleX), model.getBoardStyle().getPieceStyle());
            g2.drawImage(pieceImage, (int) (grabbedPiece.getCurrentXLocation() - offset),
                    (int) (grabbedPiece.getCurrentYLocation() - offset), squareSize, squareSize, null);
        }

        // draw arrows
        ArrayList<Arrow> arrows = model.getGame().getCurrentNode().getArrows();
        if(arrows != null) {
            for (Arrow ai : model.getGame().getCurrentNode().getArrows()) {
                drawArrow(g2, ai, arrowColor, innerXOffset, innerYOffset);
            }
        }

        // draw currently grabbed arrow
        if(drawGrabbedArrow
                && grabbedArrow.xFrom != -1 && grabbedArrow.yFrom != -1
                && grabbedArrow.xTo != -1 && grabbedArrow.yTo != -1
                && ((grabbedArrow.xFrom != grabbedArrow.xTo) || (grabbedArrow.yFrom != grabbedArrow.yTo))) {
            drawArrow(g2, grabbedArrow, arrowGrabColor, innerXOffset, innerYOffset);
        }

        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, Arrow arrow, Color color, int boardOffsetX, int boardOffsetY) {

        g2.fillRect(50, 50, 4,4);

        int xFrom = 0;
        int xTo = 0;
        int yFrom = 0;
        int yTo = 0;
        if(model.getFlipBoard()) {
            xFrom = boardOffsetX+((7-arrow.xFrom)*squareSize) + (squareSize/2);
            xTo = boardOffsetX+((7-arrow.xTo)*squareSize) + (squareSize/2);
            yFrom = boardOffsetY+(arrow.yFrom*squareSize)+ (squareSize/2);
            yTo = boardOffsetY+(arrow.yTo*squareSize)+ (squareSize/2);
        } else {
            xFrom = boardOffsetX+(arrow.xFrom*squareSize)+ (squareSize/2);
            xTo = boardOffsetX+(arrow.xTo*squareSize)+ (squareSize/2);
            yFrom = boardOffsetY+((7-arrow.yFrom)*squareSize)+ (squareSize/2);
            yTo = boardOffsetY+((7-arrow.yTo)*squareSize)+ (squareSize/2);
        }

        // incredible annoying calculation to get arrow head
        Point fromPoint = new Point(xFrom, yFrom);
        Point toPoint = new Point(xTo, yTo);

        // added to toPoint to place arrow head
        // somewhere in the center
        double vx = -toPoint.getX() + fromPoint.getX();
        double vy = -toPoint.getY() + fromPoint.getY();

        // vectors correspond to the arrows
        double dx = toPoint.getX() - fromPoint.getX();
        double dy = toPoint.getY() - fromPoint.getY();

        double length = Math.sqrt(dx * dx + dy * dy);

        double unitDx = dx / length;
        double unitDy = dy / length;

        // adjusted according to arrow length
        vx = vx * ((double) squareSize /6 /length);
        vy = vy * ((double) squareSize /6 /length);

        toPoint = new Point((int) (toPoint.getX() - vx), (int) (toPoint.getY() - vy));

        int arrowHeadBoxSize = squareSize/4;
        Point arrowPoint1 = new Point(
                (int) (toPoint.getX() - unitDx * arrowHeadBoxSize - unitDy * arrowHeadBoxSize),
                (int) (toPoint.getY() - unitDy * arrowHeadBoxSize + unitDx * arrowHeadBoxSize));

        Point arrowPoint2 = new Point(
                (int) (toPoint.getX() - unitDx * arrowHeadBoxSize + unitDy * arrowHeadBoxSize),
                (int) (toPoint.getY() - unitDy * arrowHeadBoxSize - unitDx * arrowHeadBoxSize));

        g2.setColor(color);
        g2.fillPolygon( new int[] { toPoint.x, arrowPoint1.x, arrowPoint2.x },
                new int[] { toPoint.y, arrowPoint1.y, arrowPoint2.y },
                3);

        // take the old center coordinates to draw the
        // line to, so that the line does not
        // cover the arrow head due to the line's thickness
        Point to = new Point(xTo, yTo);
        g2.setStroke(new BasicStroke(squareSize/6));
        g2.drawLine(fromPoint.x, fromPoint.y, to.x, to.y);
    }

    Point getBoardPosition(double x, double y) {

        if(x > innerXOffset && y > innerYOffset
                && x < (innerXOffset + 8*squareSize)
                && y < (innerYOffset + 8*squareSize)) {

            int i = (int) x - innerXOffset;
            int j = (int) y - innerYOffset;

            if(model.getFlipBoard()) {
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

    void touchPiece(int boardX, int boardY, double currentXLocation, double currentYLocation) {

        moveSource = new Point();
        moveSource.x = boardX;
        moveSource.y = boardY;

        grabbedPiece.setCurrentXLocation(currentXLocation);
        grabbedPiece.setCurrentYLocation(currentYLocation);
        grabbedPiece.setPiece(model.getGame().getCurrentNode().getBoard().getPieceAt(boardX,boardY));
        drawGrabbedPiece = true;
        backgroundNeedsRefresh = true;
    }


    private void handleMousePress(MouseEvent e) {

        if(!(model.isBlockGUI())) {
            int mouseButton = e.getButton();
            if (mouseButton == BUTTON1) {
                Board b = model.getGame().getCurrentNode().getBoard();
                Point boardPos = getBoardPosition(e.getX(), e.getY());
                // case (a) 1) user clicks source field, then 2) clicks destination
                // case (b) user clicks and drags piece
                if (boardPos != null) {
                    if (grabbedPiece.getPiece() != -1) {
                        // case a) 2)
                        Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                        if (b.isLegalAndPromotes(m)) {
                            model.setShortcutsEnabled(false);
                            DialogPromotion dlgProm = new DialogPromotion(
                                    model.mainFrameRef,
                                    "Promotion",
                                    b.turn,
                                    model.getBoardStyle().getPieceStyle()
                            );
                            dlgProm.setVisible(true);
                            model.setShortcutsEnabled(true);
                            int promotionPiece = dlgProm.getSelectedPiece();
                            if (promotionPiece != EMPTY) {
                                m.setPromotionPiece(promotionPiece);
                                controller_Board.applyMove(m);
                            }
                            resetMove();
                        } else if (b.isLegal(m)) {
                            controller_Board.applyMove(m);
                            resetMove();
                        } else {
                            resetMove();
                            if (b.isPieceAt(boardPos.x, boardPos.y)) {
                                touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                            }
                        }
                    } else { // case a) 1) or b)
                        if (b.isPieceAt(boardPos.x, boardPos.y)) {
                            touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                        }
                    }
                }
            }
            if (mouseButton == BUTTON3) {
                Point boardCoordinate = getBoardPosition(e.getX(), e.getY());
                handleRightClick(boardCoordinate);
            }
        }
        backgroundNeedsRefresh = true;
    }



    private void handleMouseDragged(MouseEvent me) {

        if(drawGrabbedPiece && grabbedPiece.getPiece() != -1) {
            grabbedPiece.setCurrentXLocation(me.getX());
            grabbedPiece.setCurrentYLocation(me.getY());
            repaint();
        }
        if(drawGrabbedArrow) {
            Point xy = getBoardPosition(me.getX(), me.getY());
            if(xy != null) {
                grabbedArrow.xTo = xy.x;
                grabbedArrow.yTo = xy.y;
                repaint();
            }
        }
    }

    private void handleMouseReleased(MouseEvent me) {

        int mouseButton = me.getButton();
        if(mouseButton == BUTTON1) {
            drawGrabbedPiece = false;
            Point boardPos = getBoardPosition(me.getX(), me.getY());
            Board b = model.getGame().getCurrentNode().getBoard();
            if (boardPos != null && grabbedPiece.getPiece() != -1) {
                if (!(boardPos.x == moveSource.x && boardPos.y == moveSource.y)) {
                    Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                    if (b.isLegalAndPromotes(m)) {
                        model.setShortcutsEnabled(false);
                        DialogPromotion dlgPromotion = new DialogPromotion(model.mainFrameRef,
                                "Promotion",
                                b.turn,
                                model.getBoardStyle().getPieceStyle()
                        );
                        dlgPromotion.setVisible(true);
                        model.setShortcutsEnabled(true);
                        if(dlgPromotion.getSelectedPiece() != EMPTY) {
                            m.setPromotionPiece(dlgPromotion.getSelectedPiece());
                            controller_Board.applyMove(m);
                        }
                        resetMove();
                    } else if (b.isLegal(m)) {
                        controller_Board.applyMove(m);
                    } else {
                        resetMove();
                    }

                }
            }
        }
        if(mouseButton == BUTTON3) {
            Point boardCoordinate = getBoardPosition(me.getX(), me.getY());
            handleRightClickRelease(boardCoordinate);
        }
        backgroundNeedsRefresh = true;
        repaint();
    }

    private void handleRightClickRelease(Point boardCoordinate) {
        // user clicked and is going to draw arrow
        if(boardCoordinate != null) {
            // arrow case
            if (boardCoordinate.x != colorClickSource.x || boardCoordinate.y != colorClickSource.y) {
                Arrow a = new Arrow();
                a.xFrom = colorClickSource.x;
                a.yFrom = colorClickSource.y;
                a.xTo = boardCoordinate.x;
                a.yTo = boardCoordinate.y;
                controller_Board.addOrRemoveArrow(a);
            } else { // just marking a field
                ColoredField c = new ColoredField();
                c.x = boardCoordinate.x;
                c.y = boardCoordinate.y;
                controller_Board.addOrRemoveColoredField(c);
            }
            drawGrabbedArrow = false;
        }
    }


    private void handleRightClick(Point boardCoordinate) {
        // user clicked and is going to draw arrow or mark a field
        if(boardCoordinate != null) {
            colorClickSource.x = boardCoordinate.x;
            colorClickSource.y = boardCoordinate.y;

            grabbedArrow.xFrom = boardCoordinate.x;
            grabbedArrow.yFrom = boardCoordinate.y;
            grabbedArrow.xTo = boardCoordinate.x;
            grabbedArrow.yTo = boardCoordinate.y;
            drawGrabbedArrow = true;
        }
    }

    void resetMove() {

        moveSource.x = -1;
        moveSource.y = -1;
        grabbedPiece.setPiece(-1);
        drawGrabbedPiece = false;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("boardFlipped".equals(evt.getPropertyName())) {
            backgroundNeedsRefresh = true;
            repaint();
        }
        if ("pieceStyle".equals(evt.getPropertyName()) ||
                "boardColor".equals(evt.getPropertyName())) {
            backgroundNeedsRefresh = true;
            repaint();
        }
        if ("currentGameNodeChanged".equals(evt.getPropertyName())) {
            backgroundNeedsRefresh = true;
            repaint();
        }
        if ("gameChanged".equals(evt.getPropertyName())) {
            backgroundNeedsRefresh = true;
            repaint();
        }
    }
}
