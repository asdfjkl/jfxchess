package org.asdfjkl.jerryfx.gui;

import com.kitfox.svg.A;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.OptimizedRandomAccessFile;
import org.asdfjkl.jerryfx.lib.PgnReader;
import org.asdfjkl.jerryfx.lib.TestCases;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PgnDatabase {

    private ObservableList<PgnSTR> entries;
    private ObservableList<PgnSTR> searchResults;
    PgnReader reader;
    String filename;
    static Stage stage;

    DialogDatabase dialogDatabase = null;

    public String lastOpenedFilePath = "";

    public PgnDatabase() {
        entries = FXCollections.observableArrayList(); //new ArrayList<>();
        reader = new PgnReader();
    }

    public ObservableList<PgnSTR> getEntries() {
        return entries;
    }

    public ObservableList<PgnSTR> getSearchResults() {
        return searchResults;
    }

    public void setDialogDatabase(DialogDatabase dialogDatabase) {
        this.dialogDatabase = dialogDatabase;
    }

    public void open() {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        Label lblScanPgn = new Label("Scanning PGN...");
        ProgressBar progressBar = new ProgressBar();

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(lblScanPgn, progressBar);

        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));

        Scene scene = new Scene(vbox, 400, 200);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.show();


        Task<ObservableList<PgnSTR>> task = new Task<>() {
            @Override protected ObservableList<PgnSTR> call() throws Exception {

                ArrayList<PgnSTR> newEntries = new ArrayList<>();

                boolean inComment = false;
                long game_pos = -1;
                PgnSTR current = null;
                long last_pos = 0;

                // check if we actually read ad least one game;
                boolean readAtLeastOneGame = false;

                String currentLine = "";
                OptimizedRandomAccessFile raf = null;

                File file = new File(filename);
                long fileSize = file.length();

                long gamesRead = 0;

                try {
                    raf = new OptimizedRandomAccessFile(filename, "r");
                    while ((currentLine = raf.readLine()) != null) {
                        // skip comments
                        if (currentLine.startsWith("%")) {
                            continue;
                        }

                        if (!inComment && currentLine.startsWith("[")) {

                            if (game_pos == -1) {
                                game_pos = last_pos;
                                current = new PgnSTR();
                            }
                            last_pos = raf.getFilePointer();

                            if (currentLine.length() > 4) {
                                int spaceOffset = currentLine.indexOf(' ');
                                int firstQuote = currentLine.indexOf('"');
                                int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                                String tag = currentLine.substring(1, spaceOffset);
                                String value = currentLine.substring(firstQuote + 1, secondQuote);
                                String valueEncoded = new String(value.getBytes("ISO-8859-1"), reader.getEncoding());
                                if(tag.equals("Event")) {
                                    current.setEvent(valueEncoded);
                                }
                                if(tag.equals("Site")) {
                                    current.setSite(valueEncoded);
                                }
                                if(tag.equals("Round")) {
                                    current.setRound(valueEncoded);
                                }
                                if(tag.equals("White")) {
                                    current.setWhite(valueEncoded);
                                }
                                if(tag.equals("Black")) {
                                    current.setBlack(valueEncoded);
                                }
                                if(tag.equals("Result")) {
                                    current.setResult(valueEncoded);
                                }
                                if(tag.equals("Date")) {
                                    current.setDate(valueEncoded);
                                }
                                if(tag.equals("ECO")) {
                                    current.setEco(valueEncoded);
                                }
                            }

                            continue;
                        }
                        if ((!inComment && currentLine.contains("{"))
                                || (inComment && currentLine.contains("}"))) {
                            inComment = currentLine.lastIndexOf("{") > currentLine.lastIndexOf("}");
                        }

                        if (game_pos != -1) {
                            current.setOffset(game_pos);
                            current.setIndex(newEntries.size()+1);
                            /*
                            if(current.getIndex() % 10000 == 0) {
                                System.out.println("@"+current.getIndex());
                            }*/
                            gamesRead += 1;
                            if(gamesRead > 10000) {
                                // updateMessage("Iteration " + game_pos);
                                updateProgress(game_pos, fileSize);
                                gamesRead = 0;
                            }
                            newEntries.add(current);
                            game_pos = -1;
                        }

                        last_pos = raf.getFilePointer();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return FXCollections.observableArrayList(newEntries);
            }
        };


        progressBar.progressProperty().bind(task.progressProperty());


        task.setOnSucceeded(e -> {
            entries = task.getValue();
            System.out.println("pgn database size: " + entries.size());
            System.out.println("First item: "+ entries.get(0).getWhite());
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTable();
            }
        });

        new Thread(task).start();

    }

    public void search(SearchPattern pattern) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        Label lblScanPgn = new Label("Searching...");
        ProgressBar progressBar = new ProgressBar();

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(lblScanPgn, progressBar);

        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));

        Scene scene = new Scene(vbox, 400, 200);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.show();


        Task<ObservableList<PgnSTR>> task = new Task<>() {
            @Override protected ObservableList<PgnSTR> call() throws Exception {

                /*
                final ArrayList<Long> indices = new ArrayList<Long>();
                for(PgnSTR entry : entries) {
                    indices.add(entry.getOffset());
                }

                final long startTime = System.currentTimeMillis();

                final PgnReader threadReader = new PgnReader();
                final OptimizedRandomAccessFile raf = new OptimizedRandomAccessFile(filename, "r");
                for(int i=0;i<indices.size();i++) {
                    raf.seek(indices.get(i));
                    Game g = threadReader.readGame(raf);
                    if(i%100000 == 0) {
                        System.out.println(i);
                        updateProgress(i, entries.size());
                    }
                    i++;
                }

                long stopTime = System.currentTimeMillis();
                long timeElapsed = stopTime - startTime;
                System.out.println("elapsed time for reading all games: " + (timeElapsed / 1000) + " secs");
                */

                final ArrayList<Long> indices = new ArrayList<Long>();
                for(PgnSTR entry : entries) {
                    indices.add(entry.getOffset());
                }

                PgnReader reader = new PgnReader();
                ArrayList<PgnSTR> foundEntries = new ArrayList<>();
                OptimizedRandomAccessFile raf = null;

                final long startTime = System.currentTimeMillis();

                try {
                    raf = new OptimizedRandomAccessFile(filename, "r");
                    for(int i=0;i<indices.size();i++) {
                        if(i%50000 == 0) {
                            System.out.println(i);
                            updateProgress(i, entries.size());
                        }
                        if(pattern.isSearchForHeader()) {
                            if(!pattern.matchesHeader(entries.get(i))) {
                                i++;
                                continue;
                            }
                        }
                        if(pattern.isSearchForPosition()) {
                            raf.seek(indices.get(i));
                            Game g = reader.readGame(raf);
                            if(!g.containsPosition(pattern.getPositionHash())) {
                                i++;
                                continue;
                            }
                        }
                        i++;
                        foundEntries.add(entries.get(i));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                long stopTime = System.currentTimeMillis();
                long timeElapsed = stopTime - startTime;
                System.out.println("elapsed time for reading all games: " + (timeElapsed / 1000) + " secs");

                return FXCollections.observableArrayList(foundEntries);
                //return FXCollections.observableArrayList(new ArrayList<>());
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            searchResults = task.getValue();
            System.out.println("pgn database size: " + entries.size());
            System.out.println("First item: "+ entries.get(0).getWhite());
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTableWithSearchResults();
            }
        });

        new Thread(task).start();
    }




}
