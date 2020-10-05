package org.asdfjkl.jerryfx.gui;

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
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;

public class DialogAppearance {

    Stage stage;
    boolean accepted = false;

    ShowAppearanceBoard appearanceBoard;

    Label lblPieceStyle = new Label("Piece Style");
    RadioButton rbMerida = new RadioButton("Merida (Default)");
    RadioButton rbOld = new RadioButton("Old Style");
    RadioButton rbUscf = new RadioButton("USCF");

    Label lblBoardStyle = new Label("Board Style");
    RadioButton rbBlue = new RadioButton("Blue (Default)");
    RadioButton rbBrown = new RadioButton("Brown");
    RadioButton rbGreen = new RadioButton("Green");

    Button btnOk;
    Button btnCancel;

    public boolean show(BoardStyle currentStyle, double width, double height) {

        appearanceBoard = new ShowAppearanceBoard();
        appearanceBoard.boardStyle.setPieceStyle(currentStyle.getPieceStyle());
        appearanceBoard.boardStyle.setColorStyle(currentStyle.getColorStyle());

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        ToggleGroup groupPiece = new ToggleGroup();
        rbMerida.setToggleGroup(groupPiece);
        rbOld.setToggleGroup(groupPiece);
        rbUscf.setToggleGroup(groupPiece);

        ToggleGroup groupBoard = new ToggleGroup();
        rbBlue.setToggleGroup(groupBoard);
        rbBrown.setToggleGroup(groupBoard);
        rbGreen.setToggleGroup(groupBoard);

        btnOk = new Button();
        btnOk.setText("OK");

        btnCancel = new Button();
        btnCancel.setText("Cancel");

        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
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
                rbGreen);

        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(10,30,10,30));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(appearanceBoard, vbButtonsRight);
        hbMain.setHgrow(appearanceBoard, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        vbMain.setVgrow(hbMain, Priority.ALWAYS);
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

        if(currentStyle.getColorStyle() == BoardStyle.STYLE_BLUE) {
            System.out.println("SETTING COLOR TO BLUE");
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

        Scene scene = new Scene(vbMain);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
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

}
