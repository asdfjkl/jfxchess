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

public class DialogEnterPosition implements EnterPosBoardListener {

    Stage stage;
    boolean accepted = false;

    EnterPosBoard enterPosBoard;

    Label lblCastlingRights = new Label("Castling Rights");
    CheckBox cbCastlesWK = new CheckBox("White O-O");
    CheckBox cbCastlesWQ = new CheckBox("White O-O-O");
    CheckBox cbCastlesBK = new CheckBox("Black O-O");
    CheckBox cbCastlesBQ = new CheckBox("Black O-O-O");

    Label lblTurn = new Label("Turn");
    RadioButton rbTurnWhite = new RadioButton("White");
    RadioButton rbTurnBlack = new RadioButton("Black");

    Button btnFlipBoard = new Button("Flip Board");
    Button btnInitialPosition = new Button("Initial Position");
    Button btnClearBoard = new Button("Clear Board");
    Button btnCurrentPosition = new Button("Current Position");

    Button btnOk;
    Button btnCancel;

    Board originalBoard;
    Board currentBoard;

    public boolean show(Board board, BoardStyle style, double width, double height) {

        originalBoard = board.makeCopy();
        enterPosBoard = new EnterPosBoard(originalBoard);
        currentBoard = enterPosBoard.board;
        enterPosBoard.addListener(this);
        enterPosBoard.boardStyle = style;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

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
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbButtonsRight = new VBox();
        vbButtonsRight.setPrefWidth(140);
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        Region spacer3 = new Region();
        btnFlipBoard.setMinWidth(vbButtonsRight.getPrefWidth());
        btnInitialPosition.setMinWidth(vbButtonsRight.getPrefWidth());
        btnClearBoard.setMinWidth(vbButtonsRight.getPrefWidth());
        btnCurrentPosition.setMinWidth(vbButtonsRight.getPrefWidth());

        vbButtonsRight.getChildren().addAll(lblCastlingRights,
                cbCastlesWK, cbCastlesWQ, cbCastlesBK, cbCastlesBQ,
                spacer1,
                lblTurn, rbTurnWhite, rbTurnBlack,
                spacer2,
                btnFlipBoard,
                spacer3,
                btnInitialPosition, btnClearBoard, btnCurrentPosition);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(10,30,10,30));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(enterPosBoard, vbButtonsRight);
        hbMain.setHgrow(enterPosBoard, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        vbMain.setVgrow(hbMain, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        if(currentBoard.turn == CONSTANTS.WHITE) {
            rbTurnWhite.setSelected(true);
        } else {
            rbTurnBlack.setSelected(true);
        }
        cbCastlesWK.setSelected(currentBoard.canCastleWhiteKing());
        cbCastlesWQ.setSelected(currentBoard.canCastleWhiteQueen());
        cbCastlesBK.setSelected(currentBoard.canCastleBlackKing());
        cbCastlesBQ.setSelected(currentBoard.canCastleBlackQueen());

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        cbCastlesWK.setOnAction(e -> {
            enterPosBoard.board.setCastleWKing(cbCastlesWK.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        cbCastlesWQ.setOnAction(e -> {
            enterPosBoard.board.setCastleWQueen(cbCastlesWQ.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        cbCastlesBK.setOnAction(e -> {
            enterPosBoard.board.setCastleBKing(cbCastlesBK.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        cbCastlesBQ.setOnAction(e -> {
            enterPosBoard.board.setCastleBQueen(cbCastlesBQ.isSelected());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        rbTurnWhite.setOnAction(e -> {
            enterPosBoard.board.turn = CONSTANTS.WHITE;
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        rbTurnBlack.setOnAction(e -> {
            enterPosBoard.board.turn = CONSTANTS.BLACK;
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

        btnFlipBoard.setOnAction(e -> {
            enterPosBoard.flipBoard = !enterPosBoard.flipBoard;
            enterPosBoard.updateCanvas();
        });

        btnInitialPosition.setOnAction(e -> {
            Board b = new Board(true);
            enterPosBoard.board = b;
            enterPosBoard.updateCanvas();
            rbTurnWhite.setSelected(true);
            cbCastlesWK.setSelected(true);
            cbCastlesWQ.setSelected(true);
            cbCastlesBK.setSelected(true);
            cbCastlesBQ.setSelected(true);
            btnOk.setDisable(false);
        });

        btnClearBoard.setOnAction(e -> {
            Board b = new Board(false);
            enterPosBoard.board = b;
            enterPosBoard.updateCanvas();
            rbTurnWhite.setSelected(true);
            cbCastlesWK.setSelected(false);
            cbCastlesWQ.setSelected(false);
            cbCastlesBK.setSelected(false);
            cbCastlesBQ.setSelected(false);
            btnOk.setDisable(true);
        });

        btnCurrentPosition.setOnAction(e -> {
            enterPosBoard.board = originalBoard.makeCopy();
            cbCastlesWK.setSelected(enterPosBoard.board.canCastleWhiteKing());
            cbCastlesWQ.setSelected(enterPosBoard.board.canCastleWhiteQueen());
            cbCastlesBK.setSelected(enterPosBoard.board.canCastleBlackKing());
            cbCastlesBQ.setSelected(enterPosBoard.board.canCastleBlackQueen());
            enterPosBoard.updateCanvas();
            btnOk.setDisable(!(enterPosBoard.board.isConsistent()));
        });

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
        currentBoard = enterPosBoard.board;
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }


    @Override
    public void boardChanged() {

        btnOk.setDisable(!(enterPosBoard.board.isConsistent()));

    }

}
