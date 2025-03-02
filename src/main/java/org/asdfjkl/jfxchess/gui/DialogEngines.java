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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.asdfjkl.jfxchess.gui.EngineOption.*;

public class DialogEngines {

    final FileChooser fileChooser = new FileChooser();

    Stage stage;
    boolean accepted = false;

    ObservableList<Engine> engineList;
    ListView<Engine> engineListView;

    final Button btnAdd = new Button("Add...");
    final Button btnRemove = new Button("Remove...");
    final Button btnEditParameters = new Button("Edit Parameters");
    final Button btnResetParameters = new Button("Reset Parameters");

    int colorTheme = GameModel.STYLE_LIGHT;

    Button btnOk;
    Button btnCancel;

    int selectedIndex = 0;

    public boolean show(ArrayList<Engine> engines, int idxSelectedEngine, int colorTheme) {

        engineList = FXCollections.observableArrayList(engines);

        engineListView = new ListView<>();
        engineListView.setItems(engineList);

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
                if(selectedIndex == 0) {
                    btnEditParameters.setDisable(true);
                    btnResetParameters.setDisable(true);
                    btnRemove.setDisable(true);
                } else {
                    btnEditParameters.setDisable(false);
                    btnResetParameters.setDisable(false);
                    btnRemove.setDisable(false);
                }
                if (engineList.size() >= GameModel.MAX_N_ENGINES) {
                    btnAdd.setDisable(true);
                } else {
                    btnAdd.setDisable(false);
                }
            }
        });

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Chess Engines:");

        btnOk = new Button();
        btnOk.setText("OK");

        btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        // Got a "warning:static method should be called in a static way" here.
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbButtonsRight = new VBox();
        vbButtonsRight.setPrefWidth(140);
        Region spacer1 = new Region();
        btnAdd.setMinWidth(vbButtonsRight.getPrefWidth());
        btnRemove.setMinWidth(vbButtonsRight.getPrefWidth());
        btnEditParameters.setMinWidth(vbButtonsRight.getPrefWidth());
        btnResetParameters.setMinWidth(vbButtonsRight.getPrefWidth());

        vbButtonsRight.getChildren().addAll(btnAdd, btnRemove,
                spacer1,
                btnEditParameters, btnResetParameters);
        VBox.setVgrow(spacer1, Priority.ALWAYS);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(0,0,0,10));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(engineListView, vbButtonsRight);
        HBox.setHgrow(engineListView, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        VBox.setVgrow(hbMain, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        btnAdd.setOnAction(e -> {
            btnAddEngineClicked();
        });

        btnRemove.setOnAction(e -> {
            btnRemoveEngineClicked();
        });

        btnEditParameters.setOnAction(e -> {
            btnEditParametersClicked();
        });

        btnResetParameters.setOnAction(e -> {
            btnResetParametersClicked();
        });

        engineListView.getSelectionModel().select(idxSelectedEngine);

        vbMain.getStyleClass().add(JMetroStyleClass.BACKGROUND);
        Scene scene = new Scene(vbMain);

        JMetro jMetro;
        this.colorTheme = colorTheme;
        if(colorTheme == GameModel.STYLE_LIGHT) {
            jMetro = new JMetro();
        } else {
            jMetro = new JMetro(Style.DARK);
        }
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

    private void btnRemoveEngineClicked() {
        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        engineList.remove(selectedEngine);
        // Don't know how the size could have got bigger
        // when we removed an engine, but...
    }

    private void btnResetParametersClicked() {

        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        for(EngineOption enOpt : selectedEngine.options) {
            enOpt.resetToDefault();
        }
    }

    private void btnEditParametersClicked() {
        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        DialogEngineOptions dlg = new DialogEngineOptions();
        boolean accepted = dlg.show(selectedEngine.options, colorTheme);
        if(accepted) {
            // collect all entries from dialog
            for(EngineOption enOpt : selectedEngine.options) {

                String optName = enOpt.name;
                if(enOpt.type == EN_OPT_TYPE_CHECK) {
                    CheckBox widget = dlg.checkboxWidgets.get(optName);
                    if(widget != null) {
                        enOpt.checkStatusValue = widget.isSelected();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_COMBO) {
                    ComboBox<String> widget = dlg.comboboxWidgets.get(optName);
                    if(widget != null) {
                        enOpt.comboValue = widget.getSelectionModel().getSelectedItem();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_SPIN) {
                    Spinner<Integer> widget = dlg.spinnerWidgets.get(optName);
                    if(widget != null) {
                        enOpt.spinValue = widget.getValue();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_STRING) {
                    TextField widget = dlg.textfieldWidgets.get(optName);
                    if(widget != null) {
                        enOpt.stringValue = widget.getText();
                    }
                }
            }
        }
    }

    private void btnAddEngineClicked() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);

        // This first try-catch block will catch any exception thrown
        // inside and present it as part of a user alert.
        try {
            String line;

            Process engineProcess = Runtime.getRuntime().exec(file.getAbsolutePath());

            if (!engineProcess.isAlive()) {
                throw new RuntimeException("Couldn't start engine process " + file.getAbsolutePath());
            }

            // This is a try-with-resources block (without a catch block).
            // When the execution leaves this block,normally or because of
            // an exception, bre.close(), bri.close() and bro.close() will
            // be called automatically, in that order.
            // I noticed that bro.close() unexpectedly also kills the
            // engine-process in some way. So we don't have to do that
            // separately. Possible exceptions during close() will be
            // suppressed. (Previously the engine-process would stay alive
            // if it had been started right and an exception abrupt the
            // code-flow.)
            try (BufferedWriter bro = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
                 BufferedReader bri = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
                 BufferedReader bre = new BufferedReader(new InputStreamReader(engineProcess.getErrorStream()))) {

                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Send "uci" to the engine.
                try {
                    bro.write("uci\n");
                    bro.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to send UCI-commands to the engine process. "
                            + file.getAbsolutePath()
                            + e.getClass() + ": " + e.getMessage());
                }

                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Engine engine = new Engine();
                engine.setPath(file.getAbsolutePath());

                // Read all the engine options.
                try {
                    while (bri.ready()) {
                        EngineOption engineOption = new EngineOption();
                        line = bri.readLine();
                        if (line.startsWith("id name")) {
                            engine.setName(line.substring(7).trim());
                        }
                        try {
                            boolean parsed = engineOption.parseUciOptionString(line);
                            if (parsed) {
                                engine.options.add(engineOption);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            bre.close();
                            bri.close();
                            throw (new RuntimeException("Couldn't parse engine option: "
                                    + line + "  " + e.getClass() + ": " + e.getMessage()));

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to read commands from the engine process "
                            + file.getAbsolutePath()
                            + e.getClass() + ": " + e.getMessage());
                }

                // Don't know if this is meaningful, but since we have a bre...
                while (bre.ready()) {
                    System.err.println("Error message from engine: " + bre.readLine());
                }

                // Stop the engine
                try {
                    bro.write("stop\n");
                    bro.write("quit\n");
                    bro.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to send stop and quit to the engine process. "
                            + file.getAbsolutePath()
                            + e.getClass() + ": " + e.getMessage());
                }

                // Wait for engine to quit.
                try {
                    boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                    if (!finished) {
                        engineProcess.destroy();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Add engine to the engineList and make the list item selected.
                if (engine.getName() != null && !engine.getName().isEmpty()) {
                    engineList.add(engine);
                    engineList.sort(engine);
                    int idx = engineList.indexOf(engine);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            engineListView.scrollTo(idx);
                            engineListView.getSelectionModel().select(idx);
                        }
                    });
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

