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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;

public class DialogPlayBots {

    Stage stage;
    boolean accepted = false;
    boolean startInitial = true;
    public boolean playWhite = true;

    ObservableList<BotEngine> botEngineList;
    ListView<BotEngine> botEngineListView;
    int selectedIndex = 0;
    Label lblBioHeader = new Label();
    Label lblBioContent = new Label();
    ImageView ivBot = new ImageView();

    public boolean show(ArrayList<BotEngine> botEngines) {

        // list of bots
        botEngineList = FXCollections.observableArrayList(botEngines);
        botEngineListView = new ListView<>();
        botEngineListView.setItems(botEngineList);
        botEngineListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(BotEngine item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getName() == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        botEngineListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<BotEngine>() {
            @Override
            public void changed(ObservableValue<? extends BotEngine> observable, BotEngine oldValue, BotEngine newValue) {

                selectedIndex = botEngineList.indexOf(newValue);
                BotEngine selectedEngine = botEngineList.get(selectedIndex);
                lblBioHeader.setText(selectedEngine.getName() + " (" +selectedEngine.getElo()+")");
                lblBioContent.setText(selectedEngine.getBio());
                ivBot.setImage(selectedEngine.getImage());
          }
        });
        VBox vbBotList = new VBox();
        botEngineListView.setMinHeight(450);
        vbBotList.getChildren().addAll(botEngineListView);
        vbBotList.setPadding(new Insets(10));

        // bot info
        lblBioHeader.setText("");
        lblBioHeader.setStyle("-fx-font-weight: bold;");
        lblBioContent.setText(botEngines.get(0).getBio());
        lblBioContent.setWrapText(true);  // ensures multi-line wrapping
        VBox vbBotInfo = new VBox(5, lblBioHeader, lblBioContent);

        // all remaining options
        Label lblSide = new Label("Choose your side");
        lblSide.setStyle("-fx-font-weight: bold;");
        RadioButton rbWhite = new RadioButton("White");
        RadioButton rbBlack = new RadioButton("Black");

        ToggleGroup tgSide = new ToggleGroup();
        rbWhite.setToggleGroup(tgSide);
        rbBlack.setToggleGroup(tgSide);
        rbWhite.setSelected(true); // default: White

        HBox hbSide = new HBox(10, rbWhite, rbBlack);
        VBox vbSide = new VBox(5, lblSide, hbSide);

        Label lblStarPos = new Label("Start from");
        lblStarPos.setStyle("-fx-font-weight: bold;");
        RadioButton rbInitialPos = new RadioButton("Initial Position");
        RadioButton rbCurrentPos = new RadioButton("Current Position");

        ToggleGroup tgStarPos = new ToggleGroup();
        rbInitialPos.setToggleGroup(tgStarPos);
        rbCurrentPos.setToggleGroup(tgStarPos);
        rbInitialPos.setSelected(true);

        VBox vbGameOptions = new VBox(5, lblStarPos, rbInitialPos, rbCurrentPos);
        VBox vbMidColumn = new VBox(15, new Separator(), vbSide, vbGameOptions, new Separator(), vbBotInfo);
        vbMidColumn.setPadding(new Insets(10));

        // bot image
        ivBot.setImage(botEngines.get(0).getImage());
        ivBot.setFitWidth(300);   // target width
        ivBot.setFitHeight(500);  // target height
        ivBot.setPreserveRatio(true); // keep aspect ratio (recommended)
        VBox vbImage = new VBox();
        vbImage.getChildren().addAll(ivBot);
        vbImage.setPadding(new Insets(10));

        vbBotList.setPrefWidth(180);
        vbMidColumn.setPrefWidth(300);
        vbImage.setPrefWidth(300);

        HBox hbBot = new HBox();
        hbBot.getChildren().addAll(vbBotList, vbMidColumn, vbImage);

        // Buttons
        Button btnOk = new Button("OK");
        Button btnCancel = new Button("Cancel");

        HBox hbButtonBox = new HBox(10, btnOk, btnCancel);
        hbButtonBox.setAlignment(Pos.CENTER_RIGHT);
        hbButtonBox.setPadding(new Insets(10));

        VBox root = new VBox();
        root.getChildren().addAll(hbBot, hbButtonBox);

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        // additional logic
        rbWhite.setOnAction( e -> {
            playWhite = true;
            System.out.println("rb white event");
        });
        rbBlack.setOnAction( e -> {
            playWhite = false;
            System.out.println("rb black event");
        });
        rbInitialPos.setOnAction(e -> {
            startInitial = true;
        });
        rbCurrentPos.setOnAction(e -> {
            startInitial = false;
        });

        Scene scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

        stage.setScene(scene);
        stage.getIcons().add(new Image("icons/app_icon.png"));
        stage.sizeToScene();

        botEngineListView.getSelectionModel().select(0);
        botEngineListView.getFocusModel().focus(0);

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


