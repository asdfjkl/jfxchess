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
import jfxtras.styles.jmetro.JMetro;

import java.io.File;

public class DialogDatabase {

    Stage stage;
    boolean accepted = false;
    TableView<PgnSTR> table;

    PgnDatabase pgnDatabase;
    SearchPattern searchPattern;
    GameModel gameModel;

    final FileChooser fileChooser = new FileChooser();

    public boolean show(GameModel gameModel, boolean loadFile) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        pgnDatabase = gameModel.getPgnDatabase();
        searchPattern = gameModel.getSearchPattern();
        this.gameModel = gameModel;
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
        colIndex.setSortable(false);

        TableColumn<PgnSTR, String> colEvent = new TableColumn<PgnSTR, String>("Event");
        colEvent.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Event"));
        colEvent.setMinWidth(200);
        colEvent.setSortable(false);

        TableColumn<PgnSTR, String> colDate = new TableColumn<PgnSTR, String>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Date"));
        colDate.setMinWidth(100);
        colDate.setSortable(false);

        TableColumn<PgnSTR, String> colWhite = new TableColumn<PgnSTR, String>("White");
        colWhite.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("White"));
        colWhite.setMinWidth(200);
        colWhite.setSortable(false);

        TableColumn<PgnSTR, String> colBlack = new TableColumn<PgnSTR, String>("Black");
        colBlack.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Black"));
        colBlack.setMinWidth(200);
        colBlack.setSortable(false);

        TableColumn<PgnSTR, String> colResult = new TableColumn<PgnSTR, String>("Result");
        colResult.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Result"));
        colResult.setMinWidth(100);
        colResult.setSortable(false);

        TableColumn<PgnSTR, String> colEco = new TableColumn<PgnSTR, String>("ECO");
        colEco.setCellValueFactory(new PropertyValueFactory<PgnSTR, String>("Eco"));
        colEco.setMinWidth(30);
        colEco.setSortable(false);

        //ObservableList<PgnSTR> emptyList = FXCollections.observableArrayList();

        table.getColumns().add(colIndex);
        table.getColumns().add(colWhite);
        table.getColumns().add(colBlack);
        table.getColumns().add(colEvent);
        table.getColumns().add(colEco);
        table.getColumns().add(colDate);
        table.getColumns().add(colResult);
        table.setItems(pgnDatabase.getEntries());


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

        btnSearch.setOnAction(e -> {
            btnSearchClicked();
        });

        btnResetSearch.setOnAction(e -> {
            resetSearchClicked();
        });

        btnAbout.setOnAction(e -> {
            btnAboutClicked();
        });

        if(gameModel.currentPgnDatabaseIdx < pgnDatabase.getNrGames()) {
            table.getSelectionModel().select(gameModel.currentPgnDatabaseIdx);
            table.scrollTo(gameModel.currentPgnDatabaseIdx);
        }

        Scene scene = new Scene(vbox);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setMinWidth(1050);
        // unfocus all buttons and widgets
        vbox.requestFocus();

        stage.getIcons().add(new Image("icons/app_icon.png"));
        if(loadFile) {
            Platform.runLater(() -> {
                pgnDatabase.open();
            });
        }
        stage.setScene(scene);
        stage.showAndWait();


        System.out.println("db size: "+pgnDatabase.getNrGames());

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

    private void btnAboutClicked() {
        DialogAboutDatabase.show();
    }

    private void btnSearchClicked() {

        DialogSearchGames dlg = new DialogSearchGames();
        System.out.println("MIN MOVE:" + gameModel.getSearchPattern().getMinMove());
        //dlg.recoverFromSearchPattern(gameModel.getSearchPattern());
        boolean accepted = dlg.show(gameModel.getGame().getCurrentNode().getBoard(), gameModel.boardStyle, gameModel.getSearchPattern().makeCopy());
        if(accepted) {
            SearchPattern pattern = dlg.getSearchPattern();
            pgnDatabase.search(pattern);
            gameModel.setSearchPattern(pattern);
        }

    }

    public void updateTable() {

        table.setItems(pgnDatabase.getEntries());

    }

    public void updateTableWithSearchResults() {
        table.setItems(pgnDatabase.getSearchResults());
    }

    public void resetSearchClicked() {
        updateTable();
    }

    private void btnOpenClicked() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        fileChooser.setTitle("Open PGN File");
        if(gameModel.lastOpenedDirPath != null && gameModel.lastOpenedDirPath.exists()) {
            fileChooser.setInitialDirectory(gameModel.lastOpenedDirPath);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PGN", "*.pgn")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            pgnDatabase.filename = file.getAbsolutePath();
            if(file.getParentFile() != null) {
                gameModel.lastOpenedDirPath = file.getParentFile();
            }
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
