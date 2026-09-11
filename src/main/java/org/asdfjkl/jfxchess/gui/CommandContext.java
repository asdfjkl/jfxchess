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

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public final class CommandContext {

    private final Model_JFXChess model;
    private final Workspace workspace;
    private final JTabbedPane gameTabs;
    private final WindowManager windowManager;

    public CommandContext(Model_JFXChess model,
                          Workspace workspace,
                          JTabbedPane gameTabs,
                          WindowManager windowManager) {
        this.model = Objects.requireNonNull(model, "model");
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.gameTabs = gameTabs;
        this.windowManager = windowManager;
    }

    public ActionListener bind(ActionListener command) {
        Objects.requireNonNull(command, "command");
        return event -> {
            activate(resolve(event));
            command.actionPerformed(event);
        };
    }

    public ActionListener bind(GameSession gameSession, ActionListener command) {
        Objects.requireNonNull(gameSession, "gameSession");
        Objects.requireNonNull(command, "command");
        return event -> {
            activate(new WindowContext(gameSession, windowFor(event.getSource())));
            command.actionPerformed(event);
        };
    }

    public void activate(GameSession gameSession) {
        activate(new WindowContext(gameSession, null));
    }

    public WindowContext resolve(ActionEvent event) {
        Objects.requireNonNull(event, "event");
        Object source = event.getSource();
        GameSession gameSession = sessionFor(source);
        if (gameSession == null && gameTabs != null &&
                gameTabs.getSelectedComponent() instanceof GameTabView tabView) {
            gameSession = tabView.getGameSession();
        }
        if (gameSession == null) {
            gameSession = workspace.getActiveSession();
        }
        if (gameSession == null) {
            throw new IllegalStateException("No game session is available for this command");
        }
        return new WindowContext(gameSession, windowFor(source));
    }

    private void activate(WindowContext context) {
        GameSession gameSession = context.getGameSession();
        if (!workspace.getSessions().contains(gameSession)) {
            throw new IllegalArgumentException("session is not in this workspace");
        }
        if (model.getGameSession() != gameSession) {
            model.setGameSession(gameSession);
        }
    }

    private GameSession sessionFor(Object source) {
        if (!(source instanceof Component component)) {
            return null;
        }
        for (Component current = component; current != null; current = current.getParent()) {
            if (current instanceof GameTabView tabView) {
                return tabView.getGameSession();
            }
        }
        Window window = SwingUtilities.getWindowAncestor(component);
        if (windowManager != null) {
            return windowManager.getSession(window);
        }
        return null;
    }

    private Window windowFor(Object source) {
        if (source instanceof Component component) {
            return SwingUtilities.getWindowAncestor(component);
        }
        return null;
    }
}
