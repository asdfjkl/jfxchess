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
import java.nio.file.Path;
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
                    PgnDocument document = getDocument(selectedFile.getAbsolutePath());
                    PgnScanWorker worker = new PgnScanWorker(document,
                            entriesFromWorker -> { onScanPgnCompletion(document); }
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

    private void onScanPgnCompletion(PgnDocument document) {

        PgnDatabase database = model.getPgnDatabase();
        database.setEntries(document.getEntries());
        database.setAbsoluteFilename(document.getPath().toString());

        if(database.getEntries().size() == 1) {
            // read game from file and show, don't display database dialog
            try {
                openGameInNewSession(document, document.getGameIdAt(0));
                database.setIdxOfCurrentlyOpenedGame(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(database.getEntries().size() > 1) {
            model.setShortcutsEnabled(false);
            DialogDatabase dlgDatabase = new DialogDatabase(model.mainFrameRef, model,this);
            dlgDatabase.setVisible(true);
            model.setShortcutsEnabled(true);
            if(dlgDatabase.isConfirmed()) {
                PgnGameInfo gameInfo = dlgDatabase.getSelectedGame();
                try {
                    openGameInNewSession(document, document.getGameId(gameInfo));
                    database.setIdxOfCurrentlyOpenedGame(dlgDatabase.getIndexOfSelectedGame());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ==== Save Game as New PGN (Game -> Save Game), main function
    public ActionListener saveGame() {

        return e -> {
            saveGame(model.getGameSession());
        };
    }

    private void saveGame(GameSession gameSession) {

            // first check, which options are actually possible
            // if we haven't opened a pgn before, there can be
            // no replacement/append to the current database
            boolean replaceAllowed = false;
            boolean appendToCurrentAllowed = false;

            PgnDatabase database = model.getPgnDatabase();
            String fnPgnDatabase = database.getAbsoluteFilename();
            PgnDocument document = getCurrentDocument();
            if(fnPgnDatabase != null) {
                File f = new File(fnPgnDatabase);
                if(f.exists() && !f.isDirectory()) {
                    appendToCurrentAllowed = true;
                }
            }
            PgnSourceReference source = gameSession.getPgnSourceReference();
            if (document != null && source != null &&
                    source.getDocumentPath().equals(document.getPath()) &&
                    document.indexOf(source.getGameId()) >= 0) {
                replaceAllowed = true;
            }

            // show dialog
            model.setShortcutsEnabled(false);
            DialogSave dlgSave = new DialogSave(model.mainFrameRef, appendToCurrentAllowed, replaceAllowed);
            dlgSave.setVisible(true);
            model.setShortcutsEnabled(true);
            int res = dlgSave.getResult();
            if (res != DialogSave.CANCEL) {
                if (res == DialogSave.SAVE_NEW) {
                    saveAsNewPGN(gameSession);
                }
                if (res == DialogSave.APPEND_CURRENT) {
                    appendToCurrentPGN(gameSession);
                }
                if (res == DialogSave.APPEND_OTHER) {
                    appendToOtherPGN(gameSession);
                }
                if (res == DialogSave.REPLACE_CURRENT) {
                    PgnPrinter printer = new PgnPrinter();
                    String currentAsPgn = printer.printGame(gameSession.getGame());
                    replaceCurrentPgn(gameSession, document, source.getGameId(), currentAsPgn);
                }
            }
    }

    // save, case a) Save As New...
    private void saveAsNewPGN(GameSession gameSession) {
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
                    Game g = gameSession.getGame();
                    PgnDocument document = getDocument(pgnFilename);
                    PgnGameId gameId = document.writeSingleGame(g);
                    database.setAbsoluteFilename(pgnFilename);
                    database.setIdxOfCurrentlyOpenedGame(0);
                    database.setEntries(document.getEntries());
                    setCurrentSessionSource(gameSession, document, gameId);
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
    private void appendToOtherPGN(GameSession gameSession) {

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
                    try {
                        PgnDocument document = getDocument(selectedFile.getAbsolutePath());
                        if (document.getGameIds().isEmpty()) {
                            document.reload();
                        }
                        PgnGameId gameId = document.appendGame(gameSession.getGame());
                        database.setAbsoluteFilename(selectedFile.getAbsolutePath());
                        database.setEntries(document.getEntries());
                        database.setIdxOfCurrentlyOpenedGame(document.indexOf(gameId));
                        setCurrentSessionSource(gameSession, document, gameId);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
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
        appendToCurrentPGN(model.getGameSession());
    }

    private void appendToCurrentPGN(GameSession gameSession) {

        Game g = gameSession.getGame();
        PgnDatabase database = model.getPgnDatabase();
        try {
            PgnDocument document = getCurrentDocument();
            if (document == null) {
                throw new IOException("No PGN database is open");
            }
            PgnGameId gameId = document.appendGame(g);
            database.setEntries(document.getEntries());
            database.setIdxOfCurrentlyOpenedGame(document.indexOf(gameId));
            setCurrentSessionSource(gameSession, document, gameId);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void replaceCurrentPgn(PgnDocument document, PgnGameId gameId, String gamePgn) {
        replaceCurrentPgn(model.getGameSession(), document, gameId, gamePgn);
    }

    private void replaceCurrentPgn(GameSession gameSession,
                                   PgnDocument document,
                                   PgnGameId gameId,
                                   String gamePgn) {
        PgnReplaceGameWorker worker = new PgnReplaceGameWorker(document, gameId, gamePgn,
                resultString -> onReplaceCurrentPgnFinished(
                        gameSession, document, gameId, resultString));
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Replacing PGN");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public void onReplaceCurrentPgnFinished(String resultString) {
        GameSession gameSession = model.getGameSession();
        PgnSourceReference source = gameSession.getPgnSourceReference();
        PgnDocument document = getCurrentDocument();
        if (source == null || document == null) {
            return;
        }
        onReplaceCurrentPgnFinished(gameSession, document, source.getGameId(), resultString);
    }

    private void onReplaceCurrentPgnFinished(GameSession gameSession,
                                              PgnDocument document,
                                              PgnGameId gameId,
                                              String resultString) {

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
        } else {
            model.getPgnDatabase().setEntries(document.getEntries());
            try {
                model.setGame(gameSession, document.loadGame(gameId));
                setCurrentSessionSource(gameSession, document, gameId);
                model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(document.indexOf(gameId));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    // function used for: a) after user appends to another pgn we need to open
    // that pgn and read all games + indices b) after user replaces game
    // in current pgn, we need to re-scan to get all new indices
    public void reloadPgn() {
        PgnDocument document = getCurrentDocument();
        if (document == null) {
            return;
        }
        PgnScanWorker worker = new PgnScanWorker(document,
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
                try {
                    PgnDocument document = getCurrentDocument();
                    if (document == null) {
                        return;
                    }
                    openGameInNewSession(document, document.getGameId(gameInfo));
                    model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(dlgDatabase.getIndexOfSelectedGame());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public void deleteGame(PgnGameInfo gameInfo) {
        PgnDocument document = getCurrentDocument();
        if (document == null) {
            throw new IllegalStateException("No PGN database is open");
        }
        try {
            document.deleteGame(document.getGameId(gameInfo));
            model.getPgnDatabase().setEntries(document.getEntries());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public Game loadGameAt(int index) {

        PgnDocument document = getCurrentDocument();
        if (document == null) {
            return null;
        }
        try {
            if(index < document.getEntries().size()) {
                return document.loadGame(document.getGameIdAt(index));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ActionListener goToNextGameInDatabase() {
        PgnDatabase database = model.getPgnDatabase();
        return e -> {
            GameSession gameSession = model.getGameSession();
            int nextIdx = database.getIdxOfCurrentlyOpenedGame() + 1;
            if (nextIdx < database.getEntries().size()) {
                Game g = loadGameAt(nextIdx);
                if (g != null) {
                    model.setGame(gameSession, g);
                    PgnDocument document = getCurrentDocument();
                    setCurrentSessionSource(gameSession, document, document.getGameIdAt(nextIdx));
                    database.setIdxOfCurrentlyOpenedGame(nextIdx);
                }
            }
        };
    }

    public ActionListener goToPrevGameInDatabase() {
        PgnDatabase database = model.getPgnDatabase();
        return e -> {
            GameSession gameSession = model.getGameSession();
            int nextIdx = database.getIdxOfCurrentlyOpenedGame() - 1;
            if (nextIdx >= 0) {
                Game g = loadGameAt(nextIdx);
                if (g != null) {
                    model.setGame(gameSession, g);
                    PgnDocument document = getCurrentDocument();
                    setCurrentSessionSource(gameSession, document, document.getGameIdAt(nextIdx));
                    database.setIdxOfCurrentlyOpenedGame(nextIdx);
                }
            }
        };
    }

    private PgnDocument getDocument(String filename) {
        return model.getWorkspace().getOrCreateDocument(Path.of(filename));
    }

    private PgnDocument getCurrentDocument() {
        String filename = model.getPgnDatabase().getAbsoluteFilename();
        if (filename == null || filename.isBlank()) {
            return null;
        }
        return getDocument(filename);
    }

    private void openGameInNewSession(PgnDocument document, PgnGameId gameId)
            throws IOException {
        GameSession session = model.openGameInNewSession(document.loadGame(gameId));
        session.setPgnSourceReference(new PgnSourceReference(document.getPath(), gameId,
                document.getRevision()));
    }

    private void setCurrentSessionSource(GameSession gameSession,
                                         PgnDocument document,
                                         PgnGameId gameId) {
        gameSession.setPgnSourceReference(new PgnSourceReference(document.getPath(), gameId,
                document.getRevision()));
        gameSession.markClean();
    }

}
