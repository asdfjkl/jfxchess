package org.asdfjkl.jerryfx.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.PolyglotExt;
import org.asdfjkl.jerryfx.lib.PolyglotExtEntry;

import java.util.ArrayList;

public class BookView implements StateChangeListener {

    GameModel gameModel;
    TableView bookTable;

    private ObservableList<PolyglotExtEntry> entries;

    public BookView(GameModel gameModel) {
        this.gameModel = gameModel;

        entries = FXCollections.observableArrayList();

        bookTable = new TableView<>();

        // PosCount, move, winRatio, AvgElo
        // long, string, string (for now), long
        TableColumn<PolyglotExtEntry, Long> colCount = new TableColumn<PolyglotExtEntry, Long>("# Games");
        colCount.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Long>("PosCount"));
        //colCount.setMinWidth(100);
        colCount.setSortable(false);

        TableColumn<PolyglotExtEntry, String> colMove = new TableColumn<PolyglotExtEntry, String>("Move");
        colMove.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, String>("Move"));
        //colMove.setMinWidth(100);
        colMove.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colWins = new TableColumn<PolyglotExtEntry, Integer>("Wins (%)");
        colWins.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Wins"));
        //colWins.setMinWidth(50);
        colWins.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colDraws = new TableColumn<PolyglotExtEntry, Integer>("Draws (%)");
        colDraws.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Draws"));
        //colDraws.setMinWidth(50);
        colDraws.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colLosses = new TableColumn<PolyglotExtEntry, Integer>("Losses (%)");
        colLosses.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("Losses"));
        //colLosses.setMinWidth(50);
        colLosses.setSortable(false);

        TableColumn<PolyglotExtEntry, Integer> colElo = new TableColumn<PolyglotExtEntry, Integer>("Avg. ELO");
        colElo.setCellValueFactory(new PropertyValueFactory<PolyglotExtEntry, Integer>("AvgELO"));
        //colElo.setMinWidth(100);
        colElo.setSortable(false);

        bookTable.getColumns().add(colCount);
        bookTable.getColumns().add(colMove);
        bookTable.getColumns().add(colWins);
        bookTable.getColumns().add(colDraws);
        bookTable.getColumns().add(colLosses);
        bookTable.getColumns().add(colElo);
        bookTable.setItems(this.entries);

    }

    @Override
    public void stateChange() {
        System.out.println("bookview: state change received");

        entries.clear();
        Board currentBoard = gameModel.getGame().getCurrentNode().getBoard();
        ArrayList<PolyglotExtEntry> currentEntries = gameModel.largeBook.findEntries(currentBoard);
        for(PolyglotExtEntry e : currentEntries) {
            //entries.add(e);
            System.out.println(e);
        }
        entries.setAll(currentEntries);
    }
}
