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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;

import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.BLACK;

public class Controller_UI {

    private final Model_JFXChess model;

    public Controller_UI(Model_JFXChess model) {
        this.model = model;
    }

    public ActionListener switchLaf(String laf) {
        return e -> { model.setLookAndFeel(laf); };
    }

    public ActionListener showAbout() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogAbout dlg = new DialogAbout(model.mainFrameRef, model.getVersion());
            dlg.setVisible(true);
            model.setShortcutsEnabled(true);
        };
    }


    public ActionListener switchBoardColor(int bColor) {
        return e -> {
            model.setBoardColor(bColor);
        };
    }

    public ActionListener switchPieceStyle(int pStyle) {
        return e -> {
            model.setPieceStyle(pStyle);
        };
    }

    public ActionListener copyFenToClipboard() {
        return e -> {
            String fen = model.getGame().getCurrentNode().getBoard().fen();
            StringSelection stringSelection = new StringSelection(fen);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        };
    }

    public ActionListener copyPgnToClipboard() {
        return e -> {
            PgnPrinter pgnPrinter = new PgnPrinter();
            String pgn = pgnPrinter.printGame(model.getGame());
            StringSelection stringSelection = new StringSelection(pgn);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        };
    }


    private BufferedImage renderToImage(int width, int height) {

        PieceImageProvider pieceImageProvider = new PieceImageProvider();

        BufferedImage image =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();

        // set colors for rendering
        // light square color: white
        // dark square color: black
        Color darkSquareColor = new Color(192, 192, 192, 255);
        Color lightSquareColor = new Color(255, 255, 255, 255);
        Color borderColor = new Color(100, 100, 100, 255);

        // Better quality
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // fill background
        g2.setColor(darkSquareColor);
        g2.fillRect(0, 0, width, height);

        double minWidthHeight = Math.min(width, height);

        int outerMargin = 0;
        int boardSize = (int) (minWidthHeight - (2 * outerMargin));

        int xOffset = outerMargin;
        if (width > height) {
            int surplus = width - height;
            xOffset += surplus / 2;
        }

        int borderMargin = 18;

        int squareSize = ((boardSize - (2 * borderMargin)) / 8);
        int innerXOffset = (xOffset + borderMargin);
        int innerYOffset = (outerMargin + borderMargin);

        // paint board border
        g2.setColor(borderColor);
        g2.fillRect(
                xOffset,
                outerMargin,
                (squareSize * 8) + (borderMargin * 2),
                (squareSize * 8) + (borderMargin * 2)
        );

        // paint squares
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {

                Color fieldColor;

                if ((j % 2 == 0 && i % 2 == 1) || (j % 2 == 1 && i % 2 == 0)) {
                    fieldColor = model.getFlipBoard()
                            ? darkSquareColor
                            : lightSquareColor;
                } else {
                    fieldColor = model.getFlipBoard()
                            ? lightSquareColor
                            : darkSquareColor;
                }

                int x = innerXOffset + (i * squareSize);
                if (model.getFlipBoard()) {
                    x = innerXOffset + ((7 - i) * squareSize);
                }

                int y = innerYOffset + ((7 - j) * squareSize);

                g2.setColor(fieldColor);
                g2.fillRect(x, y, squareSize, squareSize);
            }
        }

        // draw coordinates
        g2.setColor(model.getBoardStyle().getCoordinateColor());

        for (int i = 0; i < 8; i++) {

            if (model.getFlipBoard()) {
                char ch = (char) (65 + (7 - i));
                String idx = Character.toString(ch);
                String num = Integer.toString(i + 1);

                g2.drawString(
                        idx,
                        innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8))
                );

                g2.drawString(
                        num,
                        xOffset + 5,
                        innerYOffset + (i * squareSize) + (squareSize / 2) + 4
                );

            } else {

                char ch = (char) (65 + i);
                String idx = Character.toString(ch);
                String num = Integer.toString(8 - i);

                g2.drawString(
                        idx,
                        innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        (int) (innerYOffset + (8 * squareSize) + (borderMargin * 0.8))
                );

                g2.drawString(
                        num,
                        xOffset + 5,
                        innerYOffset + (i * squareSize) + (squareSize / 2) + 4
                );
            }
        }

        // draw pieces
        Board b = model.getGame().getCurrentNode().getBoard();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {

                int x = model.getFlipBoard()
                        ? innerXOffset + ((7 - i) * squareSize)
                        : innerXOffset + (i * squareSize);

                int y = innerYOffset + ((7 - j) * squareSize);

                int piece = model.getFlipBoard()
                        ? b.getPieceAt(i, 7 - j)
                        : b.getPieceAt(i, j);

                if (piece != EMPTY && piece != FRINGE) {

                    Image pieceImage =
                            pieceImageProvider.getImage(
                                    piece,
                                    (int) (squareSize),
                                    model.getBoardStyle().getPieceStyle()
                            );

                    g2.drawImage(pieceImage, x, y, squareSize, squareSize, null);
                }
            }
        }

        // side to move marker
        int xSide = innerXOffset + 8 * squareSize + 6;
        int ySide = innerYOffset + 8 * squareSize + 6;

        if (b.turn == WHITE && model.getFlipBoard()) {
            ySide = innerYOffset - 11;
        }

        if (b.turn == BLACK && !model.getFlipBoard()) {
            ySide = innerYOffset - 11;
        }

        g2.setColor(lightSquareColor);
        g2.fillRect(xSide, ySide, 4, 4);

        g2.dispose();

        return image;
    }


    public ActionListener copyBitmapToClipboard() {
        return e -> {
            BufferedImage image = renderToImage(500,500);
            ImageSelection selection = new ImageSelection(image);
            Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
        };
    }

    public ActionListener editGameData() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogEditGameData dialog =
                    new DialogEditGameData(model.mainFrameRef, model.getGame().getPgnHeaders());
            dialog.setVisible(true);
            model.setShortcutsEnabled(true);
            if(dialog.isConfirmed()) {
                model.setPgnHeaders(dialog.getData());
            }
        };
    }

    public String readTextFromClipboard() {

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    return (String) contents.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException | IOException e) {
                    return null;
                }
        }
        return null;
    }

    public ActionListener pasteFenOrGame() {
        return e -> {
            String pasteString = null;
            pasteString = readTextFromClipboard();
            if (!model.isBlockGUI()) {
                // check if this is a fen
                try {
                    Board board = new Board(pasteString);
                    if (board.isConsistent()) {
                        Game g = new Game();
                        g.getRootNode().setBoard(board);
                        model.setGame(g);
                    }
                } catch (IllegalArgumentException ex) {
                    // if not a fen string, maybe it's a full game
                    PgnReader reader = new PgnReader();
                    Game g = reader.readGame(pasteString);
                    // as a heuristic we assume it's really a pasted game string if either there is at least
                    // two game nodes, or if there is a fen string for the root board
                    if (g.getRootNode().hasChild() || !g.getRootNode().getBoard().isInitialPosition()) {
                        model.setGame(g);
                    }
                }
            }

        };
    }

    public ActionListener deleteComment(int nodeId) {
        return e -> {
            model.setComment(nodeId, "");
        };
    }

    public ActionListener addMoveAnnotation(int nodeId, int nag) {
        return e -> {
            model.addNag(nodeId, nag);
        };
    }

    public ActionListener removeMoveAnnotations(int nodeId) {
        return e -> {
            model.removeMoveAnnotations(nodeId);
        };
    }

    public ActionListener addPosAnnotation(int nodeId, int nag) {
        return e -> {
            model.addNag(nodeId, nag);
        };
    }

    public ActionListener removePosAnnotations(int nodeId) {
        return e -> {
            model.removePosAnnotations(nodeId);
        };
    }

    public ActionListener removeMoveAndPosAnnotations(int nodeId) {
        return e -> {
            model.removeMoveAndPosAnnotation(nodeId);
        };
    }

    public ActionListener moveVariantUp(int nodeId) {
        return e -> {
            model.moveVariantUp(nodeId);
        };
    }

    public ActionListener moveVariantDown(int nodeId) {
        return e -> {
            model.moveVariantDown(nodeId);
        };
    }

    public ActionListener deleteVariant(int nodeId) {
        return e -> {
            model.deleteVariant(nodeId);
        };
    }

    public ActionListener deleteFromHere(int nodeId) {
        return e -> {
            model.deleteFromHere(nodeId);
        };
    }

    public ActionListener deleteAllComments() {
        return e -> {
            model.deleteAllComments();
        };
    }

    public ActionListener deleteAllVariants() {
        return e -> {
            model.deleteAllVariants();
        };
    }

    public ActionListener flipBoard() {
        return e -> {
            model.setFlipBoard(!model.getFlipBoard());
        };
    }

    public ActionListener selectBookFile() {
        return e -> {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter binFilter = new FileNameExtensionFilter("Extended Polyglot Book", "bin");
            chooser.setFileFilter(binFilter);
            chooser.setAcceptAllFileFilterUsed(true);
            try {
                int result = chooser.showOpenDialog(model.mainFrameRef);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = chooser.getSelectedFile();
                    if (selectedFile != null &&
                            selectedFile.exists() &&
                            selectedFile.canRead()
                    ) {
                        model.setBook(selectedFile);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Error reading file.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    public ActionListener setupNewPosition() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogSetupPosition dialog = new DialogSetupPosition(model.mainFrameRef, model);
            dialog.setVisible(true);
            model.setShortcutsEnabled(true);
            if(dialog.isConfirmed()) {
                Board newBoard = dialog.getCurrentBoard();
                Game g = new Game();
                g.getRootNode().setBoard(newBoard);
                model.setGame(g);
            }
        };
    }

    public ActionListener printGame() {
        return e -> {
            PgnPrinter pgnPrinter = new PgnPrinter();
            String pgn = pgnPrinter.printGame(model.getGame());
            JTextArea textArea = new JTextArea(pgn);
            try {
                boolean printed = textArea.print();
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        };
    }

    public ActionListener printFen() {
        return e -> {
            String fen = model.getGame().getCurrentNode().getBoard().fen();
            JTextArea textArea = new JTextArea(fen);
            try {
                boolean printed = textArea.print();
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        };
    }

    public ActionListener goToHomepage() {
        return e -> {
            try {
                String url = "https://asdfjkl.github.io/jfxchess";
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }

        };
    }

    public ActionListener editComment() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogEditComment dlgComment = new DialogEditComment(model.mainFrameRef, "Edit Comment");
            model.setShortcutsEnabled(true);
            GameNode currentNode = model.getGame().getCurrentNode();
            dlgComment.setText(currentNode.getComment());
            dlgComment.setVisible(true);
            if(dlgComment.isConfirmed()) {
                // filter text elements that must not appear in PGN comment, like { } etc.
                String newComment = dlgComment.getText();
                newComment = newComment.replace('\n', ' ');
                newComment = newComment.replace('{', ' ');
                newComment = newComment.replace('}', ' ');
                newComment = newComment.replace('\r', ' ');
                model.setComment(newComment);
            }
        };
    }
}
