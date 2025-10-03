/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.text.*;

import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;

public class DialogEnterPosition implements EnterPosBoardListener {

    Stage stage;
    boolean accepted = false;

    EnterPosBoard enterPosBoard;

    final Label lblCastlingRights = new Label("Castling Rights");
    final CheckBox cbCastlesWK = new CheckBox("White O-O");
    final CheckBox cbCastlesWQ = new CheckBox("White O-O-O");
    final CheckBox cbCastlesBK = new CheckBox("Black O-O");
    final CheckBox cbCastlesBQ = new CheckBox("Black O-O-O");

    final Label lblEnPassant = new Label("En Passant Square");
    ComboBox<String> cboxEnPassant;

    final Label lblTurn = new Label("Turn");
    final RadioButton rbTurnWhite = new RadioButton("White");
    final RadioButton rbTurnBlack = new RadioButton("Black");

    final Button btnFlipBoard = new Button("Flip Board");
    final Button btnInitialPosition = new Button("Initial Position");
    final Button btnClearBoard = new Button("Clear Board");
    final Button btnCurrentPosition = new Button("Current Position");

    Button btnOk;
    Button btnCancel;

    Board originalBoard;

    public boolean show(Board board, BoardStyle style, double width, double height) {

        originalBoard = board.makeCopy();
        enterPosBoard = new EnterPosBoard(originalBoard);
        enterPosBoard.addListener(this);
        enterPosBoard.boardStyle = style;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        cboxEnPassant = new ComboBox<String>();
        cboxEnPassant.getItems().addAll("-",
                "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
                "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6");
        cboxEnPassant.setValue(enterPosBoard.getEnPassantSquare());

        ToggleGroup groupTurn = new ToggleGroup();
        rbTurnWhite.setToggleGroup(groupTurn);
        rbTurnBlack.setToggleGroup(groupTurn);

        btnOk = new Button();
        btnOk.setText("OK");

        btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbButtonsRight = new VBox();
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        Region spacer3 = new Region();
        Region spacer4 = new Region();

        btnInitialPosition.setMinWidth(140);
        btnFlipBoard.setMinWidth(140);
        btnClearBoard.setMinWidth(140);
        btnCurrentPosition.setMinWidth(140);

        vbButtonsRight.getChildren().addAll(lblCastlingRights,
                cbCastlesWK, cbCastlesWQ, cbCastlesBK, cbCastlesBQ,
                spacer1,
                lblEnPassant, cboxEnPassant,
                spacer2,
                lblTurn, rbTurnWhite, rbTurnBlack,
                spacer3,
                btnFlipBoard,
                spacer4,
                btnInitialPosition, btnClearBoard, btnCurrentPosition);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(10,30,10,30));
        vbButtonsRight.setMinWidth(140+60);

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(enterPosBoard, vbButtonsRight);
        HBox.setHgrow(enterPosBoard, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        VBox.setVgrow(hbMain, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        if(enterPosBoard.turn() == CONSTANTS.WHITE) {
            rbTurnWhite.setSelected(true);
        } else {
            rbTurnBlack.setSelected(true);
        }
        cbCastlesWK.setSelected(enterPosBoard.canCastleWhiteKing());
        cbCastlesWQ.setSelected(enterPosBoard.canCastleWhiteQueen());
        cbCastlesBK.setSelected(enterPosBoard.canCastleBlackKing());
        cbCastlesBQ.setSelected(enterPosBoard.canCastleBlackQueen());

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        cbCastlesWK.setOnAction(e -> {
            enterPosBoard.setCastleWKing(cbCastlesWK.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        cbCastlesWQ.setOnAction(e -> {
            enterPosBoard.setCastleWQueen(cbCastlesWQ.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        cbCastlesBK.setOnAction(e -> {
            enterPosBoard.setCastleBKing(cbCastlesBK.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        cbCastlesBQ.setOnAction(e -> {
            enterPosBoard.setCastleBQueen(cbCastlesBQ.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        cboxEnPassant.setOnAction(e -> {
            String epSquare = cboxEnPassant.getValue();
            enterPosBoard.setEnPassantSquare(epSquare);
            System.out.println(epSquare);
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        rbTurnWhite.setOnAction(e -> {
            enterPosBoard.setTurn(CONSTANTS.WHITE);
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        rbTurnBlack.setOnAction(e -> {
            enterPosBoard.setTurn(CONSTANTS.BLACK);
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        btnFlipBoard.setOnAction(e -> {
            enterPosBoard.flipBoard = !enterPosBoard.flipBoard;
            enterPosBoard.updateCanvas();
        });

        btnInitialPosition.setOnAction(e -> {
            enterPosBoard.resetToStartingPosition();
            rbTurnWhite.setSelected(true);
            cbCastlesWK.setSelected(true);
            cbCastlesWQ.setSelected(true);
            cbCastlesBK.setSelected(true);
            cbCastlesBQ.setSelected(true);
            btnOk.setDisable(false);
        });

        btnClearBoard.setOnAction(e -> {
            enterPosBoard.clearBoard();
            rbTurnWhite.setSelected(true);
            cbCastlesWK.setSelected(false);
            cbCastlesWQ.setSelected(false);
            cbCastlesBK.setSelected(false);
            cbCastlesBQ.setSelected(false);
            btnOk.setDisable(true);
        });

        btnCurrentPosition.setOnAction(e -> {
            enterPosBoard.copyBoard(originalBoard); 
            cbCastlesWK.setSelected(enterPosBoard.canCastleWhiteKing());
            cbCastlesWQ.setSelected(enterPosBoard.canCastleWhiteQueen());
            cbCastlesBK.setSelected(enterPosBoard.canCastleBlackKing());
            cbCastlesBQ.setSelected(enterPosBoard.canCastleBlackQueen());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.isConsistent()));
        });

        Scene scene = new Scene(vbMain);
        stage.setScene(scene);
        stage.setWidth(width+120);
        stage.getIcons().add(new Image("icons/app_icon.png"));

        stage.showAndWait();

        return accepted;
    }

    private void btnOkClicked() {
        accepted = true;
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }

    @Override
    public void boardChanged() {
        btnOk.setDisable(!(enterPosBoard.isConsistent()));
    }

    public boolean boardIsConsistent() {
        return enterPosBoard.isConsistent();
    }
        
    // A method for retrieving the resulting position from the dialog.
    public Board getCurrentBoard() {
        return enterPosBoard.makeBoardCopy();
    }
}
