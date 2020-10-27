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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jerryfx.lib.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
public class PgnDatabase {

    private ObservableList<PgnSTR> entries;
    private ObservableList<PgnSTR> searchResults;
    final PgnReader reader;
    String filename;
    static Stage stage;

    DialogDatabase dialogDatabase = null;

    //public String lastOpenedFilePath = "";

    private final ArrayList<Task> runningTasks = new ArrayList<Task>();

    public PgnDatabase() {
        entries = FXCollections.observableArrayList(); //new ArrayList<>();
        reader = new PgnReader();
    }

    public int getNrGames() { return entries.size(); }

    public ObservableList<PgnSTR> getEntries() {
        return entries;
    }

    public ObservableList<PgnSTR> getSearchResults() {
        return searchResults;
    }

    public void setDialogDatabase(DialogDatabase dialogDatabase) {
        this.dialogDatabase = dialogDatabase;
    }

    public ArrayList<Task> getRunningTasks() { return runningTasks; }

    private void registerRunningTask(Task task) {
        runningTasks.add(task);
    }

    private void unregisterRunningTask(Task task) {
        runningTasks.remove(task);
    }

    public Game loadGame(int index) {

        if(reader.isIsoLatin1(filename)) {
            reader.setEncodingIsoLatin1();
        } else {
            reader.setEncodingUTF8();
        }

        OptimizedRandomAccessFile raf = null;
        Game g = new Game();
        try {
            raf = new OptimizedRandomAccessFile(filename, "r");
            if(index < entries.size()) {
                raf.seek(entries.get(index).getOffset());
                g = reader.readGame(raf);
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
        return g;
    }

    /*
    public void open(String filename) {
        this.filename = filename;
        open();
    }*/

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

        if(reader.isIsoLatin1(filename)) {
            reader.setEncodingIsoLatin1();
        } else {
            reader.setEncodingUTF8();
        }

        final String tmpFilename = this.filename;

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

                File file = new File(tmpFilename);
                long fileSize = file.length();

                long gamesRead = 0;

                try {
                    raf = new OptimizedRandomAccessFile(tmpFilename, "r");
                    int cnt_i = 0;
                    while ((currentLine = raf.readLine()) != null) {
                        cnt_i++;
                        if (isCancelled()) {
                            break;
                        }
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
                                    if(secondQuote > firstQuote) {
                                        String value = currentLine.substring(firstQuote + 1, secondQuote);
                                        String valueEncoded = new String(value.getBytes(StandardCharsets.ISO_8859_1), reader.getEncoding());
                                        if (tag.equals("Event")) {
                                            current.setEvent(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("Site")) {
                                            current.setSite(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("Round")) {
                                            current.setRound(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("White")) {
                                            current.setWhite(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("Black")) {
                                            current.setBlack(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("Result")) {
                                            current.setResult(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("Date")) {
                                            current.setDate(valueEncoded);
                                            current.markValid();
                                        }
                                        if (tag.equals("ECO")) {
                                            current.setEco(valueEncoded);
                                            current.markValid();
                                        }
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
                            gamesRead += 1;
                            if(gamesRead > 10000) {
                                // updateMessage("Iteration " + game_pos);
                                updateProgress(game_pos, fileSize);
                                gamesRead = 0;
                            }
                            if(current.isValid()) {
                                newEntries.add(current);
                            }
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
            unregisterRunningTask(task);
            entries = task.getValue();
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTable();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(false);
        registerRunningTask(task);
        thread.start();

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
                        if (isCancelled()) {
                            break;
                        }
                        if(i%50000 == 0) {
                            updateProgress(i, entries.size());
                        }
                        if(pattern.isSearchForHeader()) {
                            if(!pattern.matchesHeader(entries.get(i))) {
                                continue;
                            }
                        }
                        if(pattern.isSearchForPosition()) {
                            raf.seek(indices.get(i));
                            Game g = reader.readGame(raf);
                            PgnPrinter printer = new PgnPrinter();
                            if(!g.containsPosition(pattern.getPositionHash(), pattern.getMinMove(), pattern.getMaxMove())) {
                                continue;
                            }
                        }
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
                return FXCollections.observableArrayList(foundEntries);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            unregisterRunningTask(task);
            searchResults = task.getValue();
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTableWithSearchResults();
            }
        });

        Thread thread = new Thread(task);
        registerRunningTask(task);
        thread.setDaemon(false);
        thread.start();
    }




}
