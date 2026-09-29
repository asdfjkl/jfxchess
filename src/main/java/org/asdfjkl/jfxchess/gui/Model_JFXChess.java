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
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

public class Model_JFXChess {

    private static final int modelVersion = 2610;

    public static final int MAX_PV = 64;
    public static final int MAX_N_ENGINES = 10;
    public static final int MODE_ENTER_MOVES = 0;
    public static final int MODE_ANALYSIS = 1;
    public static final int MODE_PLAY_WHITE = 2;
    public static final int MODE_PLAY_BLACK = 3;
    public static final int MODE_GAME_ANALYSIS = 4;
    public static final int BOTH_PLAYERS = 5;
    public static final int MODE_PLAYOUT_POSITION = 7;

    public static final String THEME_FLATLAF_LIGHT = "com.formdev.flatlaf.FlatLightLaf";
    public static final String THEME_FLATLAF_DARK = "com.formdev.flatlaf.FlatDarkLaf";
    public static final String THEME_FLATLAF_INTELLIJ = "com.formdev.flatlaf.FlatIntelliJLaf";
    public static final String THEME_FLATLAF_DARCULA = "com.formdev.flatlaf.FlatDarculaLaf";
    public static final String THEME_FLATLAF_FRUIT_LIGHT = "com.formdev.flatlaf.themes.FlatMacLightLaf";
    public static final String THEME_FLATLAF_FRUIT_DARK = "com.formdev.flatlaf.themes.FlatMacDarkLaf";
    public static final String THEME_METAL = "javax.swing.plaf.metal.MetalLookAndFeel";
    public static final String THEME_NIMBUS = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
    public static final String THEME_SYSTEM = "system.default";

    private GameSession gameSession;
    private Workspace workspace;
    public boolean wasSaved = false;

    ArrayList<Engine> engines = new ArrayList<>();
    Engine activeEngine = null;
    Engine selectedPlayEngine = null;
    Engine selectedAnalysisEngine = null;

    ArrayList<BotEngine> botEngines = new ArrayList<>();

    private ScreenGeometry screenGeometry = new ScreenGeometry();

    // to make sure that if the user play against the computer/bot
    // he is not able to/does not accidentally move the computer's pieces
    // on the computer's turn

    public String currentBestPv = "";
    public int currentBestEval = 0;
    public int currentMateInMoves = -1;
    public boolean currentIsMate = false;

    public String childBestPv = "";
    public int childBestEval = 0;
    public int childMateInMoves = -1;
    public boolean childIsMate = false;

    public PolyglotExt extBook = new PolyglotExt();

    private Preferences prefs;

    private ChessDatabase activeDatabase;
    private File lastOpenedDirPath = null;
    private File lastSaveDirPath = null;

    private String extBookPath = "";
    public int maxCpus = Runtime.getRuntime().availableProcessors();
    BoardStyle boardStyle;

    private final PropertyChangeSupport pcs =
            new PropertyChangeSupport(this);

    private String lookAndFeel;
    public View_MainFrame mainFrameRef;

    private boolean shortcutsEnabled = true;

    private int fontSizeMoveView = 12;
    private int fontSizeEngineOutput = 12;
    private boolean useCustomFontSizeMoveView = false;
    private boolean useCustomFontSizeEngineOutput = false;

