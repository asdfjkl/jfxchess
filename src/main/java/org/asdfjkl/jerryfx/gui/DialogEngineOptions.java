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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;

import java.util.ArrayList;
import java.util.HashMap;

import static org.asdfjkl.jerryfx.gui.EngineOption.*;

public class DialogEngineOptions {

    Stage stage;
    boolean accepted = false;

    Button btnOk;
    Button btnCancel;

    HashMap<String, Spinner<Integer>> spinnerWidgets;
    HashMap<String, CheckBox> checkboxWidgets;
    HashMap<String, ComboBox<String>> comboboxWidgets;
    HashMap<String, TextField> textfieldWidgets;

    public boolean show(ArrayList<EngineOption> engineOptions) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        btnOk = new Button();
        btnOk.setText("OK");

        btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        spinnerWidgets = new HashMap<>();
        checkboxWidgets = new HashMap<>();
        comboboxWidgets = new HashMap<>();
        textfieldWidgets = new HashMap<>();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        int i = 0;
        for(EngineOption enOpt : engineOptions) {

            Label lblEnOpt = new Label(enOpt.name+":");
            gridPane.add(lblEnOpt, 0, i);

            if(enOpt.type == EN_OPT_TYPE_CHECK) {
                CheckBox cbEnOpt = new CheckBox();
                cbEnOpt.setSelected(enOpt.checkStatusValue);
                checkboxWidgets.put(enOpt.name, cbEnOpt);
                gridPane.add(cbEnOpt, 1, i);
            }

            if(enOpt.type == EN_OPT_TYPE_STRING) {
                TextField tfEnOpt = new TextField();
                tfEnOpt.setText(enOpt.stringValue);
                textfieldWidgets.put(enOpt.name, tfEnOpt);
                gridPane.add(tfEnOpt, 1, i);
            }

            if(enOpt.type == EN_OPT_TYPE_SPIN) {
                Spinner<Integer> spEnOpt = new Spinner<Integer>();
                SpinnerValueFactory<Integer> valueFactory =
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(enOpt.spinMin, enOpt.spinMax, enOpt.spinValue);
                spEnOpt.setValueFactory(valueFactory);
                spEnOpt.setEditable(true);
                spinnerWidgets.put(enOpt.name, spEnOpt);
                gridPane.add(spEnOpt, 1, i);
            }

            if(enOpt.type == EN_OPT_TYPE_COMBO) {
                ObservableList<String> options = FXCollections.observableArrayList();
                int idx = 0;
                int selectedIdx = 0;
                for(String value : enOpt.comboValues) {
                    options.add(value);
                    if(value.equals(enOpt.comboValue)) {
                        selectedIdx = idx;
                    }
                    idx++;
                }
                ComboBox<String> cbEnOpt = new ComboBox<String>(options);
                comboboxWidgets.put(enOpt.name, cbEnOpt);
                gridPane.add(cbEnOpt, 1, i);
                final int fSelectedIdx = selectedIdx;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        cbEnOpt.getSelectionModel().select(fSelectedIdx);
                    }
                });
            }
            i++;
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(gridPane);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(scrollPane, hbButtons);
        vbMain.setVgrow(gridPane, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        Scene scene = new Scene(vbMain);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setScene(scene);
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
