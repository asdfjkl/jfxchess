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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
//import jfxtras.styles.jmetro.JMetro;
//import jfxtras.styles.jmetro.JMetroStyleClass;
//import jfxtras.styles.jmetro.Style;

public class DialogSave {

    static final int DLG_SAVE_CANCEL = 0;
    static final int DLG_SAVE_APPEND_CURRENT = 1;
    static final int DLG_SAVE_APPEND_OTHER = 2;
    static final int DLG_SAVE_NEW = 3;
    static final int DLG_SAVE_REPLACE = 4;

    Stage stage;
    int result = 0;

    public int show(int colorTheme, boolean replacePossible, String currentFilename) {

        Label lblCurrentFile = new Label("");
        if(currentFilename != null) {
            lblCurrentFile.setText(currentFilename);
        }
        Button btnAddToCurrent = new Button("Append to current PGN");
        btnAddToCurrent.setPrefWidth(200);
        Button btnAddToOther = new Button("Append to other PGN");
        btnAddToOther.setPrefWidth(200);
        Button btnSaveAsNew = new Button("Save as new PGN");
        btnSaveAsNew.setPrefWidth(200);
        Button btnReplace = new Button("Replace Current Game");
        btnReplace.setPrefWidth(200);
        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(200);

        if(!replacePossible) {
            btnReplace.setDisable(true);
        }
        if(currentFilename == null) {
            btnAddToCurrent.setDisable(true);
        }

        stage = new Stage();

        btnAddToCurrent.setOnAction(e -> {
            result = DLG_SAVE_APPEND_CURRENT;
            stage.close();
        });
        btnAddToOther.setOnAction(e -> {
            result = DLG_SAVE_APPEND_OTHER;
            stage.close();
        });
        btnSaveAsNew.setOnAction(e -> {
            result = DLG_SAVE_NEW;
            stage.close();
        });
        btnReplace.setOnAction(e -> {
            result = DLG_SAVE_REPLACE;
            stage.close();
        });
        btnCancel.setOnAction(e -> {
            result = DLG_SAVE_CANCEL;
            stage.close();
        });

        VBox vbox = new VBox();
        vbox.getChildren().addAll(lblCurrentFile, btnAddToCurrent, btnAddToOther,
                btnSaveAsNew, btnReplace, btnCancel);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox);
        //vbox.getStyleClass().add(JMetroStyleClass.BACKGROUND);
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));

        //JMetro jMetro;
        if (colorTheme == GameModel.STYLE_LIGHT) {
            //jMetro = new JMetro();
        } else {
            //jMetro = new JMetro(Style.DARK);
        }
        //jMetro.setScene(scene);

        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        // stage.initStyle(StageStyle.UTILITY); will result on window not having focus/keyboard
        //                                      input not working on Linux
        stage.getIcons().add(new Image("icons/app_icon.png")); // To add an icon

        btnCancel.requestFocus();


        stage.showAndWait();

        return result;
    }

}
