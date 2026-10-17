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

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View_EngineOutput extends JEditorPane implements PropertyChangeListener {

    Model_JFXChess model;
    GameSession gameSession;

    String cachedInfo = "";

    public View_EngineOutput(Model_JFXChess model) {
        this(model, model.getGameSession());
    }

    public View_EngineOutput(Model_JFXChess model, GameSession gameSession) {
        this.model = model;
        this.gameSession = gameSession;

        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        setContentType("text/html");
        setEditable(false);
        setFocusable(false);

        if (gameSession != null) {
            gameSession.addPropertyChangeListener(this);
        }

        if (model.isUseCustomFontSizeEngineOutput()) {
            updateFontSize(model.getFontSizeEngineOutput());
        }
    }

    public void updateFontSize(int fontSize) {
        if (fontSize <= 0) {
            return;
        }

        Font uiFont = UIManager.getFont("EditorPane.font");
        if (uiFont == null) {
            uiFont = UIManager.getFont("Label.font");
        }
        if (uiFont == null) {
            uiFont = getFont();
        }
        Font newFont = uiFont.deriveFont((float) fontSize);
        setFont(newFont);

        revalidate();
        repaint();
    }

    public void resetFontSize() {
        Font uiFont = UIManager.getFont("EditorPane.font");
        if (uiFont == null) {
            uiFont = UIManager.getFont("Label.font");
        }
        if (uiFont != null) {
            setFont(uiFont);
        }

        revalidate();
        repaint();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        if (model != null && model.isUseCustomFontSizeEngineOutput()) {
            updateFontSize(model.getFontSizeEngineOutput());
        }
    }

    public void refresh() {
        updateEngineOutputText();
    }

    public void updateEngineOutputText() {
        if (gameSession == null) {
            return;
        }
        String info = gameSession.getCurrentEngineInfo();
        if (info != null && info.length() > 5) {
            String engineName = "";
            Engine activeEngine = gameSession.getActiveEngine();
            if (activeEngine != null) {
                engineName = activeEngine.getName();
                int mode = gameSession.getMode();
                if (mode == Model_JFXChess.MODE_PLAY_WHITE || mode == Model_JFXChess.MODE_PLAY_BLACK) {
                    engineName += " (" + activeEngine.getUciElo() + ")";
                }
            }
            String s = info.substring(5, info.length()).replace("ENGINE_ID", engineName);
            if (!s.equals(cachedInfo)) {
                cachedInfo = s;
                setText(s);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();

        if (prop.equals("engineInfo") || prop.equals("modeChanged") || prop.equals("activeEngineChanged")) {
            updateEngineOutputText();
        }

        if (prop.equals("setFontSizeEngineOutput")) {
            updateFontSize(model.getFontSizeEngineOutput());
        }
        if (prop.equals("resetFontSizeEngineOutput")) {
            resetFontSize();
        }
    }
}

