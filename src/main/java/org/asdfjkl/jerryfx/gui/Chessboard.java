package org.asdfjkl.jerryfx.gui;

import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Move;

import java.awt.*;

import static org.asdfjkl.jerryfx.gui.BoardStyle.PIECE_STYLE_MERIDA;
import static org.asdfjkl.jerryfx.lib.CONSTANTS.*;

public class Chessboard extends Canvas implements StateChangeListener {

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

    public Chessboard(GameModel gameModel) {

        this.boardStyle = new BoardStyle();
        this.gameModel = gameModel;
        this.pieceImageProvider = new PieceImageProvider();
        this.outputScaleX = Screen.getPrimary().getOutputScaleX();
        this.grabbedPiece.setPiece(-1);
        this.moveSource = new Point(-1,-1);

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

        updateCanvas();
    }

    public void updateCanvas() {

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
        Board b = gameModel.getGame().getCurrentNode().getBoard();
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
                    piece = b.getPieceAt(i, 7-j);
                } else {
                    piece = b.getPieceAt(i, j);
                }
                if(piece != EMPTY && piece != FRINGE) {
                    if(!flipBoard) {
                        if (!(drawGrabbedPiece && i == moveSource.x && j == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    boardStyle.getPieceStyle());
                            gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                        }
                    } else {
                        if (!(drawGrabbedPiece && i == moveSource.x && (7-j) == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    boardStyle.getPieceStyle());
                            gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                        }
                    }
                }
            }
        }

        // draw grabbed piece
        if(drawGrabbedPiece) {
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

    void touchPiece(int boardX, int boardY, double currentXLocation, double currentYLocation) {

        moveSource = new Point();
        moveSource.x = boardX;
        moveSource.y = boardY;

        grabbedPiece.setCurrentXLocation(currentXLocation);
        grabbedPiece.setCurrentYLocation(currentYLocation);
        grabbedPiece.setPiece(gameModel.getGame().getCurrentNode().getBoard().getPieceAt(boardX,boardY));
        drawGrabbedPiece = true;
    }

    void handleMousePress(MouseEvent e) {

        Board b = this.gameModel.getGame().getCurrentNode().getBoard();
        Point boardPos = getBoardPosition(e.getX(), e.getY());
        // case a) 1) user clicks source field, then 2) clicks destination
        // case b) user clicks and drags piece
        if(boardPos != null) {
            if(grabbedPiece.getPiece() != -1) {
                // case a) 2)
                Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                if (b.isLegalAndPromotes(m)) {
                    int promotionPiece = DialogPromotion.show(b.turn, boardStyle.getPieceStyle());
                    if(promotionPiece != EMPTY) {
                        m.setPromotionPiece(promotionPiece);
                        applyMove(m);
                    }
                    resetMove();
                } else if (b.isLegal(m)) {
                    applyMove(m);
                    resetMove();
                } else {
                    resetMove();
                    if (b.isPieceAt(boardPos.x, boardPos.y)) {
                        touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                    }
                }
            } else { // case a) 1) or b)
                if(b.isPieceAt(boardPos.x, boardPos.y)) {
                    touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                }
            }
        }
    }

    void handleMouseDragged(MouseEvent e) {

        if(drawGrabbedPiece && grabbedPiece.getPiece() != -1) {
            grabbedPiece.setCurrentXLocation(e.getX());
            grabbedPiece.setCurrentYLocation(e.getY());
            updateCanvas();
        }
    }

    void handleMouseRelease(MouseEvent e) {

        drawGrabbedPiece = false;
        Point boardPos = getBoardPosition(e.getX(), e.getY());
        Board b = gameModel.getGame().getCurrentNode().getBoard();
        if(boardPos != null && grabbedPiece.getPiece() != -1) {
            if(!(boardPos.x == moveSource.x && boardPos.y == moveSource.y)) {
                Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                System.out.println(m.getUci());
                System.out.println(("legal and promotes: " + b.isLegalAndPromotes(m)));
                if(b.isLegalAndPromotes(m)) {
                    int promotionPiece = DialogPromotion.show(b.turn, boardStyle.getPieceStyle());
                    if(promotionPiece != EMPTY) {
                        m.setPromotionPiece(promotionPiece);
                        applyMove(m);
                    }
                    resetMove();
                } else if(b.isLegal(m)) {
                    applyMove(m);
                } else {
                    resetMove();
                }

            }
        }
        updateCanvas();
    }

    void applyMove(Move m) {

        this.gameModel.getGame().applyMove(m);
        // trigger statechange, would trigger updateCanvas itself
        this.gameModel.triggerStateChange();
        //this.updateCanvas();
    }

    void resetMove() {

        moveSource.x = -1;
        moveSource.y = -1;
        grabbedPiece.setPiece(-1);
        drawGrabbedPiece = false;
    }

    @Override
    public void stateChange() {

        if(gameModel.getFlipBoard() != flipBoard) {
            flipBoard = gameModel.getFlipBoard();
        }
        updateCanvas();
    }

}
