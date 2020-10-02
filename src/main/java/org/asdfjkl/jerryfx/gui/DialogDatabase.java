package org.asdfjkl.jerryfx.gui;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.OptimizedRandomAccessFile;
import org.asdfjkl.jerryfx.lib.PgnReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DialogDatabase {

    Stage stage;
    boolean accepted = false;
    TableView<PgnSTR> table;


    PgnDatabase pgnDatabase = new PgnDatabase();

    final FileChooser fileChooser = new FileChooser();

    public boolean show() {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        pgnDatabase.setDialogDatabase(this);

        ToolBar toolBar = new ToolBar();

        Button btnOpen = new Button("Open");
        btnOpen.setGraphic(new ImageView( new Image("icons/document-open.png")));
        btnOpen.setContentDisplay(ContentDisplay.TOP);

        Button btnSearch = new Button("Search");
        btnSearch.setGraphic(new ImageView( new Image("icons/system-search.png")));
        btnSearch.setContentDisplay(ContentDisplay.TOP);

        Button btnResetSearch = new Button("Reset Search");
        btnResetSearch.setGraphic(new ImageView( new Image("icons/view-refresh.png")));
        btnResetSearch.setContentDisplay(ContentDisplay.TOP);

        Button btnAbout = new Button("About");
        btnAbout.setGraphic(new ImageView( new Image("icons/help-browser.png")));
        btnAbout.setContentDisplay(ContentDisplay.TOP);

        toolBar.getItems().addAll(btnOpen, btnSearch, btnResetSearch, btnAbout);

        table = new TableView<>();


        TableColumn<PgnSTR, Long> colIndex = new TableColumn<PgnSTR, Long>("No.");
        colIndex.setCellValueFactory(new PropertyValueFactory<PgnSTR, Long>("Index"));
        colIndex.setMinWidth(100);

        TableColumn<PgnSTR, String> colEvent = new TableColumn<PgnSTR, String>("Event");
        colEvent.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Event"));
        colEvent.setMinWidth(200);

        TableColumn<PgnSTR, String> colSite = new TableColumn<PgnSTR, String>("Site");
        colSite.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Site"));
        colSite.setMinWidth(150);

        TableColumn<PgnSTR, String> colDate = new TableColumn<PgnSTR, String>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Date"));
        colDate.setMinWidth(100);

        TableColumn<PgnSTR, String> colRound = new TableColumn<PgnSTR, String>("Round");
        colRound.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Round"));
        colRound.setMinWidth(40);

        TableColumn<PgnSTR, String> colWhite = new TableColumn<PgnSTR, String>("White");
        colWhite.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("White"));
        colWhite.setMinWidth(200);

        TableColumn<PgnSTR, String> colBlack = new TableColumn<PgnSTR, String>("Black");
        colBlack.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Black"));
        colBlack.setMinWidth(200);

        TableColumn<PgnSTR, String> colResult = new TableColumn<PgnSTR, String>("Result");
        colResult.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Result"));
        colResult.setMinWidth(100);

        TableColumn<PgnSTR, String> colEco = new TableColumn<PgnSTR, String>("ECO");
        colEco.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Eco"));
        colEco.setMinWidth(30);

        ObservableList<PgnSTR> emptyList = FXCollections.observableArrayList();
        /*
        PgnSTR bar = new PgnSTR();
        bar.setIndex(0);
        bar.setEvent("foo");
        bar.setResult("1-0");
        bar.setBlack("Black");
        bar.setWhite("White");
        bar.setRound("Round");
        bar.setSite("Site");
        bar.setDate("date");
        foo.add(bar);
         */

        table.getColumns().add(colIndex);
        table.getColumns().add(colWhite);
        table.getColumns().add(colBlack);
        table.getColumns().add(colEvent);
        table.getColumns().add(colEco);
        // table.getColumns().add(colSite);
        table.getColumns().add(colDate);
        //table.getColumns().add(colRound);
        table.getColumns().add(colResult);
        table.setItems(emptyList);

        Button btnOk = new Button();
        btnOk.setText("Open Game");

        Button btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(toolBar, table, hbButtons);
        vbox.setVgrow(table, Priority.ALWAYS);
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        btnOpen.setOnAction(e -> {
            btnOpenClicked();
        });

        Scene scene = new Scene(vbox);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setMinWidth(1050);

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

    public void updateTable() {

        table.setItems(pgnDatabase.getEntries());

    }

    private void btnOpenClicked() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            pgnDatabase.filename = file.getAbsolutePath();
            pgnDatabase.open();
            /*
            System.out.println("pgn database size: " + pgnDatabase.entries.size());
            System.out.println("First item: "+pgnDatabase.entries.get(0).getWhite());
            table.setItems(pgnDatabase.entries);
            table.getColumns().get(0).setVisible(false);
            table.getColumns().get(0).setVisible(true);*/

        }
    }
}
