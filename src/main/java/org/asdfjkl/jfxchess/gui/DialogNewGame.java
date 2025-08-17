/* JerryFX - A Chess Graphical User Interface
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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DialogNewGame {

    public static int ENTER_ANALYSE = 0;
    public static int PLAY_BOTS = 1;
    public static int PLAY_UCI = 2;

    Stage stage;
    public int result = -1;

    public int show() {

        Button btnEnterAnalyse = new Button("Enter & Analyse");
        btnEnterAnalyse.setGraphic(new ImageView( new Image("icons/enter-analyse.png")));
        btnEnterAnalyse.setContentDisplay(ContentDisplay.TOP);

        Button btnPlayBots = new Button("Play Bots");
        btnPlayBots.setGraphic(new ImageView( new Image("icons/play-bot.png")));
        btnPlayBots.setContentDisplay(ContentDisplay.TOP);

        Button btnPlayEngine = new Button("Play Engine");
        btnPlayEngine.setGraphic(new ImageView( new Image("icons/play-engine.png")));
        btnPlayEngine.setContentDisplay(ContentDisplay.TOP);

        btnEnterAnalyse.setPrefWidth(130);
        btnPlayBots.setPrefWidth(130);
        btnPlayEngine.setPrefWidth(130);

        btnEnterAnalyse.setOnAction( e -> {
            result = ENTER_ANALYSE;
            stage.close();
        });
        btnPlayBots.setOnAction( e -> {
            result = PLAY_BOTS;
            stage.close();
        });
        btnPlayEngine.setOnAction( e -> {
            result = PLAY_UCI;
            stage.close();
        });


        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        HBox root = new HBox(10, btnEnterAnalyse, btnPlayBots, btnPlayEngine);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.getIcons().add(new Image("icons/app_icon.png"));

        stage.showAndWait();

        return result;
    }

}


