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

import java.io.File;

public class DialogDatabase {

    Stage stage;
    boolean accepted = false;
    TableView<PgnDatabaseEntry> table;

    PgnDatabase pgnDatabase;
    SearchPattern searchPattern;
    GameModel gameModel;

    final FileChooser fileChooser = new FileChooser();

    public boolean show(Stage owner, GameModel gameModel, boolean loadFile) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);

        pgnDatabase = gameModel.getPgnDatabase();
        searchPattern = gameModel.getSearchPattern();
        this.gameModel = gameModel;
        pgnDatabase.setDialogDatabase(this);

        ToolBar toolBar = new ToolBar();

        Button btnOpen = new Button("Open File");
        btnOpen.setGraphic(new ImageView(new Image("icons/document-open.png")));
        btnOpen.setContentDisplay(ContentDisplay.TOP);

        Button btnDelete = new Button("Delete Game");
        btnDelete.setGraphic(new ImageView(new Image("icons/mail-mark-not-junk.png")));
        btnDelete.setContentDisplay(ContentDisplay.TOP);

        Button btnSearch = new Button("Search");
        btnSearch.setGraphic(new ImageView(new Image("icons/system-search.png")));
        btnSearch.setContentDisplay(ContentDisplay.TOP);

        Button btnResetSearch = new Button("Reset Search");
        btnResetSearch.setGraphic(new ImageView(new Image("icons/view-refresh.png")));
        btnResetSearch.setContentDisplay(ContentDisplay.TOP);

        Button btnAbout = new Button("About");
        btnAbout.setGraphic(new ImageView(new Image("icons/help-browser.png")));
        btnAbout.setContentDisplay(ContentDisplay.TOP);

        toolBar.getItems().addAll(btnOpen, btnDelete, btnSearch, btnResetSearch, btnAbout);

        table = new TableView<>();

        TableColumn<PgnDatabaseEntry, Long> colIndex = new TableColumn<PgnDatabaseEntry, Long>("No.");
        colIndex.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, Long>("Index"));
        colIndex.setMinWidth(100);
        colIndex.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colEvent = new TableColumn<PgnDatabaseEntry, String>("Event");
        colEvent.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("Event"));
        colEvent.setMinWidth(200);
        colEvent.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colDate = new TableColumn<PgnDatabaseEntry, String>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("Date"));
        colDate.setMinWidth(100);
        colDate.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colWhite = new TableColumn<PgnDatabaseEntry, String>("White");
        colWhite.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("White"));
        colWhite.setMinWidth(200);
        colWhite.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colBlack = new TableColumn<PgnDatabaseEntry, String>("Black");
        colBlack.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("Black"));
        colBlack.setMinWidth(200);
        colBlack.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colResult = new TableColumn<PgnDatabaseEntry, String>("Result");
        colResult.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("Result"));
        colResult.setMinWidth(100);
        colResult.setSortable(false);

        TableColumn<PgnDatabaseEntry, String> colEco = new TableColumn<PgnDatabaseEntry, String>("ECO");
        colEco.setCellValueFactory(new PropertyValueFactory<PgnDatabaseEntry, String>("Eco"));
        colEco.setMinWidth(30);
        colEco.setSortable(false);

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
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(toolBar, table, hbButtons);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        btnOpen.setOnAction(e -> {
            btnOpenClicked();
        });

        btnDelete.setOnAction(e -> {
            btnDeleteClicked();
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

        if (gameModel.currentPgnDatabaseIdx < pgnDatabase.getNrGames()) {
            table.getSelectionModel().select(gameModel.currentPgnDatabaseIdx);
            table.scrollTo(gameModel.currentPgnDatabaseIdx);
        }

        Scene scene = new Scene(vbox);

        stage.setMinWidth(1050);
        // unfocus all buttons and widgets
        vbox.requestFocus();

        stage.getIcons().add(new Image("icons/app_icon.png"));
        if (loadFile) {
            Platform.runLater(() -> {
                pgnDatabase.open();
            });
        }
        stage.setScene(scene);
        stage.showAndWait();

        return accepted;
    }

    private void btnDeleteClicked() {

        if(table.getColumns().size() > 0) {
            int gameIndex = table.getSelectionModel().getSelectedIndex();
            PgnDatabaseEntry selectedEntry = pgnDatabase.getEntries().get(gameIndex);

            String msg = "Deleting Game Nr. " + (gameIndex+1) + " ("+selectedEntry.getWhite()+" - "+selectedEntry.getBlack()+")";
            DialogSimpleConfirm dlgConfirm = new DialogSimpleConfirm(this.stage, Alert.AlertType.WARNING,"Delete Game?", msg);
            dlgConfirm.showAndWait();
            if(dlgConfirm.accepted) {
                pgnDatabase.deleteGame(gameIndex);
            }
        }
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
        DialogAboutDatabase.show(stage);
    }

    private void btnSearchClicked() {

        DialogSearchGames dlg = new DialogSearchGames();
        boolean accepted = dlg.show(stage, gameModel.getGame().getCurrentNode().getBoard(),
                gameModel.boardStyle,
                gameModel.getSearchPattern().makeCopy());
        if (accepted) {
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
        if (gameModel.lastOpenedDirPath != null && gameModel.lastOpenedDirPath.exists()) {
            fileChooser.setInitialDirectory(gameModel.lastOpenedDirPath);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PGN", "*.pgn")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            pgnDatabase.filename = file.getAbsolutePath();
            if (file.getParentFile() != null) {
                gameModel.lastOpenedDirPath = file.getParentFile();
            }
            pgnDatabase.open();
        }

    }

}