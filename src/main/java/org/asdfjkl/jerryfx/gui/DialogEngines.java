package org.asdfjkl.jerryfx.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DialogEngines {

    final FileChooser fileChooser = new FileChooser();

    Stage stage;
    boolean accepted = false;

    ObservableList<Engine> engineList;
    ListView<Engine> engineListView;

    Button btnAdd = new Button("Add...");
    Button btnRemove = new Button("Remove...");
    Button btnEditParameters = new Button("Edit Parameters");
    Button btnResetParameters = new Button("Reset Parameters");

    Button btnOk;
    Button btnCancel;

    public boolean show(ArrayList<Engine> engines) {

        engineList = FXCollections.observableArrayList(new ArrayList<Engine>());

        engineListView = new ListView<Engine>();
        engineListView.setItems(engineList);

        engineListView.setCellFactory(param -> new ListCell<Engine>() {
            @Override
            protected void updateItem(Engine item, boolean empty) {
                System.out.println("update item called");
                super.updateItem(item, empty);

                if (empty || item == null || item.getName() == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

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

        VBox vbButtonsRight = new VBox();
        vbButtonsRight.setPrefWidth(120);
        Region spacer1 = new Region();
        btnAdd.setMinWidth(vbButtonsRight.getPrefWidth());
        btnRemove.setMinWidth(vbButtonsRight.getPrefWidth());
        btnEditParameters.setMinWidth(vbButtonsRight.getPrefWidth());
        btnResetParameters.setMinWidth(vbButtonsRight.getPrefWidth());

        vbButtonsRight.getChildren().addAll(btnAdd, btnRemove,
                spacer1,
                btnEditParameters, btnResetParameters);
        vbButtonsRight.setVgrow(spacer1, Priority.ALWAYS);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(0,0,0,10));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(engineListView, vbButtonsRight);
        hbMain.setHgrow(engineListView, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        vbMain.setVgrow(hbMain, Priority.ALWAYS);
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

        Scene scene = new Scene(vbMain);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setScene(scene);
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
    }

    private void btnAddEngineClicked() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String line;

                Process engineProcess = Runtime.getRuntime().exec(file.getAbsolutePath());
                OutputStream stdout = engineProcess.getOutputStream ();
                InputStream stderr = engineProcess.getErrorStream ();
                InputStream stdin = engineProcess.getInputStream ();

                BufferedReader bri = new BufferedReader (new InputStreamReader(engineProcess.getInputStream()));
                BufferedWriter bro = new BufferedWriter (new OutputStreamWriter(engineProcess.getOutputStream()));
                BufferedReader bre = new BufferedReader (new InputStreamReader(engineProcess.getErrorStream()));

                System.out.println("after sleep, sending uci");
                bro.write("uci\n");
                bro.flush();

                for(int i=0;i<20;i++) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Engine engine = new Engine();
                engine.setPath(file.getAbsolutePath());

                //System.out.println("after uci");
                while(bri.ready()) {
                    EngineOption engineOption = new EngineOption();
                    line = bri.readLine();
                    System.out.println(line);
                    if(line.startsWith("id name")) {
                        engine.setName(line.substring(7).trim());
                        System.out.println("id: "+engine.getName());
                    }
                    boolean parsed = engineOption.parseUciOptionString(line);
                    if(parsed) {
                        System.out.println(engineOption.toUciOptionString());
                        engine.options.add(engineOption);
                    }
                }
                //System.out.println("after uci, read output");

                bro.write("quit\n");
                bro.flush();
                //System.out.println("after writing quit");

                bri.close();
                bro.close();
                bre.close();

                try {
                    boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                    if(!finished) {
                        engineProcess.destroy();
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                if(engine.getName() != null && !engine.getName().isEmpty()) {
                    System.out.println("engine id: "+engine.getName());
                    engineList.add(engine);
                    int idx = engineList.indexOf(engine);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            engineListView.scrollTo(idx);
                            engineListView.getSelectionModel().select(idx);
                        }
                    });
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

}
