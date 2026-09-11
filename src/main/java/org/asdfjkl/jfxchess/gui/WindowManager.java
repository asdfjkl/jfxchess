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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public class WindowManager {

    private final Model_JFXChess model;
    private final Workspace workspace;
    private final JFrame mainFrame;
    private final JTabbedPane gameTabs;
    private final Map<GameSession, DetachedWindow> detachedWindows =
            new IdentityHashMap<>();

    public WindowManager(Model_JFXChess model,
                         Workspace workspace,
                         JFrame mainFrame,
                         JTabbedPane gameTabs) {
        this.model = Objects.requireNonNull(model, "model");
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.mainFrame = Objects.requireNonNull(mainFrame, "mainFrame");
        this.gameTabs = Objects.requireNonNull(gameTabs, "gameTabs");
    }

    public boolean isDetached(GameSession session) {
        return detachedWindows.containsKey(session);
    }

    public GameSession getSession(Window window) {
        if (window == null) {
            return null;
        }
        for (Map.Entry<GameSession, DetachedWindow> entry : detachedWindows.entrySet()) {
            if (entry.getValue().frame == window) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void detach(GameSession session, GameTabView tabView) {
        requireWorkspaceSession(session);
        Objects.requireNonNull(tabView, "tabView");
        if (tabView.getGameSession() != session) {
            throw new IllegalArgumentException("tab view belongs to a different session");
        }

        DetachedWindow existingWindow = detachedWindows.get(session);
        if (existingWindow != null) {
            activateDetachedWindow(session, existingWindow.frame);
            return;
        }

        int tabIndex = gameTabs.indexOfComponent(tabView);
        if (tabIndex < 0) {
            throw new IllegalArgumentException("tab view is not attached to the main window");
        }

        String tabTitle = gameTabs.getTitleAt(tabIndex);
        Icon tabIcon = gameTabs.getIconAt(tabIndex);
        String toolTipText = gameTabs.getToolTipTextAt(tabIndex);
        JFrame detachedFrame = new JFrame("JFXChess - " + tabTitle);
        detachedFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        detachedFrame.setIconImages(mainFrame.getIconImages());

        DetachedWindow detachedWindow = new DetachedWindow(
                detachedFrame,
                tabView,
                tabIndex,
                tabTitle,
                tabIcon,
                toolTipText
        );
        detachedWindows.put(session, detachedWindow);
        detachedFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent event) {
                activateSession(session);
            }

            @Override
            public void windowGainedFocus(WindowEvent event) {
                activateSession(session);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                attach(session);
            }
        });

        JPanel detachedContent = new JPanel(new BorderLayout());
        gameTabs.remove(tabView);
        detachedContent.add(tabView, BorderLayout.CENTER);
        detachedFrame.setContentPane(detachedContent);
        detachedFrame.setSize(mainFrame.getSize());
        detachedFrame.setLocationByPlatform(true);
        detachedFrame.setVisible(true);
        activateSession(session);
    }

    public void attach(GameSession session) {
        DetachedWindow detachedWindow = detachedWindows.remove(session);
        if (detachedWindow == null) {
            return;
        }

        removeFromDetachedFrame(detachedWindow);
        int tabIndex = Math.min(detachedWindow.tabIndex, gameTabs.getTabCount());
        gameTabs.insertTab(
                detachedWindow.tabTitle,
                detachedWindow.tabIcon,
                detachedWindow.tabView,
                detachedWindow.toolTipText,
                tabIndex
        );
        detachedWindow.frame.dispose();
        gameTabs.setSelectedComponent(detachedWindow.tabView);
        activateSession(session);
    }

    public void disposeDetachedSession(GameSession session) {
        DetachedWindow detachedWindow = detachedWindows.remove(session);
        if (detachedWindow == null) {
            return;
        }
        removeFromDetachedFrame(detachedWindow);
        detachedWindow.frame.dispose();
    }

    public void activateDetachedWindow(GameSession session) {
        DetachedWindow detachedWindow = detachedWindows.get(session);
        if (detachedWindow != null) {
            activateDetachedWindow(session, detachedWindow.frame);
        }
    }

    public void updateLookAndFeel() {
        for (DetachedWindow detachedWindow : detachedWindows.values()) {
            SwingUtilities.updateComponentTreeUI(detachedWindow.frame);
            detachedWindow.frame.invalidate();
            detachedWindow.frame.validate();
            detachedWindow.frame.repaint();
        }
    }

    private void activateDetachedWindow(GameSession session, JFrame detachedFrame) {
        activateSession(session);
        detachedFrame.toFront();
        detachedFrame.requestFocus();
    }

    private void activateSession(GameSession session) {
        if (model.getGameSession() != session) {
            model.setGameSession(session);
        }
    }

    private void requireWorkspaceSession(GameSession session) {
        if (!workspace.getSessions().contains(session)) {
            throw new IllegalArgumentException("session is not in this workspace");
        }
    }

    private void removeFromDetachedFrame(DetachedWindow detachedWindow) {
        Container parent = detachedWindow.tabView.getParent();
        if (parent != null) {
            parent.remove(detachedWindow.tabView);
        }
    }

    private static class DetachedWindow {

        private final JFrame frame;
        private final GameTabView tabView;
        private final int tabIndex;
        private final String tabTitle;
        private final Icon tabIcon;
        private final String toolTipText;

        private DetachedWindow(JFrame frame,
                               GameTabView tabView,
                               int tabIndex,
                               String tabTitle,
                               Icon tabIcon,
                               String toolTipText) {
            this.frame = frame;
            this.tabView = tabView;
            this.tabIndex = tabIndex;
            this.tabTitle = tabTitle;
            this.tabIcon = tabIcon;
            this.toolTipText = toolTipText;
        }
    }
}
