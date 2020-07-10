package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Game;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;

public class GameModel {

    public static final int MODE_ENTER_MOVES = 0;
    public static final int MODE_ANALYSIS = 1;
    public static final int MODE_PLAY_WHITE = 2;
    public static final int MODE_PLAY_BLACK = 3;
    Game game;
    private ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private int currentMode;
    private int computerThinkTimeMs = 3000;
    private int multiPv = 1;
    private boolean flipBoard = false;
    private boolean humanPlayerColor = CONSTANTS.WHITE;

    public GameModel() {
        this.game = new Game();
        Board b = new Board(true);
        this.game.getRootNode().setBoard(b);
        this.currentMode = MODE_ENTER_MOVES;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public void setComputerThinkTimeMs(int ms) {
        computerThinkTimeMs = ms;
    }

    public int getComputerThinkTimeMs() {
        return computerThinkTimeMs;
    }

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
        System.out.println("state change");

        for (StateChangeListener sl : stateChangeListeners)
            sl.stateChange();
    }


}