    public Model_JFXChess() {
        Game game = new Game();
        Board b = new Board(true);

        boardStyle = new BoardStyle();
        lookAndFeel = THEME_FLATLAF_INTELLIJ;

        game.getRootNode().setBoard(b);
        gameSession = new GameSession(game);
        String stockfishPath = getStockfishPath();
        Engine stockfish = new Engine();
        stockfish.setName(CONSTANTS.INTERNAL_ENGINE_NAME);
        if(stockfishPath != null) {
            stockfish.setPath(stockfishPath);
        }
        stockfish.setInternal(true);
        engines.add(stockfish);
        selectedAnalysisEngine = stockfish;
        activeEngine = stockfish;

        // manually add for internal stockfish up to 4 mpv
        EngineOption internalMPV = new EngineOption();
        internalMPV.name = "MultiPV";
        internalMPV.spinMin = 1;
        internalMPV.spinMax = 4;
        internalMPV.spinDefault = 1;
        internalMPV.spinValue = 1;
        internalMPV.type = EngineOption.EN_OPT_TYPE_SPIN;

        EngineOption internalElo = new EngineOption();
        internalElo.name = "UCI_Elo";
        internalElo.spinMin = 1320;
        internalElo.spinMax = 3190;
        internalElo.spinDefault = 1320;
        internalElo.spinValue = 3190;
        internalElo.type = EngineOption.EN_OPT_TYPE_SPIN;

        EngineOption internalLimitStrength = new EngineOption();
        internalLimitStrength.name = "UCI_LimitStrength";
        internalLimitStrength.type = EngineOption.EN_OPT_TYPE_CHECK;
        internalLimitStrength.checkStatusDefault = false;
        internalLimitStrength.checkStatusValue = false;

        EngineOption internalThreads = new EngineOption();
        internalThreads.name = "Threads";
        internalThreads.type = EngineOption.EN_OPT_TYPE_SPIN;
        internalThreads.spinMin = 1;
        internalThreads.spinMax = 1024;
        internalThreads.spinDefault = 1;
        internalThreads.spinValue = 1;

        activeEngine.options.add(internalElo);
        activeEngine.options.add(internalMPV);
        activeEngine.options.add(internalLimitStrength);
        activeEngine.options.add(internalThreads);

        // add bots
        String botPath = getBotEnginePath();
        botEngines = BotEngines.createEngines(botPath);
        selectedPlayEngine = botEngines.get(0); // set benny as default

        extBookPath = getExtBookPath();
        File fileExtBookPath = new File(extBookPath);
        if(fileExtBookPath.exists()) {
            extBook.loadBook(fileExtBookPath);
        }
    }



    public void addListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void setBook(File bookPath) {
        if(bookPath.exists()) {
            extBook.loadBook(bookPath);
        }
        pcs.firePropertyChange("bookChanged", null, null);
    }

    public Path getJarPath() {
        try {
            Path jarPath = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path jarDir = jarPath.getParent();
            return jarDir;
        } catch (URISyntaxException e) {
            System.err.println("[ERROR] Failed to resolve JAR location: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error locating engine binary: " + e.getMessage());
            return null;
        }
    }

