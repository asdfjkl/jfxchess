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

public class DialogAppearance {

    Stage stage;
    boolean accepted = false;

    ShowAppearanceBoard appearanceBoard;

    final Label lblPieceStyle = new Label("Piece Style");
    final RadioButton rbMerida = new RadioButton("Merida (Default)");
    final RadioButton rbOld = new RadioButton("Old Style");
    final RadioButton rbUscf = new RadioButton("USCF");

    final Label lblBoardStyle = new Label("Board Style");
    final RadioButton rbBlue = new RadioButton("Blue (Default)");
    final RadioButton rbBrown = new RadioButton("Brown");
    final RadioButton rbGreen = new RadioButton("Green");

    final Label lblTheme = new Label("Theme");
    final RadioButton rbThemeLight = new RadioButton("Light");
    final RadioButton rbThemeDark = new RadioButton("Dark (Default)");

    int initialColorTheme;
    int selectedColorTheme;

    Button btnOk;
    Button btnCancel;

    public boolean show(Stage owner, BoardStyle currentStyle, double width, double height, int colorTheme) {

        appearanceBoard = new ShowAppearanceBoard();
        appearanceBoard.boardStyle.setPieceStyle(currentStyle.getPieceStyle());
        appearanceBoard.boardStyle.setColorStyle(currentStyle.getColorStyle());

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);

        ToggleGroup groupPiece = new ToggleGroup();
        rbMerida.setToggleGroup(groupPiece);
        rbOld.setToggleGroup(groupPiece);
        rbUscf.setToggleGroup(groupPiece);

        ToggleGroup groupBoard = new ToggleGroup();
        rbBlue.setToggleGroup(groupBoard);
        rbBrown.setToggleGroup(groupBoard);
        rbGreen.setToggleGroup(groupBoard);

        ToggleGroup groupTheme = new ToggleGroup();
        rbThemeLight.setToggleGroup(groupTheme);
        rbThemeDark.setToggleGroup(groupTheme);

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
        vbButtonsRight.getChildren().addAll(
                lblPieceStyle,
                rbMerida,
                rbOld,
                rbUscf,
                spacer1,
                lblBoardStyle,
                rbBlue,
                rbBrown,
                rbGreen,
                spacer2,
                lblTheme,
                rbThemeLight,
                rbThemeDark);

        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(10,30,10,30));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(appearanceBoard, vbButtonsRight);
        HBox.setHgrow(appearanceBoard, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        VBox.setVgrow(hbMain, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        rbMerida.setOnAction(e -> {
            appearanceBoard.boardStyle.setPieceStyle(BoardStyle.PIECE_STYLE_MERIDA);
            appearanceBoard.updateCanvas();
        });

        rbOld.setOnAction(e -> {
            appearanceBoard.boardStyle.setPieceStyle(BoardStyle.PIECE_STYLE_OLD);
            appearanceBoard.updateCanvas();
        });

        rbUscf.setOnAction(e -> {
            appearanceBoard.boardStyle.setPieceStyle(BoardStyle.PIECE_STYLE_USCF);
            appearanceBoard.updateCanvas();
        });

        rbBlue.setOnAction(e -> {
            appearanceBoard.boardStyle.setColorStyle(BoardStyle.STYLE_BLUE);
            appearanceBoard.updateCanvas();
        });

        rbBrown.setOnAction(e -> {
            appearanceBoard.boardStyle.setColorStyle(BoardStyle.STYLE_BROWN);
            appearanceBoard.updateCanvas();
        });

        rbGreen.setOnAction(e -> {
            appearanceBoard.boardStyle.setColorStyle(BoardStyle.STYLE_GREEN);
            appearanceBoard.updateCanvas();
        });

        rbThemeLight.setOnAction(e -> {
            selectedColorTheme = GameModel.STYLE_LIGHT;
        });

        rbThemeDark.setOnAction(e -> {
            selectedColorTheme = GameModel.STYLE_DARK;
        });

        if(currentStyle.getColorStyle() == BoardStyle.STYLE_BLUE) {
            rbBlue.setSelected(true);
        }
        if(currentStyle.getColorStyle() == BoardStyle.STYLE_BROWN) {
            rbBrown.setSelected(true);
        }
        if(currentStyle.getColorStyle() == BoardStyle.STYLE_GREEN) {
            rbGreen.setSelected(true);
        }
        if(currentStyle.getPieceStyle() == BoardStyle.PIECE_STYLE_MERIDA) {
            rbMerida.setSelected(true);
        }
        if(currentStyle.getPieceStyle() == BoardStyle.PIECE_STYLE_OLD) {
            rbOld.setSelected(true);
        }
        if(currentStyle.getPieceStyle() == BoardStyle.PIECE_STYLE_USCF) {
            rbUscf.setSelected(true);
        }

        if(colorTheme == GameModel.STYLE_LIGHT) {
            rbThemeLight.setSelected(true);
        } else {
            rbThemeDark.setSelected(true);
        }
        initialColorTheme = colorTheme;
        selectedColorTheme = colorTheme;

        Scene scene = new Scene(vbMain);
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
        PlatformUtils.applyDialogSizeFix(stage, width, height);
        stage.getIcons().add(new Image("icons/app_icon.png"));
        stage.showAndWait();

        return accepted;
    }

    private void btnOkClicked() {
        accepted = true;
        if( (rbThemeLight.isSelected() && initialColorTheme != GameModel.STYLE_LIGHT)
                || (rbThemeDark.isSelected() && initialColorTheme != GameModel.STYLE_DARK)) {

            DialogSimpleAlert dlgAlert = new DialogSimpleAlert(stage, Alert.AlertType.INFORMATION, "Please Restart!", "In order for style changes to take effect, please restart the application.");
            dlgAlert.showAndWait();
        }
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }

}
