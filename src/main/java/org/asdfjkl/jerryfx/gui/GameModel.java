package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.Game;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;

public class GameModel {

    Game game;
    private ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<>();

    public GameModel() {
        this.game = new Game();
        Board b = new Board(true);
        this.game.getRootNode().setBoard(b);
    }

    public Game getGame() {
        return game;
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