    private String getStockfishPath() {

        // currently redundant, but let's keep it here if we
        // have different packaging requirements in the future
        Path jarDir = getJarPath();
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            if (jarDir != null) {
                Path engineBinary = jarDir.resolve("engine").resolve("stockfish.exe");
                return engineBinary.toString();
            }
        }
        if(os.contains("linux")) {
            Path engineBinary = null;
            if (jarDir != null) {
                engineBinary = jarDir.resolve("engine").resolve("stockfish_x64");
                return engineBinary.toString();
            }
        }
        return null;
    }

    private String getBotEnginePath() {

        // currently redundant, but let's keep it here if we
        // have different packaging requirements in the future
        Path jarDir = getJarPath();
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            if (jarDir != null) {
                Path engineBinary = jarDir.resolve("engine").resolve("stockfish5.exe");
                return engineBinary.toString();
            }
        }
        if(os.contains("linux")) {
            Path engineBinary = null;
            if (jarDir != null) {
                engineBinary = jarDir.resolve("engine").resolve("stockfish5_x64");
                return engineBinary.toString();
            }
        }
        return null;

    }

    public String getExtBookPath() {

        Path jarDir = getJarPath();
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            Path extBookBinary = null;
            if (jarDir != null) {
                extBookBinary = jarDir.resolve("book").resolve("extbook.bin");
                return extBookBinary.toString();
            }
        }
        if(os.contains("linux")) {
            Path extBookBinary = null;
            if (jarDir != null) {
                extBookBinary = jarDir.resolve("book").resolve("extbook.bin");
                return extBookBinary.toString();
            }
        }
        return null;
    }

    public void setLookAndFeel(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
        pcs.firePropertyChange("switchLaf", "stuff", this.lookAndFeel);
    }

    public String getLookAndFeel() {
        return this.lookAndFeel;
    }


    public Game getGame() {
        return gameSession.getGame();
    }

    public void setGame(Game game) {
        gameSession.setGame(game);
        pcs.firePropertyChange("gameChanged", null, null);
    }

    public void setGame(GameSession gameSession, Game game) {
        Objects.requireNonNull(gameSession, "gameSession");
        gameSession.setGame(game);
        if (this.gameSession == gameSession) {
            pcs.firePropertyChange("gameChanged", null, null);
        }
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public GameSession openGameInNewSession(Game game) {
        if (workspace == null) {
            throw new IllegalStateException("Workspace has not been configured");
        }
        GameSession session = workspace.createSession(game);
        setGameSession(session);
        return session;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = Objects.requireNonNull(gameSession, "gameSession");
        if (workspace != null && workspace.getActiveSession() != gameSession) {
            workspace.setActiveSession(gameSession);
        }
        pcs.firePropertyChange("gameChanged", null, null);
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public int getGameAnalysisForPlayer() { return gameSession.getGameAnalysisForPlayer(); }

    public void setGameAnalysisForPlayer(int player) { gameSession.setGameAnalysisForPlayer(player); }

    public double getGameAnalysisThreshold() { return gameSession.getGameAnalysisThreshold(); }

    public void setGameAnalysisThreshold(double threshold) { gameSession.setGameAnalysisThreshold(threshold); }

    public void setGameAnalysisThinkTimeSecs(int thinktimeSecs) { gameSession.setGameAnalysisThinkTimeSecs(thinktimeSecs); }

    public int getGameAnalysisThinkTimeSecs() { return gameSession.getGameAnalysisThinkTimeSecs(); }

    public void setMode(int mode) {
        gameSession.setMode(mode);
        pcs.firePropertyChange("modeChanged", null, null);
    }

    public void setComputerThinkTimeSecs(int secs) {
        gameSession.setComputerThinkTimeSecs(secs);
    }

    public int getComputerThinkTimeSecs() {
        return gameSession.getComputerThinkTimeSecs();
    }

    public int getMode() {
        return gameSession.getMode();
    }

    public int getMultiPv() {
        return activeEngine.getMultiPV();
    }

    public void setFlipBoard(boolean flipBoard) {
        gameSession.setFlipBoard(flipBoard);
        pcs.firePropertyChange("boardFlipped", null, null);
    }

    public boolean getFlipBoard() {
        return gameSession.getFlipBoard();
    }

    public void setHumanPlayerColor(boolean humanPlayerColor) {
        gameSession.setHumanPlayerColor(humanPlayerColor);
    }

    public boolean getHumanPlayerColor() {
        return gameSession.getHumanPlayerColor();
    }

    public void setMultiPv(int multiPv) {
        if(multiPv >= 1 && multiPv <= activeEngine.getMaxMultiPV() && multiPv <= MAX_PV) {
            activeEngine.setMultiPV(multiPv);
            // this.multiPvChanged = true;
        }
    }

    public String getVersion() {

        int mainVersion = modelVersion / 100;
        int subVersion = (modelVersion % 100) / 10;
        int subSubVersion = ((modelVersion % 100) % 10);

        return mainVersion + "." + subVersion + "." + subSubVersion;

    }

    public boolean isBlockGUI() {
        return gameSession.isBlockGui();
    }

    public void setBlockGUI(boolean blockGUI) {
        gameSession.setBlockGui(blockGUI);
        pcs.firePropertyChange("blockGUI", null, blockGUI);
    }

    public void setShortcutsEnabled(boolean enabled) {
        this.shortcutsEnabled = enabled;
    }

    public boolean getShortcutsEnabled() {
        return shortcutsEnabled;
    }

    public void applyMove(Move m) {
        // after applying a move, we block the GUI
        // when we are playing against the computer
        boolean treeWasChanged = gameSession.applyMove(m);
        if(treeWasChanged) {
            pcs.firePropertyChange("treeChanged", null, null);
        }
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void notifySessionPositionChanged(GameSession changedSession) {
        if (gameSession != changedSession) {
            return;
        }
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void goToChild(int idx) {
        getGame().goToChild(idx);
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void goToParent() {
        getGame().goToParent();
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void setBoardColor(int style) {
        boardStyle.setColorStyle(style);
        pcs.firePropertyChange("boardColor", null, style);
    }

    public void setPieceStyle(int style) {
        boardStyle.setPieceStyle(style);
        pcs.firePropertyChange("pieceStyle", null, style);
    }

    public BoardStyle getBoardStyle() {
        return boardStyle;
    }

    public void setBoardStyle(BoardStyle boardStyle) {
        this.boardStyle = boardStyle;
    }

    public void setPgnHeaders(HashMap<String, String> data) {
        getGame().setPgnHeaders(data);
        pcs.firePropertyChange("pgnHeadersChanged", null, null);
        // Special case: If the PGN header for the result changed, we also need to
        // update the game tree. Therefore, fire treeChanged as well.
        String result = data.get("Result");
        if(result != null) {
            if(result.equals("1-0") && getGame().getResult() != RES_WHITE_WINS) {
                getGame().setResult(RES_WHITE_WINS);
                pcs.firePropertyChange("treeChanged", null, null);
            }
            if(result.equals("0-1") && getGame().getResult() != RES_BLACK_WINS) {
                getGame().setResult(RES_BLACK_WINS);
                pcs.firePropertyChange("treeChanged", null, null);
            }
            if(result.equals("1/2-1/2") && getGame().getResult() != RES_DRAW) {
                getGame().setResult(RES_DRAW);
                pcs.firePropertyChange("treeChanged", null, null);
            }
            if(result.equals("*") && getGame().getResult() != RES_UNDEF) {
                getGame().setResult(RES_UNDEF);
                pcs.firePropertyChange("treeChanged", null, null);
            }
        }


    }

    public void goToNode(int id) {
        try {
            GameNode node = getGame().findNodeById(id);
            getGame().setCurrent(node);
            pcs.firePropertyChange("currentGameNodeChanged", null, null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void seekToEnd() {
        getGame().goToLeaf();
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void seekToBeginning() {
        getGame().goToRoot();
        pcs.firePropertyChange("currentGameNodeChanged", null, null);
    }

    public void setComment(int nodeId, String s) {
        try {
            GameNode node = getGame().findNodeById(nodeId);
            node.setComment(s);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {
        }
    }

    public void setGameResult(int resultCode) {
        getGame().setResult(resultCode);
        if(resultCode == RES_WHITE_WINS) {
            getGame().setHeader("Result", "1-0");
        }
        if(resultCode == CONSTANTS.RES_BLACK_WINS) {
            getGame().setHeader("Result", "0-1");
        }
        if(resultCode == CONSTANTS.RES_DRAW) {
            getGame().setHeader("Result", "1/2-1/2");
        }
        if(resultCode == CONSTANTS.RES_UNDEF) {
            getGame().setHeader("Result", "*");
        }
        pcs.firePropertyChange("treeChanged", null, null);
    }

    public void setComment(String s) {
        try {
            GameNode node = getGame().getCurrentNode();
            node.setComment(s);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {
        }
    }

    public void addNag(int nodeId, int nag) {
        try {
            GameNode node = getGame().findNodeById(nodeId);
            node.addNag(nag);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void removeMoveAnnotations(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            selectedNode.removeNagsInRange(0, CONSTANTS.MOVE_ANNOTATION_UPPER_LIMIT);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void removePosAnnotations(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            selectedNode.removeNagsInRange(CONSTANTS.POSITION_ANNOTATION_LOWER_LIMIT,
                    CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void removeMoveAndPosAnnotation(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            selectedNode.removeNagsInRange(0, CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void moveVariantUp(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            getGame().moveUp(selectedNode);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void moveVariantDown(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            getGame().moveDown(selectedNode);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void deleteVariant(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            getGame().delVariant(selectedNode);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void deleteFromHere(int nodeId) {
        try {
            GameNode selectedNode = getGame().findNodeById(nodeId);
            getGame().delBelow(selectedNode);
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void deleteAllComments() {
        try {
            getGame().removeAllComments();
            pcs.firePropertyChange("treeChanged", null, null);
        } catch(IllegalArgumentException ignored) {

        }
    }

    public void deleteAllVariants() {
        getGame().removeAllVariants();
        pcs.firePropertyChange("treeChanged", null, null);
    }

    public void markTreeChange() {
        pcs.firePropertyChange("treeChanged", null, null);
    }

    public String getCurrentEngineInfo() {
        return gameSession.getCurrentEngineInfo();
    }

    public void setCurrentEngineInfo(String info) {
        gameSession.setCurrentEngineInfo(info);
        pcs.firePropertyChange("engineInfo", null, null);
    }

    public ChessDatabase getActiveDatabase() {
        return activeDatabase;
    }

    public void setActiveDatabase(ChessDatabase activeDatabase) {
        ChessDatabase oldDatabase = this.activeDatabase;
        this.activeDatabase = activeDatabase;
        pcs.firePropertyChange("activeDatabaseChanged", oldDatabase, activeDatabase);
    }

    public void detachAllSessions() {
        if (workspace != null) {
            for (GameSession session : workspace.getSessions()) {
                session.setPgnSourceReference(null);
            }
        }
    }

    public void closeActiveDatabase() throws java.io.IOException {
        if (activeDatabase != null) {
            ChessDatabase db = activeDatabase;
            db.close();
            detachAllSessions();
            setActiveDatabase(null);
        }
    }

    private boolean databaseModifiedWarning = false;

    public boolean isDatabaseModifiedWarning() {
        return databaseModifiedWarning;
    }

    public void setDatabaseModifiedWarning(boolean warning) {
        this.databaseModifiedWarning = warning;
    }

    public File getLastOpenedDirPath() {
        return lastOpenedDirPath;
    }

    public void setLastOpenedDirPath(File lastOpenedDirPath) {
        this.lastOpenedDirPath = lastOpenedDirPath;
    }

    public File getLastSaveDirPath() {
        return lastSaveDirPath;
    }

    public void setLastSaveDirPath(File lastSaveDirPath) {
        this.lastSaveDirPath = lastSaveDirPath;
    }

    public ScreenGeometry getScreenGeometry() {
        return screenGeometry;
    }

    public void setScreenGeometry(ScreenGeometry screenGeometry) {
        this.screenGeometry = screenGeometry;
    }

    public int getHorizontalDividerLocation() {
        return screenGeometry.dividerHorizontal;
    }

    public int getVerticalDividerLocation() {
        return screenGeometry.dividerVertical;
    }

    public void setDividerHorizontal(int location) {
        if (location > 0 && screenGeometry.dividerHorizontal != location) {
            int old = screenGeometry.dividerHorizontal;
            screenGeometry.dividerHorizontal = location;
            pcs.firePropertyChange("dividerHorizontal", old, location);
        }
    }

    public void setDividerVertical(int location) {
        if (location > 0 && screenGeometry.dividerVertical != location) {
            int old = screenGeometry.dividerVertical;
            screenGeometry.dividerVertical = location;
            pcs.firePropertyChange("dividerVertical", old, location);
        }
    }

    public void setDividerLocations(int horizontal, int vertical) {
        setDividerHorizontal(horizontal);
        setDividerVertical(vertical);
    }

    public void resetDividerLocations() {
        setDividerLocations(600, 450);
    }

    public void save() {

        prefs = Preferences.userRoot().node("/org/asdfjkl/jfxchess");

        // Version
        prefs.putInt("modelVersion",modelVersion);

        // currently open game
        PgnPrinter printer = new PgnPrinter();
        String pgn = printer.printGame(getGame());
        prefs.put("currentGame", pgn);

        // All open sessions
        if (workspace != null) {
            List<GameSession> sessions = workspace.getSessions();
            prefs.putInt("SESSION_COUNT", sessions.size());
            GameSession activeSess = workspace.getActiveSession();
            int activeIndex = sessions.indexOf(activeSess);
            prefs.putInt("ACTIVE_SESSION_INDEX", activeIndex >= 0 ? activeIndex : 0);

            for (int i = 0; i < sessions.size(); i++) {
                GameSession sess = sessions.get(i);
                String sessionPgn = printer.printGame(sess.getGame());
                prefs.put("SESSION_" + i + "_PGN", sessionPgn);

                int dbIndex = -1;
                PgnSourceReference ref = sess.getPgnSourceReference();
                if (ref != null && activeDatabase != null && activeDatabase.isOpen()) {
                    if (ref.getDocumentPath().toAbsolutePath().normalize().equals(
                            activeDatabase.getPath().toAbsolutePath().normalize())) {
                        ArrayList<GameInfo> indexList = activeDatabase.getIndex();
                        for (int j = 0; j < indexList.size(); j++) {
                            if (indexList.get(j).getId().equals(ref.getGameId().getValue())) {
                                dbIndex = j;
                                break;
                            }
                        }
                    }
                }
                prefs.putInt("SESSION_" + i + "_DB_INDEX", dbIndex);
            }
        } else {
            prefs.putInt("SESSION_COUNT", 0);
        }

        // Active database persistence
        if (activeDatabase != null && activeDatabase.isOpen() && activeDatabase.getFilename() != null && !activeDatabase.getFilename().isBlank()) {
            String dbFilename = activeDatabase.getFilename();
            prefs.put("LAST_DATABASE_PATH", dbFilename);
            String lastModifiedStr = "";
            try {
                Path p = activeDatabase.getPath();
                if (p != null && Files.exists(p)) {
                    lastModifiedStr = Files.getLastModifiedTime(p).toString();
                }
            } catch (Exception e) {
                lastModifiedStr = "";
            }
            prefs.put("LAST_DATABASE_MODIFIED", lastModifiedStr);
            try {
                activeDatabase.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            prefs.put("LAST_DATABASE_PATH", "");
            prefs.put("LAST_DATABASE_MODIFIED", "");
        }

        // last used directories
        if(lastOpenedDirPath != null) {
            prefs.put("lastOpenDir", lastOpenedDirPath.toString());
        }
        if(lastSaveDirPath != null) {
            prefs.put("lastSaveDir", lastSaveDirPath.toString());
        }

        // style of board & pieces
        prefs.putInt("COLOR_STYLE", boardStyle.getColorStyle());
        prefs.putInt("PIECE_STYLE", boardStyle.getPieceStyle());

        // look and feel (ui theme)
        prefs.put("LOOK_AND_FEEL", lookAndFeel);

        // engines: first clean up preferences, since preferences for
        // engines which have been removed may still be there.
        for(int i=0;i<MAX_N_ENGINES;i++) {
            prefs.remove("ENGINE"+i);
        }
        for(int i=1;i<engines.size();i++) {
            Engine engine = engines.get(i);
            String engineString = engine.writeToString();
            prefs.put("ENGINE"+i, engineString);
        }
        int activeEngineIdx = engines.indexOf(activeEngine);
        if(activeEngineIdx > 0) {
            prefs.putInt("ACTIVE_ENGINE_IDX", engines.indexOf(activeEngine));
        } else {
            prefs.putInt("ACTIVE_ENGINE_IDX", 0);
        }

        // path of opening book
        if(!extBookPath.isEmpty()) {
            prefs.put("EXT_BOOK_PATH_FILE", extBookPath);
        }

        // settings for game analysis
        prefs.putInt("GAME_ANALYSIS_SECS", getGameAnalysisThinkTimeSecs());
        prefs.putDouble("GAME_ANALYSIS_THRESHOLD", getGameAnalysisThreshold());

        // screen geometry (i.e. window size, position of dividers)
        if (mainFrameRef != null) {
            Rectangle bounds = mainFrameRef.getBounds();

            prefs.putInt("WINDOW_WIDTH" , bounds.width);
            prefs.putInt("WINDOW_HEIGHT" , bounds.height);
            prefs.putInt("WINDOW_POSITION_X" , bounds.x);
            prefs.putInt("WINDOW_POSITION_Y" , bounds.y);

            boolean maximized =
                    (mainFrameRef.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
            prefs.putBoolean("WINDOW_MAXIMIZED", maximized);
        }

        prefs.putInt("DIVIDER_HORIZONTAL", screenGeometry.dividerHorizontal);
        prefs.putInt("DIVIDER_VERTICAL", screenGeometry.dividerVertical);

        prefs.putInt("FONT_SIZE_MOVES", fontSizeMoveView);
        prefs.putBoolean("USE_CUSTOM_FONT_SIZE_MOVES", useCustomFontSizeMoveView);

        prefs.putInt("FONT_SIZE_ENGINE", fontSizeEngineOutput);
        prefs.putBoolean("USE_CUSTOM_FONT_SIZE_ENGINE", useCustomFontSizeEngineOutput);

    }


    public void restore() {

        prefs = Preferences.userRoot().node("/org/asdfjkl/jfxchess");
        int mVersion = prefs.getInt("modelVersion", 0);

        // only restore if modelVersion fits - otherwise
        // it was from a different version
        if(mVersion == modelVersion) {

            // restore sessions
            PgnReader reader = new PgnReader();
            int sessionCount = prefs.getInt("SESSION_COUNT", 0);
            List<Game> restoredGames = new ArrayList<>();
            List<Integer> restoredDbIndices = new ArrayList<>();

            if (sessionCount > 0) {
                for (int i = 0; i < sessionCount; i++) {
                    String pgnStr = prefs.get("SESSION_" + i + "_PGN", "");
                    int dbIdx = prefs.getInt("SESSION_" + i + "_DB_INDEX", -1);
                    if (!pgnStr.isEmpty()) {
                        Game g = reader.readGame(pgnStr);
                        if (g.getRootNode().getBoard().isConsistent()) {
                            restoredGames.add(g);
                            restoredDbIndices.add(dbIdx);
                        }
                    }
                }
            }

            if (restoredGames.isEmpty()) {
                String pgn = prefs.get("currentGame", "");
                if (!pgn.isEmpty()) {
                    Game g = reader.readGame(pgn);
                    if (g.getRootNode().getBoard().isConsistent()) {
                        restoredGames.add(g);
                        restoredDbIndices.add(-1);
                    }
                }
            }

            List<GameSession> restoredSessions = new ArrayList<>();
            if (workspace != null) {
                for (Game g : restoredGames) {
                    GameSession sess = workspace.createSession(g);
                    restoredSessions.add(sess);
                }
                int activeIndex = prefs.getInt("ACTIVE_SESSION_INDEX", 0);
                if (activeIndex >= 0 && activeIndex < restoredSessions.size()) {
                    workspace.setActiveSession(restoredSessions.get(activeIndex));
                    setGameSession(restoredSessions.get(activeIndex));
                } else if (!restoredSessions.isEmpty()) {
                    workspace.setActiveSession(restoredSessions.get(0));
                    setGameSession(restoredSessions.get(0));
                }
            } else if (!restoredGames.isEmpty()) {
                setGame(restoredGames.get(0));
                restoredGames.get(0).setTreeWasChanged(true);
            }

            // Restore database
            String lastDbPath = prefs.get("LAST_DATABASE_PATH", "");
            String lastDbModified = prefs.get("LAST_DATABASE_MODIFIED", "");
            databaseModifiedWarning = false;

            if (!lastDbPath.isEmpty() && workspace != null) {
                Path dbPath = Path.of(lastDbPath);
                if (!Files.exists(dbPath)) {
                    setActiveDatabase(null);
                    for (GameSession sess : restoredSessions) {
                        sess.setPgnSourceReference(null);
                    }
                } else {
                    String currentModified = "";
                    try {
                        currentModified = Files.getLastModifiedTime(dbPath).toString();
                    } catch (Exception e) {
                        currentModified = "";
                    }

                    if (lastDbModified.isEmpty() || !currentModified.equals(lastDbModified)) {
                        try {
                            ChessDatabase db = workspace.getOrCreateDatabase(dbPath);
                            db.scanGames();
                            setActiveDatabase(db);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        for (GameSession sess : restoredSessions) {
                            sess.setPgnSourceReference(null);
                        }
                        databaseModifiedWarning = true;
                    } else {
                        try {
                            ChessDatabase db = workspace.getOrCreateDatabase(dbPath);
                            db.scanGames();
                            setActiveDatabase(db);
                            long rev = db.getRevision();
                            ArrayList<GameInfo> indexList = db.getIndex();

                            for (int i = 0; i < restoredSessions.size(); i++) {
                                int dbIdx = restoredDbIndices.get(i);
                                GameSession sess = restoredSessions.get(i);
                                if (dbIdx >= 0 && dbIdx < indexList.size()) {
                                    GameInfo info = indexList.get(dbIdx);
                                    sess.setPgnSourceReference(new PgnSourceReference(db.getPath(), info.getId(), rev));
                                } else {
                                    sess.setPgnSourceReference(null);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (workspace != null) {
                setActiveDatabase(null);
                for (GameSession sess : restoredSessions) {
                    sess.setPgnSourceReference(null);
                }
            }

            // screen geometry (i.e. window size + slide positions)
            screenGeometry.height = prefs.getInt("WINDOW_HEIGHT", screenGeometry.height);
            screenGeometry.width = prefs.getInt("WINDOW_WIDTH", screenGeometry.width);
            screenGeometry.posX = prefs.getInt("WINDOW_POSITION_X", screenGeometry.posX);
            screenGeometry.posY = prefs.getInt("WINDOW_POSITION_Y", screenGeometry.posY);
            screenGeometry.isMaximized = prefs.getBoolean("WINDOW_MAXIMIZED", false);
            int divH = prefs.getInt("DIVIDER_HORIZONTAL", screenGeometry.dividerHorizontal);
            int divV = prefs.getInt("DIVIDER_VERTICAL", screenGeometry.dividerVertical);
            screenGeometry.dividerHorizontal = divH > 0 ? divH : 600;
            screenGeometry.dividerVertical = divV > 0 ? divV : 450;


            // look and feel
            lookAndFeel = prefs.get("LOOK_AND_FEEL", THEME_FLATLAF_INTELLIJ);

            // last opened directories
            String lastOpenDir = prefs.get("lastOpenDir", "");
            String lastSaveDir = prefs.get("lastSaveDir", "");
            if (!lastOpenDir.isEmpty()) {
                lastOpenedDirPath = new File(lastOpenDir);
                if (!lastOpenedDirPath.exists()) {
                    lastOpenedDirPath = null;
                }
            }
            if (!lastSaveDir.isEmpty()) {
                lastSaveDirPath = new File(lastSaveDir);
                if (!lastSaveDirPath.exists()) {
                    lastSaveDirPath = null;
                }
            }

            // restore engines
            // don't restore engine with idx 0 (that's stockfish internal)
            for (int i = 1; i < MAX_N_ENGINES; i++) {
                String engineString = prefs.get("ENGINE" + i, "");
                if (!engineString.isEmpty()) {
                    Engine engine;
                    //if (i == 0) {
                    //    // engine 0 is Stockfish internal
                    //    // engine = engines.get(0);
                    //    // engine.restoreFromString(engineString);
                    //} else {
                    engine = new Engine();
                    engine.restoreFromString(engineString);
                    engines.add(engine);
                    //}
                }
            }
            int activeIdx = prefs.getInt("ACTIVE_ENGINE_IDX", 0);
            if(activeIdx < engines.size()) {
                activeEngine = engines.get(activeIdx);
                selectedAnalysisEngine = engines.get(activeIdx);
            } else {
                activeEngine = engines.get(0);
                selectedAnalysisEngine = engines.get(0);
            }

            // style of board and pieces
            BoardStyle boardStyle = new BoardStyle();
            int colorStyle = prefs.getInt("COLOR_STYLE", BoardStyle.STYLE_BLUE);
            int pieceStyle = prefs.getInt("PIECE_STYLE", BoardStyle.PIECE_STYLE_MERIDA);
            boardStyle.setPieceStyle(pieceStyle);
            boardStyle.setColorStyle(colorStyle);
            setBoardStyle(boardStyle);

            // thresholds for game analysis
            int gameAnalysisSecs = prefs.getInt("GAME_ANALYSIS_SECS", 3);
            double gameAnalysisThreshold = prefs.getDouble("GAME_ANALYSIS_THRESHOLD", 0.5);
            setGameAnalysisThinkTimeSecs(gameAnalysisSecs);
            setGameAnalysisThreshold(gameAnalysisThreshold);

            // path of opening book
            extBookPath = prefs.get("EXT_BOOK_PATH_FILE", getExtBookPath());

            fontSizeMoveView = prefs.getInt("FONT_SIZE_MOVES", 12);
            useCustomFontSizeMoveView = prefs.getBoolean("USE_CUSTOM_FONT_SIZE_MOVES", false);

            fontSizeEngineOutput = prefs.getInt("FONT_SIZE_ENGINE", 12);
            useCustomFontSizeEngineOutput = prefs.getBoolean("USE_CUSTOM_FONT_SIZE_ENGINE", false);

            }
    }


    // does not verify if newNrThreads is a valid parameter for
    // active engine. Must be checked prior by the caller
    public void setNrThreads(int newNrThreads) {
        activeEngine.setThreads(newNrThreads);
    }

    public int getFontSizeMoveView() {
        return fontSizeMoveView;
    }

    public void setFontSizeMoveView(int size) {
        fontSizeMoveView = size;
        useCustomFontSizeMoveView = true;
        pcs.firePropertyChange("setFontSizeMoveView", null, null);
    }

    public void resetFontSizeMoveView() {
        useCustomFontSizeMoveView = false;
        pcs.firePropertyChange("resetFontSizeMoveView", null, null);
    }

    public int getFontSizeEngineOutput() {
        return fontSizeEngineOutput;
    }

    public void setFontSizeEngineOutput(int size) {
        useCustomFontSizeEngineOutput = true;
        fontSizeEngineOutput = size;
        pcs.firePropertyChange("setFontSizeEngineOutput", null, null);
    }

    public void resetFontSizeEngineOutput() {
        useCustomFontSizeEngineOutput = false;
        pcs.firePropertyChange("resetFontSizeEngineOutput", null, null);
    }

    public boolean isUseCustomFontSizeMoveView() {
        return useCustomFontSizeMoveView;
    }

    public void setUseCustomFontSizeMoveView(boolean useCustomFontSizeMoveView) {
        this.useCustomFontSizeMoveView = useCustomFontSizeMoveView;
    }

    public boolean isUseCustomFontSizeEngineOutput() {
        return useCustomFontSizeEngineOutput;
    }

    public void setUseCustomFontSizeEngineOutput(boolean useCustomFontSizeEngineOutput) {
        this.useCustomFontSizeEngineOutput = useCustomFontSizeEngineOutput;
    }
}
