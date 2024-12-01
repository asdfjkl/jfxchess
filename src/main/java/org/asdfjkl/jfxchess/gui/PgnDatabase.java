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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jfxchess.lib.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
public class PgnDatabase {

    private ObservableList<PgnDatabaseEntry> entries;
    private ObservableList<PgnDatabaseEntry> searchResults;
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

    public ObservableList<PgnDatabaseEntry> getEntries() {
        return entries;
    }

    public ObservableList<PgnDatabaseEntry> getSearchResults() {
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

    public void saveDatabase() {

        saveDatabaseAs(filename);

    }

    public void saveDatabaseAs(String filename) {

        String tmpFilenameWoDir = Util.getRandomFilename();

        File file = new File(filename);
        File path = file.getParentFile();
        String filenameWithoutDir = file.getName();
        File tmpFile = new File(path, tmpFilenameWoDir);

        final String currentPgnFilename = this.filename;
        final String pgnFilename = filename;
        final String tmpFilename = tmpFile.getAbsolutePath();

        final boolean overwrite = pgnFilename.equals(tmpFilename);

        final ObservableList<PgnDatabaseEntry> entries = this.entries;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        Label lblScanPgn = new Label("Saving PGN...");
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

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {

                OptimizedRandomAccessFile rafReader = null;
                OptimizedRandomAccessFile rafWriter = null;
                BufferedWriter writer = null;

                File currentPgn = new File(currentPgnFilename);
                long fileSize = currentPgn.length();

                PgnPrinter pgnPrinter = new PgnPrinter();

                int startIndex = 0;

                long linesWritten = 0;

                try {
                    rafReader = new OptimizedRandomAccessFile(currentPgnFilename, "r");
                    //rafWriter = new OptimizedRandomAccessFile(tmpFilename, "rw");
                    writer = new BufferedWriter(new FileWriter(tmpFilename));

                    for (int i = 0; i < entries.size(); i++) {

                        // if game was modified, always write it out
                        if(entries.get(i).wasModified()) {
                            // first write out everything unmodified up until now
                            long startOffset = entries.get(startIndex).getOffset();
                            long stopOffset = entries.get(i).getOffset();
                            rafReader.seek(startOffset);
                            while(rafReader.getFilePointer() < stopOffset) {
                                String line = rafReader.readLine();
                                linesWritten += 1;
                                if(linesWritten % 20000 == 0) {
                                    linesWritten = 1;
                                    updateProgress(rafReader.getFilePointer(), fileSize);
                                }
                                if(line == null) {
                                    break;
                                } else {
                                    writer.write(line);
                                    writer.write(0xa); // 0xa = LF = \n
                                }
                            }
                            // write the modified game
                            Game g = entries.get(i).getModifiedGame();
                            if(g!=null) {
                                String sGame = pgnPrinter.printGame(g);
                                writer.write(sGame);
                                writer.write(0xa); // 0xa = LF = \n
                                writer.write(0xa); // 0xa = LF = \n
                            }
                        } else {
                            // if it wasn't modified, just collect
                            // only exception: we encountered the last game
                            if(i>0 && entries.get(i-1).wasModified()) {
                                startIndex = i;
                            }
                            if(i == entries.size()-1) {
                                rafReader.seek(entries.get(startIndex).getOffset());
                                while(rafReader.getFilePointer() < fileSize) {
                                    String line = rafReader.readLine();
                                    linesWritten++;
                                    if(linesWritten % 20000 == 0) {
                                        linesWritten = 1;
                                        updateProgress(rafReader.getFilePointer(), fileSize);
                                    }
                                    if(line == null) {
                                        break;
                                    } else {
                                        writer.write(line);
                                        writer.write(0xa); // 0xa = LF = \n
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (rafReader != null) {
                        try {
                            rafReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (rafWriter != null) {
                        try {
                            rafWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(writer != null) {
                        try {
                            writer.flush();
                            writer.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                File pgn = new File(pgnFilename);
                pgn.delete();
                tmpFile.renameTo(pgn);

                return null;
            }
        };


        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            unregisterRunningTask(task);
            task.getValue();
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTable();
            }
            this.filename = pgnFilename;
            open();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(false);
        registerRunningTask(task);
        thread.start();

    }

    public void appendToCurrentPGN(Game g) {

        BufferedWriter writer = null;
        try {
            File file = new File(filename);
            long offset = file.length() + 2;
            PgnPrinter pgnPrinter = new PgnPrinter();
            writer = new BufferedWriter(new FileWriter(filename, true));
            String sGame = pgnPrinter.printGame(g);
            writer.write(0xa); // 0xa = LF = \n
            writer.write(0xa); // 0xa = LF = \n
            writer.write(sGame);
            writer.write(0xa); // 0xa = LF = \n
            writer.write(0xa); // 0xa = LF = \n
            PgnDatabaseEntry newEntry = new PgnDatabaseEntry();
            newEntry.setWhite(g.getHeader("White"));
            newEntry.setBlack(g.getHeader("Black"));
            newEntry.setDate(g.getHeader("Date"));
            newEntry.setOffset(offset);
            newEntry.setEvent(g.getHeader("Event"));
            newEntry.setEco(g.getHeader("Eco"));
            newEntry.setResult(g.getHeader("Result"));
            newEntry.setRound(g.getHeader("Round"));
            newEntry.setSite(g.getHeader("Site"));
            newEntry.setIndex(entries.size()+1);
            entries.add(newEntry);
            writer.close();
        } catch(IOException e) {
            System.err.println(e);
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    public void appendToOtherPGN(GameModel gameModel) {

        // write game to file, then re-load this file into database
        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if(gameModel.lastSaveDirPath != null && gameModel.lastSaveDirPath.exists()) {
            fileChooser.setInitialDirectory(gameModel.lastSaveDirPath);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PGN", "*.pgn")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            if(file.getParentFile() != null) {
                gameModel.lastSaveDirPath = file.getParentFile();
            }

            BufferedWriter writer = null;
            try {
                PgnPrinter pgnPrinter = new PgnPrinter();
                writer = new BufferedWriter(new FileWriter(file, true));
                String sGame = pgnPrinter.printGame(gameModel.getGame());
                writer.write(0xa); // 0xa = LF = \n
                writer.write(0xa); // 0xa = LF = \n
                writer.write(sGame);
                writer.write(0xa); // 0xa = LF = \n
                writer.write(0xa); // 0xa = LF = \n
                writer.close();
            } catch(IOException e) {
                System.err.println(e);
            } finally {
                if(writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }
            this.filename = file.getAbsolutePath();
            open();
        }
    }

    public void replaceCurrentGame(Game g, int currentDatabaseIndex) {
        entries.get(currentDatabaseIndex).setModifiedGame(g);
        entries.get(currentDatabaseIndex).markAsModified();
        PgnPrinter tmp = new PgnPrinter();
        String tmp_game = tmp.printGame(g);
        saveDatabase();
    }

    public void saveAsNewPGN(GameModel gameModel) {

        // write game to file, then re-load this file into database
        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if(gameModel.lastSaveDirPath != null && gameModel.lastSaveDirPath.exists()) {
            fileChooser.setInitialDirectory(gameModel.lastSaveDirPath);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PGN", "*.pgn")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            if(file.getParentFile() != null) {
                gameModel.lastSaveDirPath = file.getParentFile();
            }
            PgnPrinter printer = new PgnPrinter();
            printer.writeGame(gameModel.getGame(), file.getAbsolutePath());

            this.filename = file.getAbsolutePath();
            open();
            //gameModel.currentPgnDatabaseIdx = 0;
        }

    }

    public void deleteGame(int index) {

        String tmpFilenameWoDir = Util.getRandomFilename();

        File file = new File(filename);
        File path = file.getParentFile();
        String filenameWithoutDir = file.getName();
        File tmpFile = new File(path, tmpFilenameWoDir);

        final String currentPgnFilename = this.filename;
        final String pgnFilename = filename;
        final String tmpFilename = tmpFile.getAbsolutePath();

        final boolean overwrite = pgnFilename.equals(tmpFilename);

        final ObservableList<PgnDatabaseEntry> entries = this.entries;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        Label lblScanPgn = new Label("Deleting Game...");
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

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {

                OptimizedRandomAccessFile rafReader = null;
                OptimizedRandomAccessFile rafWriter = null;
                BufferedWriter writer = null;

                File currentPgn = new File(currentPgnFilename);
                long fileSize = currentPgn.length();

                PgnPrinter pgnPrinter = new PgnPrinter();


                long linesWritten = 0;

                try {
                    rafReader = new OptimizedRandomAccessFile(currentPgnFilename, "r");
                    //rafWriter = new OptimizedRandomAccessFile(tmpFilename, "rw");
                    writer = new BufferedWriter(new FileWriter(tmpFilename));

                    long startOffset = entries.get(0).getOffset();
                    long stopOffset = entries.get(index).getOffset();
                    long afterStartOffset = -1;
                    if(entries.size() > (index+1)) {
                        afterStartOffset = entries.get(index+1).getOffset();
                    }
                    rafReader.seek(startOffset);
                    while(rafReader.getFilePointer() < stopOffset) {
                          String line = rafReader.readLine();
                          linesWritten += 1;
                          if(linesWritten % 20000 == 0) {
                                linesWritten = 1;
                                updateProgress(rafReader.getFilePointer(), fileSize);
                          }
                          if(line == null) {
                              break;
                          } else {
                              writer.write(line);
                              writer.write(0xa); // 0xa = LF = \n
                          }
                    }
                    if(afterStartOffset > 0) {
                        rafReader.seek(afterStartOffset);
                        while(rafReader.getFilePointer() < fileSize) {
                            String line = rafReader.readLine();
                            linesWritten++;
                            if(linesWritten % 20000 == 0) {
                                linesWritten = 1;
                                updateProgress(rafReader.getFilePointer(), fileSize);
                            }
                            if(line == null) {
                                break;
                            } else {
                                writer.write(line);
                                writer.write(0xa); // 0xa = LF = \n
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (rafReader != null) {
                        try {
                            rafReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (rafWriter != null) {
                        try {
                            rafWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(writer != null) {
                        try {
                            writer.flush();
                            writer.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                File pgn = new File(pgnFilename);
                pgn.delete();
                tmpFile.renameTo(pgn);
                updateProgress(fileSize, fileSize);
                return null;
            }
        };


        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            unregisterRunningTask(task);
            task.getValue();
            stage.close();
            if(this.dialogDatabase != null) {
                dialogDatabase.updateTable();
            }
            this.filename = pgnFilename;
            open();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(false);
        registerRunningTask(task);
        thread.start();

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

        final String tmpFilename = this.filename;

        entries.clear();
        System.gc();

        Task<ObservableList<PgnDatabaseEntry>> task = new Task<>() {
            @Override protected ObservableList<PgnDatabaseEntry> call() throws Exception {

                ArrayList<PgnDatabaseEntry> newEntries = new ArrayList<>();

                boolean inComment = false;
                long game_pos = -1;
                PgnDatabaseEntry current = null;
                long last_pos = 0;

                // check if we actually read at least one game;
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
                                current = new PgnDatabaseEntry();
                                //System.out.println(currentLine);

                            }
                            last_pos = raf.getFilePointer();
                            if (currentLine.length() > 4) {
                                    int spaceOffset = currentLine.indexOf(' ');
                                    int firstQuote = currentLine.indexOf('"');
                                    int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                                    String tag = currentLine.substring(1, spaceOffset);
                                    if(secondQuote > firstQuote) {
                                        String value = currentLine.substring(firstQuote + 1, secondQuote);
                                        String valueEncoded = new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
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
                dialogDatabase.table.scrollTo(0);
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


        Task<ObservableList<PgnDatabaseEntry>> task = new Task<>() {
            @Override protected ObservableList<PgnDatabaseEntry> call() throws Exception {

                final ArrayList<Long> indices = new ArrayList<Long>();
                for(PgnDatabaseEntry entry : entries) {
                    indices.add(entry.getOffset());
                }

                PgnReader reader = new PgnReader();
                ArrayList<PgnDatabaseEntry> foundEntries = new ArrayList<>();
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
