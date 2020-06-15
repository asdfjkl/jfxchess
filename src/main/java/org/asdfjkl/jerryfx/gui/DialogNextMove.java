package org.asdfjkl.jerryfx.gui;

import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;

import java.util.ArrayList;

public class DialogNextMove {

    static Stage stage;
    static int selectedMove = -1;
    static ListView<String> lvMoves;
    final static double LIST_CELL_HEIGHT = 35.;

    public static int show(ArrayList<String> possibleMoves) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

        lvMoves = new ListView<>();
        lvMoves.getItems().addAll(possibleMoves);
        lvMoves.setPrefHeight(lvMoves.getItems().size() * LIST_CELL_HEIGHT);
        lvMoves.getSelectionModel().select(0);

        lvMoves.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {
                if (click.getClickCount() == 2) {
                    btnOkClicked();
                }
            }
        });

        Button btnOk = new Button();
        btnOk.setText("OK");
        btnOk.setOnAction(e -> btnOkClicked());

        Button btnCancel = new Button();
        btnCancel.setText("Cancel");
        btnCancel.setOnAction(e -> btnCancelClicked());

        HBox hbox = new HBox();
        Region spacer = new Region();
        hbox.getChildren().addAll(spacer, btnOk, btnCancel);
        hbox.setHgrow(spacer, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(lvMoves, hbox);

        Scene scene = new Scene(vbox);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.RIGHT) {
                btnOkClicked();
            }
            if (event.getCode() == KeyCode.LEFT) {
                btnCancelClicked();
            }
            if (event.getCode() == KeyCode.UP) {
                keypressUp();
            }
            if (event.getCode() == KeyCode.DOWN) {
                keypressDown();
            }
            if (event.getCode() == KeyCode.ENTER) {
                btnOkClicked();
            }
            event.consume();
        });

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

        return selectedMove;

    }

    private static void btnOkClicked() {
        int selectedIndex = lvMoves.getSelectionModel().getSelectedIndex();
        selectedMove = selectedIndex;
        stage.close();
    }

    private static void btnCancelClicked() {
        selectedMove = -1;
        stage.close();
    }

    private static void keypressDown() {
        int selectedIndex = lvMoves.getSelectionModel().getSelectedIndex();
        if(selectedIndex < lvMoves.getItems().size() - 1) {
            lvMoves.getSelectionModel().select(selectedIndex + 1);
        }
    }

    private static void keypressUp() {
        int selectedIndex = lvMoves.getSelectionModel().getSelectedIndex();
        if(selectedIndex > 0) {
            lvMoves.getSelectionModel().select(selectedIndex - 1);
        }
    }

}
