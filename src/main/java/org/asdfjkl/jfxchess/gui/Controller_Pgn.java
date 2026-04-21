/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2026 Dominik Klein
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

import org.asdfjkl.jfxchess.lib.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

public class Controller_Pgn {

    private final PgnReader reader;
    private final Model_JFXChess model;

    public Controller_Pgn(Model_JFXChess model) {
        reader = new PgnReader();
        this.model = model;
    }

    // === Opening a PGN via Game -> Open Menu
    public ActionListener openFile() {
        return e -> {
            openAndScanPgn();
        };
    }

    private void openAndScanPgn() {

        File lastDir = model.getLastOpenedDirPath();
        JFileChooser chooser;
        if(lastDir != null && lastDir.exists() && lastDir.isDirectory()) {
            chooser = new JFileChooser(lastDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        chooser.setFileFilter(pgnFilter);
        chooser.setAcceptAllFileFilterUsed(true);
        try {
            int result = chooser.showOpenDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                model.setLastOpenedDirPath(chooser.getCurrentDirectory());
                if (selectedFile != null &&
                        selectedFile.exists() &&
                        selectedFile.canRead()
                ) {
                    String pgnFilename = selectedFile.getAbsolutePath();
                    PgnScanWorker worker = new PgnScanWorker(pgnFilename,
                            reader,
                            entriesFromWorker -> { onScanPgnCompletion(pgnFilename, entriesFromWorker); }
                    );
                    DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning PGN");
                    worker.execute();
                    dlgProgress.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Error reading file.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onScanPgnCompletion(String pgnFilename,
                                     ArrayList<PgnGameInfo> entriesFromWorker) {

        PgnDatabase database = model.getPgnDatabase();
        database.setEntries(entriesFromWorker);
        database.setAbsoluteFilename(pgnFilename);

        if(database.getEntries().size() == 1) {
            // read game from file and show, don't display database dialog
            OptimizedRandomAccessFile raf = null;
            PgnReader reader = new PgnReader();
            try {
                raf = new OptimizedRandomAccessFile(pgnFilename, "r");
                Game g = reader.readGame(raf);
                model.setGame(g);
                database.setIdxOfCurrentlyOpenedGame(0);
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
        }
        if(database.getEntries().size() > 1) {
            model.setShortcutsEnabled(false);
            DialogDatabase dlgDatabase = new DialogDatabase(model.mainFrameRef, model,this);
            dlgDatabase.setVisible(true);
            model.setShortcutsEnabled(true);
            if(dlgDatabase.isConfirmed()) {
                PgnGameInfo gameInfo = dlgDatabase.getSelectedGame();
                OptimizedRandomAccessFile raf = null;
                PgnReader reader = new PgnReader();
                try {
                    raf = new OptimizedRandomAccessFile(pgnFilename, "r");
                    raf.seek(gameInfo.getOffset());
                    Game g = reader.readGame(raf);
                    model.setGame(g);
                    database.setIdxOfCurrentlyOpenedGame(dlgDatabase.getIndexOfSelectedGame());
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
            }
        }
    }

    // ==== Save Game as New PGN (Game -> Save Game), main function
    public ActionListener saveGame() {

        return e -> {

            // first check, which options are actually possible
            // if we haven't opened a pgn before, there can be
            // no replacement/append to the current database
            boolean replaceAllowed = false;
            boolean appendToCurrentAllowed = false;

            PgnDatabase database = model.getPgnDatabase();
            String fnPgnDatabase = database.getAbsoluteFilename();
            long fileSize = 0;
            if(fnPgnDatabase != null) {
                File f = new File(fnPgnDatabase);
                if(f.exists() && !f.isDirectory()) {
                    appendToCurrentAllowed = true;
                    fileSize = f.length();
                }
            }
            long offset1 = -1;
            int gameIdxCurrent = database.getIdxOfCurrentlyOpenedGame();
            if(gameIdxCurrent >= 0 && gameIdxCurrent < database.getEntries().size()) {
                offset1 = database.getEntries().get(gameIdxCurrent).getOffset();
                replaceAllowed = true;
            }
            long offset2 = -1;
            if(gameIdxCurrent + 1 < database.getEntries().size()) {
                offset2 = database.getEntries().get(gameIdxCurrent+1).getOffset();
            } else {
                // no subsequent game, essentially append - take end of file size
                offset2 = fileSize;
            }

            // show dialog
            model.setShortcutsEnabled(false);
            DialogSave dlgSave = new DialogSave(model.mainFrameRef, appendToCurrentAllowed, replaceAllowed);
            dlgSave.setVisible(true);
            model.setShortcutsEnabled(true);
            int res = dlgSave.getResult();
            if (res != DialogSave.CANCEL) {
                if (res == DialogSave.SAVE_NEW) {
                    saveAsNewPGN();
                }
                if (res == DialogSave.APPEND_CURRENT) {
                    appendToCurrentPGN();
                }
                if (res == DialogSave.APPEND_OTHER) {
                    appendToOtherPGN();
                }
                if (res == DialogSave.REPLACE_CURRENT) {
                    PgnPrinter printer = new PgnPrinter();
                    String currentAsPgn = printer.printGame(model.getGame());
                    replaceCurrentPgn(fnPgnDatabase, currentAsPgn, offset1, offset2);
                }
            }
        };
    }

    // save, case a) Save As New...
    private void saveAsNewPGN() {
        JFileChooser chooser;
        File lastSaveDir = model.getLastSaveDirPath();
        if(lastSaveDir != null &&  lastSaveDir.exists() && lastSaveDir.isDirectory()) {
            chooser = new JFileChooser(lastSaveDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        chooser.setFileFilter(pgnFilter);

        chooser.setAcceptAllFileFilterUsed(true);

        PgnDatabase database = model.getPgnDatabase();

        try {
            int result = chooser.showSaveDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile != null) {
                    model.setLastSaveDirPath(chooser.getCurrentDirectory());
                    String pgnFilename = selectedFile.getAbsolutePath();
                    Game g = model.getGame();
                    PgnPrinter printer = new PgnPrinter();
                    printer.writeGame(g, pgnFilename);

                    // now update the internal PGN database to the new file
                    database.setAbsoluteFilename(pgnFilename);
                    database.setIdxOfCurrentlyOpenedGame(0);
                    PgnGameInfo newEntry = new PgnGameInfo();
                    newEntry.setOffset(0);
                    newEntry.setWhite(g.getHeader("White"));
                    newEntry.setBlack(g.getHeader("Black"));
                    newEntry.setDate(g.getHeader("Date"));
                    newEntry.setEvent(g.getHeader("Event"));
                    newEntry.setResult(g.getHeader("Result"));
                    newEntry.setRound(g.getHeader("Round"));
                    newEntry.setSite(g.getHeader("Site"));
                    newEntry.setSite(g.getHeader("WhiteElo"));
                    newEntry.setSite(g.getHeader("BlackElo"));
                    ArrayList<PgnGameInfo> newEntries = new ArrayList<>();
                    newEntries.add(newEntry);
                    database.setEntries(newEntries);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Error saving PGN.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // save, case b) Append to other PGN
    private void appendToOtherPGN() {

        JFileChooser chooser;
        File lastSaveDir = model.getLastSaveDirPath();
        if(lastSaveDir != null &&  lastSaveDir.exists() && lastSaveDir.isDirectory()) {
            chooser = new JFileChooser(lastSaveDir);
        } else {
            chooser = new JFileChooser();
        }

        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        chooser.setFileFilter(pgnFilter);

        chooser.setAcceptAllFileFilterUsed(true);
        PgnDatabase database = model.getPgnDatabase();

        try {
            int result = chooser.showSaveDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile != null &&
                        selectedFile.exists() &&
                        selectedFile.canRead()
                ) {
                    model.setLastSaveDirPath(chooser.getCurrentDirectory());
                    BufferedWriter writer = null;
                    try {
                        PgnPrinter pgnPrinter = new PgnPrinter();
                        writer = new BufferedWriter(new FileWriter(selectedFile, true));
                        String sGame = pgnPrinter.printGame(model.getGame());
                        writer.write(0xa); // 0xa = LF = \n
                        writer.write(0xa); // 0xa = LF = \n
                        writer.write(sGame);
                        writer.write(0xa); // 0xa = LF = \n
                        writer.write(0xa); // 0xa = LF = \n
                        writer.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // now reload this file the current database
                    database.setAbsoluteFilename(selectedFile.getAbsolutePath());
                    reloadPgn();
                    database.setIdxOfCurrentlyOpenedGame(database.getEntries().size()-1);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Error saving PGN.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // save, case c) Append to current PGN
    public void appendToCurrentPGN() {

        Game g = model.getGame();
        PgnDatabase database = model.getPgnDatabase();
        BufferedWriter writer = null;
        try {
            File file = new File(database.getAbsoluteFilename());
            long offset = file.length() + 2;
            PgnPrinter pgnPrinter = new PgnPrinter();
            writer = new BufferedWriter(new FileWriter(database.getAbsoluteFilename(), true));
            String sGame = pgnPrinter.printGame(g);
            writer.write(0xa); // 0xa = LF = \n
            writer.write(0xa); // 0xa = LF = \n
            writer.write(sGame);
            writer.write(0xa); // 0xa = LF = \n
            writer.write(0xa); // 0xa = LF = \n
            writer.close();

            PgnGameInfo newEntry = new PgnGameInfo();
            newEntry.setWhite(g.getHeader("White"));
            newEntry.setBlack(g.getHeader("Black"));
            newEntry.setDate(g.getHeader("Date"));
            newEntry.setOffset(offset);
            newEntry.setEvent(g.getHeader("Event"));
            newEntry.setEco(g.getHeader("Eco"));
            newEntry.setResult(g.getHeader("Result"));
            newEntry.setRound(g.getHeader("Round"));
            newEntry.setSite(g.getHeader("Site"));
            database.getEntries().add(newEntry);
            database.setIdxOfCurrentlyOpenedGame(database.getEntries().size()-1);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void replaceCurrentPgn(String filename, String gamePgn, long offset1, long offset2) {

        PgnReplaceGameWorker worker = new PgnReplaceGameWorker(filename, gamePgn, offset1, offset2,
                resultString -> { onReplaceCurrentPgnFinished(resultString); });
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Replacing PGN");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public void onReplaceCurrentPgnFinished(String resultString) {

        if(!resultString.equals("SUCCESS")) {
            String msg = "Error replacing Game";
            if(!resultString.equals("CANCELLED")) {
                    msg += "\n" + resultString;
            }
            JOptionPane.showMessageDialog(null,
                    msg,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            // something went wrong here, so we can't say anymore where
            // the current game belongs
            model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(-1);
        }
        reloadPgn();
    }

    // function used for: a) after user appends to another pgn we need to open
    // that pgn and read all games + indices b) after user replaces game
    // in current pgn, we need to re-scan to get all new indices
    public void reloadPgn() {
        String pgnFilename = model.getPgnDatabase().getAbsoluteFilename();
        PgnScanWorker worker = new PgnScanWorker(pgnFilename,
                reader,
                entriesFromWorker -> { model.getPgnDatabase().setEntries(entriesFromWorker ); }
        );
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning PGN");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public ActionListener showDatabase() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogDatabase dlgDatabase = new DialogDatabase(model.mainFrameRef, model, this);
            model.setShortcutsEnabled(true);
            dlgDatabase.setVisible(true);
            if(dlgDatabase.isConfirmed()) {
                PgnGameInfo gameInfo = dlgDatabase.getSelectedGame();
                OptimizedRandomAccessFile raf = null;
                PgnReader reader = new PgnReader();
                try {
                    String pgnFilename = model.getPgnDatabase().getAbsoluteFilename();
                    raf = new OptimizedRandomAccessFile(pgnFilename, "r");
                    raf.seek(gameInfo.getOffset());
                    Game g = reader.readGame(raf);
                    model.setGame(g);
                    model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(dlgDatabase.getIndexOfSelectedGame());
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    public void deleteGame(String pgnFilename, long startOffset, long nextGameOffset) {
        reader.deleteGame(pgnFilename, startOffset, nextGameOffset);
    }

    public void deleteGame(String pgnFilename, long startOffset) {
        reader.deleteGame(pgnFilename, startOffset);
    }

    public Game loadGameAt(int index) {

        ArrayList<PgnGameInfo> entries = model.getPgnDatabase().getEntries();
        OptimizedRandomAccessFile raf = null;
        Game g = null;
        try {
            raf = new OptimizedRandomAccessFile(model.getPgnDatabase().getAbsoluteFilename(), "r");
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

    public ActionListener goToNextGameInDatabase() {
        PgnDatabase database = model.getPgnDatabase();
        return e -> {
            int nextIdx = database.getIdxOfCurrentlyOpenedGame() + 1;
            if (nextIdx < database.getEntries().size()) {
                Game g = loadGameAt(nextIdx);
                if (g != null) {
                    model.setGame(g);
                    database.setIdxOfCurrentlyOpenedGame(nextIdx);
                }
            }
        };
    }

    public ActionListener goToPrevGameInDatabase() {
        PgnDatabase database = model.getPgnDatabase();
        return e -> {
            int nextIdx = database.getIdxOfCurrentlyOpenedGame() - 1;
            if (nextIdx >= 0) {
                Game g = loadGameAt(nextIdx);
                if (g != null) {
                    model.setGame(g);
                    database.setIdxOfCurrentlyOpenedGame(nextIdx);
                }
            }
        };
    }

}
