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

    public Model_JFXChess getModel() {
        return model;
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
        FileNameExtensionFilter cbhFilter = new FileNameExtensionFilter("ChessBase Classic Databases (*.cbh)", "cbh");
        FileNameExtensionFilter twoCbhFilter = new FileNameExtensionFilter("ChessBase Modern Databases (*.2cbh)", "2cbh");
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.addChoosableFileFilter(cbhFilter);
        chooser.addChoosableFileFilter(twoCbhFilter);
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
                    if (!lower.endsWith(".pgn") && !lower.endsWith(".si5")
                            && !lower.endsWith(".cbh") && !lower.endsWith(".2cbh")) {
                        if (chooser.getFileFilter() == scidFilter) {
                            filename += ".si5";
                        } else if (chooser.getFileFilter() == cbhFilter) {
                            filename += ".cbh";
                        } else if (chooser.getFileFilter() == twoCbhFilter) {
                            filename += ".2cbh";
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

    public ActionListener importGames() {
        return e -> {
            ChessDatabase targetDatabase = getCurrentDatabase();
            if (targetDatabase == null || !targetDatabase.isOpen()) {
                return;
            }
            openAndScanForImport(targetDatabase);
        };
    }

    private void openAndScanForImport(ChessDatabase targetDatabase) {
        if (targetDatabase == null || targetDatabase.isReadOnly()) {
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Cannot import games into a read-only database.",
                    "Read-Only Database",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        File lastDir = model.getLastOpenedDirPath();
        JFileChooser chooser;
        if (lastDir != null && lastDir.exists() && lastDir.isDirectory()) {
            chooser = new JFileChooser(lastDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter allDbsFilter = new FileNameExtensionFilter(
                "Chess Databases (*.pgn, *.si5, *.cbh, *.2cbh)", "pgn", "si5", "cbh", "2cbh");
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        FileNameExtensionFilter scidFilter = new FileNameExtensionFilter("SCID5 Databases (*.si5)", "si5");
        FileNameExtensionFilter cbFilter = new FileNameExtensionFilter("ChessBase Databases (*.cbh, *.2cbh)", "cbh", "2cbh");
        chooser.addChoosableFileFilter(allDbsFilter);
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.addChoosableFileFilter(cbFilter);
        chooser.setFileFilter(allDbsFilter);
        chooser.setAcceptAllFileFilterUsed(true);

        try {
            model.setShortcutsEnabled(false);
            int result = chooser.showOpenDialog(model.mainFrameRef);
            model.setShortcutsEnabled(true);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                model.setLastOpenedDirPath(chooser.getCurrentDirectory());
                if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
                    String filename = selectedFile.getAbsolutePath();
                    String lower = filename.toLowerCase();
                    if (!isSupportedDatabaseExtension(lower)) {
                        JOptionPane.showMessageDialog(model.mainFrameRef,
                                "Unsupported database format. Please select a .pgn, .si5, .cbh, or .2cbh file.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ChessDatabase sourceDatabase = ChessDatabase.openDatabase(filename);
                    if (sourceDatabase.getPath() != null && sourceDatabase.getPath().equals(targetDatabase.getPath())) {
                        sourceDatabase.close();
                        JOptionPane.showMessageDialog(model.mainFrameRef,
                                "Cannot import a database into itself.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ChessScanWorker worker = new ChessScanWorker(sourceDatabase,
                            entriesFromWorker -> onScanImportSourceCompletion(sourceDatabase, targetDatabase)
                    );
                    DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning Database");
                    model.setShortcutsEnabled(false);
                    worker.execute();
                    dlgProgress.setVisible(true);
                    model.setShortcutsEnabled(true);
                    if (worker.isCancelled()) {
                        try {
                            sourceDatabase.close();
                        } catch (IOException ignored) {
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(model.mainFrameRef,
                            "Error reading file.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            model.setShortcutsEnabled(true);
            e.printStackTrace();
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Error opening source database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onScanImportSourceCompletion(ChessDatabase sourceDatabase, ChessDatabase targetDatabase) {
        model.setShortcutsEnabled(false);
        DialogImportGames dlgImport = new DialogImportGames(model.mainFrameRef, sourceDatabase, targetDatabase);
        dlgImport.setVisible(true);
        model.setShortcutsEnabled(true);

        if (dlgImport.isConfirmed()) {
            ArrayList<GameInfo> selectedGames = dlgImport.getSelectedGames();
            ChessImportGamesWorker importWorker = new ChessImportGamesWorker(
                    sourceDatabase,
                    targetDatabase,
                    selectedGames
            );
            DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, importWorker, "Importing Games");
            model.setShortcutsEnabled(false);
            importWorker.execute();
            dlgProgress.setVisible(true);
            model.setShortcutsEnabled(true);
        } else {
            try {
                sourceDatabase.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean isSupportedDatabaseExtension(String lowerFilename) {
        return lowerFilename.endsWith(".pgn")
                || lowerFilename.endsWith(".si5")
                || lowerFilename.endsWith(".sn5")
                || lowerFilename.endsWith(".sg5")
                || isChessBaseExtension(lowerFilename);
    }

    private static boolean isChessBaseExtension(String lowerFilename) {
        return lowerFilename.endsWith(".cbh")
                || lowerFilename.endsWith(".cbg")
                || lowerFilename.endsWith(".cba")
                || lowerFilename.endsWith(".cbp")
                || lowerFilename.endsWith(".cbt")
                || lowerFilename.endsWith(".cbc")
                || lowerFilename.endsWith(".cbs")
                || lowerFilename.endsWith(".2cbh")
                || lowerFilename.endsWith(".2cbg")
                || lowerFilename.endsWith(".2cba")
                || lowerFilename.endsWith(".2lid");
    }

    private void openAndScanPgn() {
        File lastDir = model.getLastOpenedDirPath();
        JFileChooser chooser;
        if (lastDir != null && lastDir.exists() && lastDir.isDirectory()) {
            chooser = new JFileChooser(lastDir);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter allDbsFilter = new FileNameExtensionFilter(
                "Chess Databases (*.pgn, *.si5, *.cbh, *.2cbh)", "pgn", "si5", "cbh", "2cbh");
        FileNameExtensionFilter pgnFilter = new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn");
        FileNameExtensionFilter scidFilter = new FileNameExtensionFilter("SCID5 Databases (*.si5)", "si5");
        FileNameExtensionFilter cbFilter = new FileNameExtensionFilter("ChessBase Databases (*.cbh, *.2cbh)", "cbh", "2cbh");
        chooser.addChoosableFileFilter(allDbsFilter);
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.addChoosableFileFilter(cbFilter);
        chooser.setFileFilter(allDbsFilter);
        chooser.setAcceptAllFileFilterUsed(true);
        try {
            model.setShortcutsEnabled(false);
            int result = chooser.showOpenDialog(model.mainFrameRef);
            model.setShortcutsEnabled(true);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                model.setLastOpenedDirPath(chooser.getCurrentDirectory());
                if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
                    String filename = selectedFile.getAbsolutePath();
                    String lower = filename.toLowerCase();
                    if (!isSupportedDatabaseExtension(lower)) {
                        JOptionPane.showMessageDialog(model.mainFrameRef,
                                "Unsupported database format. Please select a .pgn, .si5, .cbh, or .2cbh file.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ChessDatabase database = getDatabase(filename);
                    ChessScanWorker worker = new ChessScanWorker(database,
                            entriesFromWorker -> onScanPgnCompletion(database)
                    );
                    DialogProgress dlgProgress = new DialogProgress(model.mainFrameRef, worker, "Scanning Database");
                    model.setShortcutsEnabled(false);
                    worker.execute();
                    dlgProgress.setVisible(true);
                    model.setShortcutsEnabled(true);
                } else {
                    JOptionPane.showMessageDialog(model.mainFrameRef,
                            "Error reading file.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            model.setShortcutsEnabled(true);
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
        boolean isReadOnly = (isDbOpen && database.isReadOnly());
        PgnSourceReference source = gameSession.getPgnSourceReference();
        GameInfo currentGameInfo = null;

        if (isDbOpen && !isReadOnly && source != null && source.getDocumentPath().equals(database.getPath())) {
            for (GameInfo info : database.getIndex()) {
                if (info.getId().equals(source.getGameId().getValue())) {
                    currentGameInfo = info;
                    break;
                }
            }
        }

        boolean appendAllowed = !isReadOnly;
        boolean replaceAllowed = (isDbOpen && !isReadOnly && currentGameInfo != null);

        // show dialog
        model.setShortcutsEnabled(false);
        DialogSave dlgSave = new DialogSave(model.mainFrameRef, appendAllowed, replaceAllowed);
        dlgSave.setVisible(true);
        model.setShortcutsEnabled(true);
        int res = dlgSave.getResult();
        if (isReadOnly) {
            return;
        }
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
        FileNameExtensionFilter cbhFilter = new FileNameExtensionFilter("ChessBase Classic Databases (*.cbh)", "cbh");
        FileNameExtensionFilter twoCbhFilter = new FileNameExtensionFilter("ChessBase Modern Databases (*.2cbh)", "2cbh");
        chooser.addChoosableFileFilter(pgnFilter);
        chooser.addChoosableFileFilter(scidFilter);
        chooser.addChoosableFileFilter(cbhFilter);
        chooser.addChoosableFileFilter(twoCbhFilter);
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
                    if (!lower.endsWith(".pgn") && !lower.endsWith(".si5")
                            && !lower.endsWith(".cbh") && !lower.endsWith(".2cbh")) {
                        if (chooser.getFileFilter() == scidFilter) {
                            filename += ".si5";
                        } else if (chooser.getFileFilter() == cbhFilter) {
                            filename += ".cbh";
                        } else if (chooser.getFileFilter() == twoCbhFilter) {
                            filename += ".2cbh";
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
        if (database.isReadOnly()) {
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Cannot save to a read-only database.",
                    "Read-Only Database",
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
        if (database == null || database.isReadOnly()) {
            JOptionPane.showMessageDialog(model.mainFrameRef,
                    "Cannot save to a read-only database.",
                    "Read-Only Database",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
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
            dlgDatabase.setVisible(true);
            model.setShortcutsEnabled(true);
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
        if (database.isReadOnly()) {
            throw new UnsupportedOperationException("Cannot delete games from a read-only database");
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
