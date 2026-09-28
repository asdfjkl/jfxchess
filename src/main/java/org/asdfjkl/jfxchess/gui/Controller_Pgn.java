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

import org.asdfjkl.jfxchess.lib.ChessDatabase;
import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.GameInfo;
import org.asdfjkl.jfxchess.lib.PgnReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

public class Controller_Pgn {

    private final PgnReader reader;
    private final Model_JFXChess model;

    public Controller_Pgn(Model_JFXChess model) {
        this.reader = new PgnReader();
        this.model = model;
    }

    // === Opening a PGN via Database -> Open Menu
    public ActionListener openFile() {
        return e -> openAndScanPgn();
    }

    public ActionListener openDatabase() {
        return openFile();
    }

    public ActionListener createNewDatabase() {
        return e -> createNewDatabaseAction();
    }

    public boolean createNewDatabaseAction() {
        JFileChooser chooser;
        File lastSaveDir = model.getLastSaveDirPath();
        if (lastSaveDir != null && lastSaveDir.exists() && lastSaveDir.isDirectory()) {
            chooser = new JFileChooser(lastSaveDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        FileNameExtensionFilter scidFilter = new FileNameExtensionFilter("SCID5 Databases (*.si5)", "si5");
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.setFileFilter(pgnFilter);
        chooser.setAcceptAllFileFilterUsed(false);

        try {
            int result = chooser.showSaveDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile != null) {
                    model.setLastSaveDirPath(chooser.getCurrentDirectory());
                    String filename = selectedFile.getAbsolutePath();
                    String lower = filename.toLowerCase();
                    if (!lower.endsWith(".pgn") && !lower.endsWith(".si5")) {
                        if (chooser.getFileFilter() == scidFilter) {
                            filename += ".si5";
                        } else {
                            filename += ".pgn";
                        }
                    }
                    ChessDatabase database = getDatabase(filename);
                    database.createNew(filename);
                    model.setActiveDatabase(database);
                    model.detachAllSessions();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Error creating new database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public ActionListener closeDatabase() {
        return e -> {
            try {
                model.closeActiveDatabase();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error closing database: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
    }

    private void openAndScanPgn() {
        File lastDir = model.getLastOpenedDirPath();
        JFileChooser chooser;
        if (lastDir != null && lastDir.exists() && lastDir.isDirectory()) {
            chooser = new JFileChooser(lastDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter allDbsFilter = new FileNameExtensionFilter("Chess Databases (*.pgn, *.si5)", "pgn", "si5");
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        FileNameExtensionFilter scidFilter = new FileNameExtensionFilter("SCID5 Databases (*.si5)", "si5");
        chooser.addChoosableFileFilter(allDbsFilter);
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.setFileFilter(allDbsFilter);
        chooser.setAcceptAllFileFilterUsed(true);
        try {
            int result = chooser.showOpenDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                model.setLastOpenedDirPath(chooser.getCurrentDirectory());
                if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
                    String filename = selectedFile.getAbsolutePath();
                    String lower = filename.toLowerCase();
                    if (!lower.endsWith(".pgn") && !lower.endsWith(".si5") && !lower.endsWith(".sn5") && !lower.endsWith(".sg5")) {
                        JOptionPane.showMessageDialog(model.mainFrameRef,
                                "Unsupported database format. Please select a .pgn or .si5 file.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ChessDatabase database = getDatabase(filename);
                    ChessScanWorker worker = new ChessScanWorker(database,
                            entriesFromWorker -> onScanPgnCompletion(database)
                    );
                    DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning Database");
                    worker.execute();
                    dlgProgress.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(model.mainFrameRef,
                            "Error reading file.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Error opening database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onScanPgnCompletion(ChessDatabase database) {
        model.setActiveDatabase(database);

        if (database.getIndex().size() == 1) {
            try {
                openGameInNewSession(database, database.getIndex().get(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (database.getIndex().size() > 1) {
            model.setShortcutsEnabled(false);
            GameInfo currentInfo = getCurrentSessionGameInfo(database);
            DialogDatabase dlgDatabase = new DialogDatabase(model.mainFrameRef, database, this, currentInfo);
            dlgDatabase.setVisible(true);
            model.setShortcutsEnabled(true);
            if (dlgDatabase.isConfirmed()) {
                GameInfo gameInfo = dlgDatabase.getSelectedGame();
                try {
                    openGameInNewSession(database, gameInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ==== Save Game (Game -> Save Game), main function
    public ActionListener saveGame() {
        return e -> saveGame(model.getGameSession());
    }

    private void saveGame(GameSession gameSession) {
        ChessDatabase database = getCurrentDatabase();
        boolean isDbOpen = (database != null && database.isOpen());
        PgnSourceReference source = gameSession.getPgnSourceReference();
        GameInfo currentGameInfo = null;

        if (isDbOpen && source != null && source.getDocumentPath().equals(database.getPath())) {
            for (GameInfo info : database.getIndex()) {
                if (info.getId().equals(source.getGameId().getValue())) {
                    currentGameInfo = info;
                    break;
                }
            }
        }

        boolean replaceAllowed = (isDbOpen && currentGameInfo != null);

        // show dialog
        model.setShortcutsEnabled(false);
        DialogSave dlgSave = new DialogSave(model.mainFrameRef, replaceAllowed);
        dlgSave.setVisible(true);
        model.setShortcutsEnabled(true);
        int res = dlgSave.getResult();
        if (res == DialogSave.SAVE_REPLACE && currentGameInfo != null) {
            replaceCurrentPgn(gameSession, database, currentGameInfo);
        } else if (res == DialogSave.SAVE_NEW_APPEND) {
            if (isDbOpen) {
                appendToCurrentPGN(gameSession);
            } else {
                createNewDatabaseAndSave(gameSession);
            }
        }
    }

    private void createNewDatabaseAndSave(GameSession gameSession) {
        JFileChooser chooser;
        File lastSaveDir = model.getLastSaveDirPath();
        if (lastSaveDir != null && lastSaveDir.exists() && lastSaveDir.isDirectory()) {
            chooser = new JFileChooser(lastSaveDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        FileNameExtensionFilter scidFilter = new FileNameExtensionFilter("SCID5 Databases (*.si5)", "si5");
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.setFileFilter(pgnFilter);
        chooser.setAcceptAllFileFilterUsed(false);

        try {
            int result = chooser.showSaveDialog(model.mainFrameRef);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile != null) {
                    model.setLastSaveDirPath(chooser.getCurrentDirectory());
                    String filename = selectedFile.getAbsolutePath();
                    String lower = filename.toLowerCase();
                    if (!lower.endsWith(".pgn") && !lower.endsWith(".si5")) {
                        if (chooser.getFileFilter() == scidFilter) {
                            filename += ".si5";
                        } else {
                            filename += ".pgn";
                        }
                    }
                    ChessDatabase database = getDatabase(filename);
                    database.createNew(filename);
                    model.setActiveDatabase(database);
                    model.detachAllSessions();

                    ChessSaveGameWorker worker = new ChessSaveGameWorker(database, gameSession.getGame(), (savedInfo, error) -> {
                        if (error != null) {
                            JOptionPane.showMessageDialog(model.mainFrameRef,
                                    "Error saving into new database: " + error.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        } else if (savedInfo != null) {
                            setCurrentSessionSource(gameSession, database, savedInfo);
                        }
                    });
                    DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Saving Game");
                    worker.execute();
                    dlgProgress.setVisible(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Error saving into new database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    // save, case c) Append to current PGN
    public void appendToCurrentPGN() {
        appendToCurrentPGN(model.getGameSession());
    }

    private void appendToCurrentPGN(GameSession gameSession) {
        Game g = gameSession.getGame();
        ChessDatabase database = getCurrentDatabase();
        if (database == null) {
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "No chess database is open",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ChessSaveGameWorker worker = new ChessSaveGameWorker(database, g, (savedInfo, error) -> {
            if (error != null) {
                JOptionPane.showMessageDialog(model.mainFrameRef,
                        "Error saving game: " + error.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else if (savedInfo != null) {
                setCurrentSessionSource(gameSession, database, savedInfo);
            }
        });
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Saving Game");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public void replaceCurrentPgn(ChessDatabase database, GameInfo currentGameInfo) {
        replaceCurrentPgn(model.getGameSession(), database, currentGameInfo);
    }

    private void replaceCurrentPgn(GameSession gameSession,
                                   ChessDatabase database,
                                   GameInfo currentGameInfo) {
        ChessReplaceGameWorker worker = new ChessReplaceGameWorker(database, gameSession.getGame(), currentGameInfo,
                resultString -> onReplaceCurrentPgnFinished(
                        gameSession, database, currentGameInfo, resultString));
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Replacing Game");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public void onReplaceCurrentPgnFinished(String resultString) {
        GameSession gameSession = model.getGameSession();
        PgnSourceReference source = gameSession.getPgnSourceReference();
        ChessDatabase database = getCurrentDatabase();
        if (source == null || database == null) {
            return;
        }
        GameInfo targetInfo = null;
        for (GameInfo info : database.getIndex()) {
            if (info.getId().equals(source.getGameId().getValue())) {
                targetInfo = info;
                break;
            }
        }
        if (targetInfo != null) {
            onReplaceCurrentPgnFinished(gameSession, database, targetInfo, resultString);
        }
    }

    private void onReplaceCurrentPgnFinished(GameSession gameSession,
                                             ChessDatabase database,
                                             GameInfo currentGameInfo,
                                             String resultString) {
        if (!resultString.equals("SUCCESS")) {
            String msg = "Error replacing Game";
            if (!resultString.equals("CANCELLED")) {
                msg += "\n" + resultString;
            }
            JOptionPane.showMessageDialog(null,
                    msg,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                model.setGame(gameSession, database.loadGame(currentGameInfo));
                setCurrentSessionSource(gameSession, database, currentGameInfo);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    public void reloadPgn() {
        ChessDatabase database = getCurrentDatabase();
        if (database == null) {
            return;
        }
        ChessScanWorker worker = new ChessScanWorker(database, entriesFromWorker -> { });
        DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning Database");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    public ActionListener showDatabase() {
        return e -> {
            ChessDatabase database = getCurrentDatabase();
            if (database == null) {
                return;
            }
            model.setShortcutsEnabled(false);
            GameInfo currentInfo = getCurrentSessionGameInfo(database);
            DialogDatabase dlgDatabase = new DialogDatabase(model.mainFrameRef, database, this, currentInfo);
            model.setShortcutsEnabled(true);
            dlgDatabase.setVisible(true);
            if (dlgDatabase.isConfirmed()) {
                GameInfo gameInfo = dlgDatabase.getSelectedGame();
                try {
                    openGameInNewSession(database, gameInfo);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public void deleteGame(GameInfo gameInfo) {
        ChessDatabase database = getCurrentDatabase();
        if (database == null) {
            throw new IllegalStateException("No chess database is open");
        }
        try {
            database.deleteGame(gameInfo);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public Game loadGameAt(int index) {
        ChessDatabase database = getCurrentDatabase();
        if (database == null) {
            return null;
        }
        try {
            if (index >= 0 && index < database.getIndex().size()) {
                return database.loadGame(index);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ActionListener goToNextGameInDatabase() {
        return e -> {
            GameSession gameSession = model.getGameSession();
            ChessDatabase database = getCurrentDatabase();
            if (database == null) {
                return;
            }
            int currentIdx = getCurrentSessionGameIndex(gameSession, database);
            int nextIdx = currentIdx + 1;
            if (nextIdx >= 0 && nextIdx < database.getIndex().size()) {
                GameInfo nextInfo = database.getIndex().get(nextIdx);
                try {
                    Game g = database.loadGame(nextInfo);
                    model.setGame(gameSession, g);
                    setCurrentSessionSource(gameSession, database, nextInfo);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public ActionListener goToPrevGameInDatabase() {
        return e -> {
            GameSession gameSession = model.getGameSession();
            ChessDatabase database = getCurrentDatabase();
            if (database == null) {
                return;
            }
            int currentIdx = getCurrentSessionGameIndex(gameSession, database);
            int prevIdx = currentIdx - 1;
            if (prevIdx >= 0 && prevIdx < database.getIndex().size()) {
                GameInfo prevInfo = database.getIndex().get(prevIdx);
                try {
                    Game g = database.loadGame(prevInfo);
                    model.setGame(gameSession, g);
                    setCurrentSessionSource(gameSession, database, prevInfo);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    private int getCurrentSessionGameIndex(GameSession session, ChessDatabase database) {
        if (session == null || session.getPgnSourceReference() == null || database == null) {
            return -1;
        }
        UUID gameId = session.getPgnSourceReference().getGameId().getValue();
        ArrayList<GameInfo> index = database.getIndex();
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i).getId().equals(gameId)) {
                return i;
            }
        }
        return -1;
    }

    private GameInfo getCurrentSessionGameInfo(ChessDatabase database) {
        GameSession session = model.getGameSession();
        int idx = getCurrentSessionGameIndex(session, database);
        if (idx >= 0 && idx < database.getIndex().size()) {
            return database.getIndex().get(idx);
        }
        return null;
    }

    private ChessDatabase getDatabase(String filename) {
        return model.getWorkspace().getOrCreateDatabase(Path.of(filename));
    }

    private ChessDatabase getCurrentDatabase() {
        return model.getActiveDatabase();
    }

    private void openGameInNewSession(ChessDatabase database, GameInfo gameInfo)
            throws IOException {
        GameSession session = model.openGameInNewSession(database.loadGame(gameInfo));
        long rev = database.getRevision();
        session.setPgnSourceReference(new PgnSourceReference(database.getPath(), gameInfo.getId(), rev));
    }

    private void setCurrentSessionSource(GameSession gameSession,
                                         ChessDatabase database,
                                         GameInfo gameInfo) {
        long rev = database.getRevision();
        gameSession.setPgnSourceReference(new PgnSourceReference(database.getPath(), gameInfo.getId(), rev));
        gameSession.markClean();
    }
}
