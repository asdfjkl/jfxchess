package org.asdfjkl.jfxchess.gui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.asdfjkl.jfxchess.lib.*;

import java.util.ArrayList;

public class BookView implements StateChangeListener {

    GameModel gameModel;
    TableView<PolyglotExtEntry> bookTable;

    private ObservableList<PolyglotExtEntry> entries;

    public BookView(GameModel gameModel) {
        this.gameModel = gameModel;

        entries = FXCollections.observableArrayList();

        bookTable = new TableView<>();

        // PosCount, move, winRatio, AvgElo
        // long, string, string (for now), long
        TableColumn<PolyglotExtEntry, Long> colCount = new TableColumn<PolyglotExtEntry, Long>("# Games");
        colCount.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Long>("PosCount"));
        colCount.setMinWidth(100);
        colCount.setStyle( "-fx-alignment: CENTER-RIGHT;");
        colCount.setSortable(false);

        TableColumn<PolyglotExtEntry, String> colMove = new TableColumn<PolyglotExtEntry, String>("Move");
        //colMove.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, String>("Move"));
        colMove.setCellValueFactory(entry ->
                new SimpleStringProperty(gameModel.getGame().getCurrentNode().getBoard().san(new Move(entry.getValue().getMove())))
        );

        colMove.setMinWidth(80);
        colMove.setSortable(false);

        TableColumn<PolyglotExtEntry, PolyglotExtEntry> colWDL = new TableColumn<>("Win/Draw/Loss");
        colWDL.setCellValueFactory(entry ->
                new SimpleObjectProperty<>(entry.getValue())
        );
        colWDL.setCellFactory(p ->
                new RatioCell(gameModel.THEME)
        );
        colWDL.setMinWidth(150);

        /*
        TableColumn<PolyglotExtEntry, Integer> colWins = new TableColumn<PolyglotExtEntry, Integer>("Win");
        colWins.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Wins"));
        colWins.setMinWidth(75);
        //colWins.setStyle( "-fx-alignment: CENTER;");
        colWins.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colDraws = new TableColumn<PolyglotExtEntry, Integer>("Draw");
        colDraws.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Draws"));
        colDraws.setMinWidth(75);
        //colDraws.setStyle( "-fx-alignment: CENTER;");
        colDraws.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colLosses = new TableColumn<PolyglotExtEntry, Integer>("Loss");
        colLosses.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Losses"));
        colLosses.setMinWidth(75);
        //colLosses.getStyleClass().add("right");
        //colLosses.setStyle("-fx-alignment: CENTER;");
        //colLosses.getStyleClass().add("leftAlignedTableColumnHeader");
        colLosses.setSortable(false);
*/
        TableColumn<PolyglotExtEntry, Integer> colElo = new TableColumn<PolyglotExtEntry, Integer>("Avg. ELO");
        colElo.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("AvgELO"));
        colElo.setMinWidth(95);
        colElo.setSortable(false);

        /*
        colCount.prefWidthProperty().bind(bookTable.widthProperty().divide(7));
        colMove.prefWidthProperty().bind(bookTable.widthProperty().divide(6));
        colWins.prefWidthProperty().bind(bookTable.widthProperty().divide(6));
        colDraws.prefWidthProperty().bind(bookTable.widthProperty().divide(6));
        colLosses.prefWidthProperty().bind(bookTable.widthProperty().divide(6));
        colElo.prefWidthProperty().bind(bookTable.widthProperty().divide(7));
*/
        bookTable.getColumns().add(colCount);
        bookTable.getColumns().add(colMove);

        bookTable.getColumns().add(colWDL);
        //bookTable.getColumns().add(colWins);
        //bookTable.getColumns().add(colDraws);
        //bookTable.getColumns().add(colLosses);

        //bookTable.getColumns().add(colWinLoss);
        bookTable.getColumns().add(colElo);
        bookTable.setItems(this.entries);

        bookTable.setRowFactory( tv -> {
            TableRow<PolyglotExtEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    PolyglotExtEntry rowData = row.getItem();
                    handleMoveSelection(rowData.getMove());
                }
            });
            return row ;
        });

    }

    void handleMoveSelection(String uciMove) {

        Move m = new Move(uciMove);
        if(this.gameModel.getGame().getCurrentNode().getBoard().isLegal(m)) {
            this.gameModel.getGame().applyMove(m);
            this.gameModel.getGame().setTreeWasChanged(true);
            this.gameModel.triggerStateChange();
        }

    }

    @Override
    public void stateChange() {
        entries.clear();
        Board currentBoard = gameModel.getGame().getCurrentNode().getBoard();
        ArrayList<PolyglotExtEntry> currentEntries = gameModel.extBook.findEntries(currentBoard);
        entries.setAll(currentEntries);
    }
}
