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

import org.asdfjkl.jfxchess.lib.GameNode;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class View_Eval extends JPanel implements PropertyChangeListener {

    private List<Float> evals = new ArrayList<>(30);
    private final float maxEval; // defines range [-maxEval, +maxEval]
    private final Model_JFXChess model;
    private final GameSession gameSession;
    private final HashMap<Integer, Integer> idPos = new HashMap<>();

    public View_Eval(Model_JFXChess model, float maxEval) {
        this(model, model.getGameSession(), maxEval);
    }

    public View_Eval(Model_JFXChess model, GameSession gameSession, float maxEval) {
        this.maxEval = maxEval;
        this.model = model;
        this.gameSession = gameSession;
        //setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        for (int i = 0; i < 30; i++) {
            evals.add(0f);
        }
    }

    public void setEvalAt(int pos, float val) {
        // Expand list if needed
        while (evals.size() <= pos) {
            evals.add(0f);
        }

        // Clamp value to range
        val = Math.max(-maxEval, Math.min(maxEval, val));

        evals.set(pos, val);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (evals.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();

        int n = evals.size();
        float barWidth = (float) width / n;

        int midY = height / 2;

        // Draw center line (0 eval)
        g2.setColor(Color.GRAY);
        g2.drawLine(0, midY, width, midY);

        for (int i = 0; i < n; i++) {
            float val = evals.get(i);

            // Normalize value to [-1, 1]
            float norm = val / maxEval;

            int barHeight = (int) (norm * (height / 2));

            int x = (int) (i * barWidth);

            if (val >= 0) {
                g2.setColor(Color.GRAY);
                g2.fillRect(x, midY - barHeight, (int) Math.ceil(barWidth), barHeight);
            } else {
                g2.setColor(Color.GRAY);
                g2.fillRect(x, midY, (int) Math.ceil(barWidth), -barHeight);
            }
        }
    }

    public float extractFloat(String text) {
        int start = text.indexOf('(');
        int end = text.indexOf(')', start + 1);

        if (start == -1 || end == -1 || start >= end) {
            return 0.0f;
        }

        String inside = text.substring(start + 1, end);

        try {
            return Float.parseFloat(inside);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    public void updateHashmap() {
        if (gameSession == null) {
            return;
        }
        GameNode node = gameSession.getGame().getRootNode();
        idPos.put(node.getId(), 0);
        int pos = 1;
        while (node.hasChild()) {
            node = node.getVariation(0);
            idPos.put(node.getId(), pos);
            pos++;
        }
    }

    public void resetEvals() {
        if (gameSession == null) {
            return;
        }
        int nGame = gameSession.getGame().countHalfmoves();
        ArrayList<Float> newEval = new ArrayList<Float>();
        int count = Math.max(30, nGame);
        for (int i = 0; i < count; i++) {
            newEval.add(0.0f);
        }
        evals = newEval;
    }

    public void adjustEvalLength() {
        if (gameSession == null) {
            return;
        }
        int nGame = gameSession.getGame().countHalfmoves();
        ArrayList<Float> newEval = new ArrayList<Float>();
        int count = Math.max(30, nGame);
        for (int i = 0; i < count; i++) {
            if (i < evals.size()) {
                newEval.add(evals.get(i));
            } else {
                newEval.add(0.0f);
            }
        }
        evals = newEval;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if ("gameChanged".equals(prop)) {
            resetEvals();
            updateHashmap();
            repaint();
        }
        if ("treeChanged".equals(prop)) {
            adjustEvalLength();
            updateHashmap();
            repaint();
        }
        if ("engineInfo".equals(prop)) {
            if (gameSession == null) {
                return;
            }
            float evalValue = extractFloat(gameSession.getCurrentEngineInfo());
            int id = gameSession.getGame().getCurrentNode().getId();
            int mode = gameSession.getMode();
            if (mode == Model_JFXChess.MODE_ANALYSIS ||
                    mode == Model_JFXChess.MODE_GAME_ANALYSIS ||
                    mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                if (idPos.get(id) != null) {
                    int pos = idPos.get(id);
                    if (pos < evals.size() && pos > 0) {
                        evals.set(pos - 1, evalValue);
                        repaint();
                    }
                }
            }
        }
    }


}