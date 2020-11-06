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

package org.asdfjkl.jerryfx.gui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;

public class DialogAbout {

    static Stage stage;

    public static void show() {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

        TextFlow tfAbout = new TextFlow();

        Text txtJerry = new Text("JerryFX\n");
        txtJerry.setFont(Font.font("Verdana", FontWeight.BOLD, 14));

        Text txtCopyright = new Text("Version 4.0.0\n" +
                "Copyright © 2014-2020\n" +
                "Dominik Klein\n" +
                "licensed under GNU GPL 2");

        Text txtCredits = new Text("Credits\n");
        txtCredits.setFont(Font.font("Verdana", FontWeight.BOLD, 14));

        Text txtCreditText = new Text(
                "Stockfish Chess Engine\n" +
                "by the Stockfish-Team\n\n" +
                "'VARIED.BIN' opening book\n" +
                "        by Marc Lacrosse/Jose-Chess Tool        \n\n" +
                "Piece Images\n" +
                "from Raptor Chess Interface\n\n" +
                "all licensed under GNU GPL 2");


        tfAbout.getChildren().addAll(
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()),
                txtJerry,
                new Text(System.lineSeparator()),
                txtCopyright,
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()),
                txtCredits,
                new Text(System.lineSeparator()),
                txtCreditText,
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()));

        tfAbout.setTextAlignment(TextAlignment.CENTER);

        Button btnOk = new Button();
        btnOk.setText("OK");
        btnOk.setOnAction(e -> btnOkClicked());

        VBox vbox = new VBox();
        vbox.getChildren().addAll(tfAbout);
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

    }

    private static void btnOkClicked() {
        stage.close();
    }

}
