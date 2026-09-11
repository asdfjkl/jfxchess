/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2026 Dominik Klein
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

import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.Move;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.UUID;

public class GameSession {

    private final UUID id = UUID.randomUUID();
    private final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);

    private Game game;
    private EngineSession engineSession;
    private boolean dirty;
    private boolean stale;
    private PgnSourceReference pgnSourceReference;
    private int mode = Model_JFXChess.MODE_ENTER_MOVES;
    private boolean flipBoard;
    private boolean humanPlayerColor = true;
    private boolean blockGui;
    private int computerThinkTimeSecs = 3;
    private int gameAnalysisForPlayer = Model_JFXChess.BOTH_PLAYERS;
    private double gameAnalysisThreshold = 0.5;
    private int gameAnalysisThinkTimeSecs = 3;
    private String currentEngineInfo = "";

    GameSession(Game game) {
        this.game = Objects.requireNonNull(game, "game");
    }

    public UUID getId() {
        return id;
    }

    public synchronized EngineSession getEngineSession() {
        if (engineSession == null) {
            engineSession = new EngineSession();
        }
        return engineSession;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        Game oldGame = this.game;
        this.game = Objects.requireNonNull(game, "game");
        boolean wasDirty = dirty;
        dirty = false;
        propertyChangeSupport.firePropertyChange("gameChanged", oldGame, game);
        propertyChangeSupport.firePropertyChange("dirtyChanged", wasDirty, false);
    }

    public boolean applyMove(Move move) {
        boolean treeChanged = game.applyMove(move);
        if (treeChanged) {
            markDirty();
            propertyChangeSupport.firePropertyChange("treeChanged", null, null);
        }
        propertyChangeSupport.firePropertyChange("currentGameNodeChanged", null, null);
        return treeChanged;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isStale() {
        return stale;
    }

    public void markStale() {
        if (!stale) {
            stale = true;
            propertyChangeSupport.firePropertyChange("staleChanged", false, true);
        }
    }

    public void markDirty() {
        if (!dirty) {
            dirty = true;
            propertyChangeSupport.firePropertyChange("dirtyChanged", false, true);
        }
    }

    public void markClean() {
        boolean wasDirty = dirty;
        boolean wasStale = stale;
        dirty = false;
        stale = false;
        if (wasDirty) {
            propertyChangeSupport.firePropertyChange("dirtyChanged", true, false);
        }
        if (wasStale) {
            propertyChangeSupport.firePropertyChange("staleChanged", true, false);
        }
    }

    public PgnSourceReference getPgnSourceReference() {
        return pgnSourceReference;
    }

    public void setPgnSourceReference(PgnSourceReference pgnSourceReference) {
        PgnSourceReference oldReference = this.pgnSourceReference;
        this.pgnSourceReference = pgnSourceReference;
        propertyChangeSupport.firePropertyChange(
                "pgnSourceChanged",
                oldReference,
                pgnSourceReference
        );
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void close() {
        if (engineSession != null) {
            engineSession.stop();
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        int oldMode = this.mode;
        this.mode = mode;
        propertyChangeSupport.firePropertyChange("modeChanged", oldMode, mode);
    }

    public boolean getFlipBoard() {
        return flipBoard;
    }

    public void setFlipBoard(boolean flipBoard) {
        boolean oldFlipBoard = this.flipBoard;
        this.flipBoard = flipBoard;
        propertyChangeSupport.firePropertyChange("boardFlipped", oldFlipBoard, flipBoard);
    }

    public boolean getHumanPlayerColor() {
        return humanPlayerColor;
    }

    public void setHumanPlayerColor(boolean humanPlayerColor) {
        this.humanPlayerColor = humanPlayerColor;
    }

    public boolean isBlockGui() {
        return blockGui;
    }

    public void setBlockGui(boolean blockGui) {
        boolean oldBlockGui = this.blockGui;
        this.blockGui = blockGui;
        propertyChangeSupport.firePropertyChange("blockGUI", oldBlockGui, blockGui);
    }

    public int getComputerThinkTimeSecs() {
        return computerThinkTimeSecs;
    }

    public void setComputerThinkTimeSecs(int computerThinkTimeSecs) {
        this.computerThinkTimeSecs = computerThinkTimeSecs;
    }

    public int getGameAnalysisForPlayer() {
        return gameAnalysisForPlayer;
    }

    public void setGameAnalysisForPlayer(int gameAnalysisForPlayer) {
        this.gameAnalysisForPlayer = gameAnalysisForPlayer;
    }

    public double getGameAnalysisThreshold() {
        return gameAnalysisThreshold;
    }

    public void setGameAnalysisThreshold(double gameAnalysisThreshold) {
        this.gameAnalysisThreshold = gameAnalysisThreshold;
    }

    public int getGameAnalysisThinkTimeSecs() {
        return gameAnalysisThinkTimeSecs;
    }

    public void setGameAnalysisThinkTimeSecs(int gameAnalysisThinkTimeSecs) {
        this.gameAnalysisThinkTimeSecs = gameAnalysisThinkTimeSecs;
    }

    public String getCurrentEngineInfo() {
        return currentEngineInfo;
    }

    public void setCurrentEngineInfo(String currentEngineInfo) {
        String oldEngineInfo = this.currentEngineInfo;
        this.currentEngineInfo = currentEngineInfo;
        propertyChangeSupport.firePropertyChange("engineInfo", oldEngineInfo, currentEngineInfo);
    }
}
