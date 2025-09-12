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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;

public class DialogPlayEngine {

    Stage stage;
    boolean accepted = false;

    final Slider sliderStrength = new Slider();
    boolean startInitial = true;
    boolean playWhite = true;
    int elo = 0;

    ObservableList<Engine> engineList;
    ListView<Engine> engineListView;
    int selectedIndex = 0;

    public boolean show(ArrayList<Engine> engines) {

        // list of engines
        engineList = FXCollections.observableArrayList(engines);
        engineListView = new ListView<>();
        engineListView.setItems(engineList);
        engineListView.getSelectionModel().select(0);
        engineListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Engine item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getName() == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        engineListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Engine>() {
            @Override
            public void changed(ObservableValue<? extends Engine> observable, Engine oldValue, Engine newValue) {

                selectedIndex = engineList.indexOf(newValue);
                Engine selectedEngine = engineList.get(selectedIndex);
                System.out.println("selected engine: "+ (selectedEngine.getName()));
                if(selectedEngine.supportsUciLimitStrength()) {
                    sliderStrength.setDisable(false);
                    sliderStrength.setMin(selectedEngine.getMinUciElo());
                    sliderStrength.setMax(selectedEngine.getMaxUciElo());
                    sliderStrength.setValue(selectedEngine.getUciElo());
                } else {
                    sliderStrength.setDisable(true);
                }
            }
        });

        VBox vbEngineList = new VBox();
        vbEngineList.getChildren().addAll(engineListView);
        vbEngineList.setMinWidth(160);
        vbEngineList.setPadding(new Insets(10));

        // bot info
        Label lblBotElo = new Label("Elo");
        lblBotElo.setStyle("-fx-font-weight: bold;");

        Engine selectedEngine = engineList.get(selectedIndex);
        sliderStrength.setMin(selectedEngine.getMinUciElo());
        sliderStrength.setMax(selectedEngine.getMaxUciElo());

        sliderStrength.setBlockIncrement(1);
        sliderStrength.setMajorTickUnit(1);
        sliderStrength.setMinorTickCount(0);
        sliderStrength.setSnapToTicks(true);

        Label txtStrength = new Label();
        HBox hboxStrength = new HBox();
        hboxStrength.getChildren().addAll(sliderStrength, txtStrength);
        hboxStrength.setSpacing(20);

        sliderStrength.valueProperty().addListener(
                ((observableValue, number, t1) -> {
                    elo = t1.intValue();;
                    txtStrength.setText("Elo "+ elo);
                })
        );
        sliderStrength.setValue(selectedEngine.getUciElo());
        sliderStrength.setStyle("-show-value-on-interaction: false;");

        VBox vbStrength = new VBox(5, lblBotElo, hboxStrength);

        // all remaining options
        Label lblSide = new Label("Choose your side");
        lblSide.setStyle("-fx-font-weight: bold;");
        RadioButton rbWhite = new RadioButton("White");
        RadioButton rbBlack = new RadioButton("Black");

        ToggleGroup tgSide = new ToggleGroup();
        rbWhite.setToggleGroup(tgSide);
        rbBlack.setToggleGroup(tgSide);
        rbWhite.setSelected(true); // default: White

        rbWhite.setOnAction( e -> {
            playWhite = true;
        });
        rbBlack.setOnAction( e -> {
            playWhite = false;
        });

        HBox hbSide = new HBox(10, rbWhite, rbBlack);
        VBox vbSide = new VBox(5, lblSide, hbSide);

        Label lblStart = new Label("Start from");
        lblStart.setStyle("-fx-font-weight: bold;");
        RadioButton rbInitialPos = new RadioButton("Initial Position");
        RadioButton rbCurrentPos = new RadioButton("Current Position");

        ToggleGroup tgPos = new ToggleGroup();
        rbInitialPos.setToggleGroup(tgPos);
        rbCurrentPos.setToggleGroup(tgPos);
        rbInitialPos.setSelected(true);

        rbInitialPos.setOnAction(e -> {
            startInitial = true;
        });
        rbCurrentPos.setOnAction(e -> {
            startInitial = false;
        });

        VBox vbGameOptions = new VBox(5, lblStart, rbInitialPos, rbCurrentPos);

        VBox bioBox = new VBox(15, new Separator(), vbSide, vbGameOptions, new Separator(), vbStrength);
        bioBox.setPadding(new Insets(10));

        vbEngineList.setPrefWidth(160);
        bioBox.setPrefWidth(300);

        HBox hbBot = new HBox();
        hbBot.getChildren().addAll(vbEngineList, bioBox);

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
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

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


