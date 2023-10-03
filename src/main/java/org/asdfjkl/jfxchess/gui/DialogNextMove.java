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

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;

import java.util.ArrayList;

public class DialogNextMove {

    static Stage stage;
    static int selectedMove = -1;
    static ListView<String> lvMoves;
    final static double LIST_CELL_HEIGHT = 35.;

    public static int show(ArrayList<String> possibleMoves, int colorTheme) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        // stage.initStyle(StageStyle.UTILITY); bug: will result in window not having focus
        // stage.requestFocus();                     and thus not accepting key input on Linux
        stage.getIcons().add(new Image("icons/app_icon.png")); // To add an icon

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
        vbox.getStyleClass().add(JMetroStyleClass.BACKGROUND);

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

        JMetro jMetro;
        if(colorTheme == GameModel.STYLE_LIGHT) {
            jMetro = new JMetro();
        } else {
            jMetro = new JMetro(Style.DARK);
        }
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

        return selectedMove;

    }

    private static void btnOkClicked() {
        selectedMove = lvMoves.getSelectionModel().getSelectedIndex();
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
