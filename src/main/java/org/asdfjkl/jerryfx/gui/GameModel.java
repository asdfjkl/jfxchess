package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.Polyglot;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class GameModel {

    public static final int MODE_ENTER_MOVES = 0;
    public static final int MODE_ANALYSIS = 1;
    public static final int MODE_PLAY_WHITE = 2;
    public static final int MODE_PLAY_BLACK = 3;
    public static final int MODE_GAME_ANALYSIS = 4;
    public static final int BOTH_PLAYERS = 5;
    Game game;
    private ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private int currentMode;
    private int multiPv = 1;
    private boolean flipBoard = false;
    private boolean humanPlayerColor = CONSTANTS.WHITE;

    public boolean wasSaved = false;
    private int engineStrength = 20;
    private int engineThinkTimeSecs = 3;

    ArrayList<Engine> engines = new ArrayList<>();
    Engine activeEngine = null;

    private int gameAnalysisForPlayer = BOTH_PLAYERS;
    private int gameAnalysisThreshold = 500; // centipawns
    private int gameAnalysisThinkTime = 3000;

    private boolean gameAnalysisJustStarted = false;

    public String currentBestPv = "";
    public int currentBestEval = 0;
    public int currentMateInMoves = -1;
    public boolean currentIsMate = false;

    public String childBestPv = "";
    public int childBestEval = 0;
    public int childMateInMoves = -1;
    public boolean childIsMate = false;

    public Polyglot book;

    public GameModel() {
        this.game = new Game();
        Board b = new Board(true);
        this.game.getRootNode().setBoard(b);
        this.currentMode = MODE_ENTER_MOVES;

        Engine stockfish = new Engine();
        stockfish.setName("Stockfish (Internal)");
        stockfish.setPath("C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe");
        stockfish.setInternal(true);
        engines.add(stockfish);
        activeEngine = stockfish;

        book = new Polyglot();
        File file = null;
        URL urlBook = getClass().getClassLoader().getResource("book/varied.bin");
        if(urlBook != null) {
            file = new File(urlBook.getFile());
            book.loadBook(file);
        }
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getGameAnalysisForPlayer() { return gameAnalysisForPlayer; }

    public void setGameAnalysisForPlayer(int player) { gameAnalysisForPlayer = player; }

    public int getGameAnalysisThreshold() { return gameAnalysisThreshold; }

    public void setGameAnalysisThreshold(int threshold) { gameAnalysisThreshold = threshold; }

    public void setGameAnalysisThinkTime(int thinktime) { gameAnalysisThinkTime = thinktime; }

    public int getGameAnalysisThinkTime() { return gameAnalysisThinkTime; }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public void setComputerThinkTimeSecs(int secs) {
        engineThinkTimeSecs = secs;
    }

    public int getComputerThinkTimeSecs() {
        return engineThinkTimeSecs;
    }

    public int getEngineStrength() { return engineStrength; }

    public void setEngineStrength(int strength) { engineStrength = strength; }

    public int getMode() {
        return currentMode;
    }

    public int getMultiPv() {
        return multiPv;
    }

    public void setFlipBoard(boolean flipBoard) {
        this.flipBoard = flipBoard;
    }

    public boolean getFlipBoard() {
        return flipBoard;
    }

    public void setHumanPlayerColor(boolean humanPlayerColor) {
        this.humanPlayerColor = humanPlayerColor;
    }

    public boolean getHumanPlayerColor() {
        return humanPlayerColor;
    }

    public boolean getGameAnalysisJustStarted() { return gameAnalysisJustStarted; }

    public void setGameAnalysisJustStarted(boolean val) { gameAnalysisJustStarted = val; }

    public void setMultiPv(int multiPv) {
        if(multiPv < 1) {
            throw new IllegalArgumentException("setMultiPV: "+multiPv+ " but must be >= 1!");
        }
        if(multiPv > 4) {
            throw new IllegalArgumentException("setMultiPV: "+multiPv+ " but must be <= 4!");
        }
        this.multiPv = multiPv;
    }


    public void addListener(StateChangeListener toAdd) {
        stateChangeListeners.add(toAdd);
    }

    public void triggerStateChange() {
        //System.out.println("state change");

        for (StateChangeListener sl : stateChangeListeners)
            sl.stateChange();
    }


}



