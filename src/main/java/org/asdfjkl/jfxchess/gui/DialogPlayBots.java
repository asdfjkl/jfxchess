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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DialogPlayBots {

    Stage stage;
    boolean accepted = false;

    final Slider sliderStrength = new Slider();
    RadioButton rbStartInitial = new RadioButton("Initial Position (New Game)");
    RadioButton rbStartCurrent = new RadioButton("Current Board");
    int strength = 0;

    public boolean show() {

        /*
        ToggleGroup grpEnterMoves = new ToggleGroup();

        rbStartInitial.setToggleGroup(grpEnterMoves);
        rbStartCurrent.setToggleGroup(grpEnterMoves);
        rbStartInitial.setSelected(true);

        VBox vbEnterMoves = new VBox(10, rbStartInitial, rbStartCurrent); // spacing = 10px
        vbEnterMoves.setAlignment(Pos.CENTER_LEFT);
        vbEnterMoves.setPadding(new Insets(20));

        tabMoves.setContent(vbEnterMoves);

        tabPane.getTabs().addAll(tabMoves, tabBots, tabEngine);
        tabPane.setTabMinWidth(100); // or a value that ensures tabs always show
        tabPane.setTabMaxWidth(Double.MAX_VALUE);
         */

        // list of bots
        ListView<String> lvBots = new ListView<String>();
        lvBots.getItems().add("Benny, the Beginner");
        lvBots.getItems().add("Lila the Learner");
        lvBots.getItems().add("Captain Castle");
        VBox vbBots = new VBox();
        vbBots.getChildren().addAll(lvBots);
        vbBots.setMinWidth(160);
        vbBots.setPadding(new Insets(10));

        // bot info
        Label lblBioHeader = new Label("Bio");
        lblBioHeader.setStyle("-fx-font-weight: bold;");
        Label lblBioContent = new Label("Benny just learned how to move the pieces " +
                "yesterday. He’s enthusiastic, sometimes forgets about his king’s safety, " +
                "but always has fun no matter the result."        );
        lblBioContent.setWrapText(true);  // ensures multi-line wrapping
        VBox section1 = new VBox(5, lblBioHeader, lblBioContent);

        Label lblBotElo = new Label("Elo");
        lblBotElo.setStyle("-fx-font-weight: bold;");
        Label lblBotValue = new Label("1200");
        VBox section2 = new VBox(5, lblBotElo, lblBotValue);

        // all remaining options
        Label sideLabel = new Label("Choose your side");
        sideLabel.setStyle("-fx-font-weight: bold;");
        RadioButton whiteBtn = new RadioButton("White");
        RadioButton blackBtn = new RadioButton("Black");

        ToggleGroup sideGroup = new ToggleGroup();
        whiteBtn.setToggleGroup(sideGroup);
        blackBtn.setToggleGroup(sideGroup);
        whiteBtn.setSelected(true); // default: White

        HBox sideBox = new HBox(10, whiteBtn, blackBtn);
        VBox sideSection = new VBox(5, sideLabel, sideBox);

        Label startLabel = new Label("Start from");
        startLabel.setStyle("-fx-font-weight: bold;");
        RadioButton initialBtn = new RadioButton("Initial Position");
        RadioButton currentBtn = new RadioButton("Current Position");

        ToggleGroup startGroup = new ToggleGroup();
        initialBtn.setToggleGroup(startGroup);
        currentBtn.setToggleGroup(startGroup);
        initialBtn.setSelected(true);

        VBox startSection = new VBox(5, startLabel, initialBtn, currentBtn);

        VBox bioBox = new VBox(15, sideSection, startSection, section1, section2);
        bioBox.setPadding(new Insets(10));

        // bot image
        ImageView ivBot = new ImageView(new Image("bots/01_benny_the_beginner.png"));
        ivBot.setFitWidth(300);   // target width
        ivBot.setFitHeight(500);  // target height
        ivBot.setPreserveRatio(true); // keep aspect ratio (recommended)
        VBox vbImage = new VBox();
        vbImage.getChildren().addAll(ivBot);
        vbImage.setPadding(new Insets(10));

        vbBots.setPrefWidth(160);
        bioBox.setPrefWidth(300);
        vbImage.setPrefWidth(300);

        HBox hbBot = new HBox();
        hbBot.getChildren().addAll(vbBots, bioBox, vbImage);

        // Buttons
        Button btnOk = new Button("OK");
        Button btnCancel = new Button("Cancel");

        HBox buttonBox = new HBox(10, btnOk, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox();
        root.getChildren().addAll(hbBot, buttonBox);

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });


        Scene scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        stage.getIcons().add(new Image("icons/app_icon.png"));
        stage.sizeToScene();

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


