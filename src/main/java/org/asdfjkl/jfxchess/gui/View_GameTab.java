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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.asdfjkl.jfxchess.lib.HtmlPrinter;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class View_GameTab extends JPanel implements PropertyChangeListener {

    private final Model_JFXChess model;
    private final GameSession gameSession;
    private final Controller_UI controllerUI;
    private final Controller_Board controllerBoard;
    private final Controller_Engine controllerEngine;

    private final HtmlPrinter htmlPrinter = new HtmlPrinter();

    private final JSplitPane horizontalSplit;
    private final JSplitPane verticalSplit;
    private final JLabel gameHeader;
    private JToggleButton engineSwitch;
    private JButton addLineBtn;
    private JButton removeLineBtn;
    private JButton threadsBtn;
    private JButton enginesBtn;
    private final List<JButton> navigationButtons = new ArrayList<>();
    private View_EngineOutput engineOutput;
    private View_Eval viewEval;
    private final View_Moves viewMoves;
    private final View_Chessboard chessboard;

    private Object currentHighlight;

    public View_GameTab(Model_JFXChess model,
                        GameSession gameSession,
                        Controller_UI controllerUI,
                        Controller_Engine controllerEngine) {

        super(new BorderLayout());
        this.model = model;
        this.gameSession = gameSession;
        this.controllerUI = controllerUI;
        this.controllerBoard = new Controller_Board(model, gameSession);
        this.controllerEngine = controllerEngine;

        chessboard = new View_Chessboard(model, gameSession, controllerUI, controllerBoard);
        gameHeader = createGameHeader();
        viewMoves = new View_Moves(model, gameSession, controllerUI, controllerBoard);

        JPanel rightPanel = createRightPanel();
        horizontalSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                chessboard,
                rightPanel
        );
        horizontalSplit.setResizeWeight(0.7);
        horizontalSplit.setDividerLocation(model.getHorizontalDividerLocation());
        horizontalSplit.setContinuousLayout(true);
        horizontalSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (isCurrentActiveTab() && evt.getNewValue() instanceof Integer loc && loc > 0) {
                if (loc != model.getHorizontalDividerLocation()) {
                    controllerUI.changeDividerHorizontal(loc);
                }
            }
        });

        JPanel bottomPanel = createBottomPanel();
        verticalSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                horizontalSplit,
                bottomPanel
        );
        verticalSplit.setResizeWeight(0.8);
        verticalSplit.setDividerLocation(model.getVerticalDividerLocation());
        verticalSplit.setContinuousLayout(true);
        verticalSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (isCurrentActiveTab() && evt.getNewValue() instanceof Integer loc && loc > 0) {
                if (loc != model.getVerticalDividerLocation()) {
                    controllerUI.changeDividerVertical(loc);
                }
            }
        });

        add(verticalSplit, BorderLayout.CENTER);
        model.addListener(this);
        gameSession.addPropertyChangeListener(this);
        updateGameView();
        updateEngineControls();
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public Controller_Board getControllerBoard() {
        return controllerBoard;
    }

    private JLabel createGameHeader() {
        JLabel header = new JLabel(
                "<html><div style='text-align:center;'>N., N. - N., N.<br>" +
                        "Somewhere, 01.01.1900</div></html>"
        );
        header.setHorizontalAlignment(SwingConstants.CENTER);
        header.setVerticalAlignment(SwingConstants.CENTER);
        return header;
    }

    private JPanel createRightPanel() {
        JButton editGameHeader = new JButton();
        editGameHeader.putClientProperty("JButton.buttonType", "toolBarButton");
        editGameHeader.setIcon(new FlatSVGIcon("icons/edit_game_header_18px.svg"));
        editGameHeader.setToolTipText("Edit Game Data");
        editGameHeader.setFocusable(false);
        editGameHeader.addActionListener(controllerUI.editGameData());

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.add(gameHeader, BorderLayout.CENTER);
        headerPanel.add(editGameHeader, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JScrollPane movesScroll = new JScrollPane(viewMoves);

        View_Book viewBook = new View_Book(
                model, gameSession, controllerBoard);
        JScrollPane bookScroll = new JScrollPane(viewBook);

        viewEval = new View_Eval(model, gameSession, 6.0f);
        model.addListener(viewEval);
        gameSession.addPropertyChangeListener(viewEval);

        JTabbedPane detailTabs = new JTabbedPane();
        detailTabs.addTab("Moves", movesScroll);
        detailTabs.addTab("Book", bookScroll);

        JPanel navigationPanel = createNavigationPanel();
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(detailTabs, BorderLayout.CENTER);
        viewEval.setPreferredSize(new Dimension(
                0,
                (int) (navigationPanel.getPreferredSize().height * 1.5)
        ));
        centerPanel.add(viewEval, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(centerPanel, BorderLayout.CENTER);
        rightPanel.add(navigationPanel, BorderLayout.SOUTH);
        return rightPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnStart = createNavigationButton(
                "icons/fast_rewind.svg",
                "Seek To Beginning",
                controllerBoard.seekToBeginning()
        );
        JButton btnBack = createNavigationButton(
                "icons/arrow_back.svg",
                "Move Back",
                controllerBoard.moveBack()
        );
        JButton btnForward = createNavigationButton(
                "icons/play_arrow.svg",
                "Move Forward",
                controllerBoard.moveForward()
        );
        JButton btnEnd = createNavigationButton(
                "icons/fast_forward.svg",
                "Seek to End",
                controllerBoard.seekToEnd()
        );
        navigationButtons.add(btnStart);
        navigationButtons.add(btnBack);
        navigationButtons.add(btnForward);
        navigationButtons.add(btnEnd);

        navigationPanel.add(btnStart);
        navigationPanel.add(btnBack);
        navigationPanel.add(btnForward);
        navigationPanel.add(btnEnd);
        return navigationPanel;
    }

    private JButton createNavigationButton(String icon,
                                           String toolTipText,
                                           java.awt.event.ActionListener listener) {
        JButton button = new JButton();
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setIcon(new FlatSVGIcon(icon));
        button.setToolTipText(toolTipText);
        button.setFocusable(false);
        button.addActionListener(listener);
        return button;
    }

    private JPanel createBottomPanel() {
        JPanel bottomControlBar = new JPanel(new BorderLayout());
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

        engineSwitch = new JToggleButton("Start Engine");
        engineSwitch.addActionListener(controllerEngine.toggleEngine(gameSession));

        addLineBtn = new JButton("+");
        addLineBtn.addActionListener(controllerEngine.incMultiPV(gameSession));
        removeLineBtn = new JButton("-");
        removeLineBtn.addActionListener(controllerEngine.decMultiPV(gameSession));
        threadsBtn = new JButton("Set # Threads");
        threadsBtn.addActionListener(controllerEngine.changeNrThreads(gameSession));

        engineSwitch.setFocusable(false);
        addLineBtn.setFocusable(false);
        removeLineBtn.setFocusable(false);
        threadsBtn.setFocusable(false);

        leftGroup.add(engineSwitch);
        leftGroup.add(addLineBtn);
        leftGroup.add(removeLineBtn);
        leftGroup.add(threadsBtn);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        enginesBtn = new JButton();
        enginesBtn.setIcon(new FlatSVGIcon("icons/engine_18px.svg"));
        enginesBtn.setToolTipText("Select Engine");
        enginesBtn.setFocusable(false);
        enginesBtn.addActionListener(controllerEngine.editEngines());
        rightGroup.add(enginesBtn);

        bottomControlBar.add(leftGroup, BorderLayout.WEST);
        bottomControlBar.add(rightGroup, BorderLayout.EAST);

        engineOutput = new View_EngineOutput(model, gameSession);
        model.addListener(engineOutput);
        gameSession.addPropertyChangeListener(engineOutput);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(bottomControlBar, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(engineOutput), BorderLayout.CENTER);
        return bottomPanel;
    }

    public void setDividerLocations(int horizontal, int vertical) {
        if (horizontal > 0 && horizontalSplit.getDividerLocation() != horizontal) {
            horizontalSplit.setDividerLocation(horizontal);
        }
        if (vertical > 0 && verticalSplit.getDividerLocation() != vertical) {
            verticalSplit.setDividerLocation(vertical);
        }
    }

    private boolean isCurrentActiveTab() {
        return model.getGameSession() == this.gameSession;
    }

    public int getHorizontalDividerLocation() {
        return horizontalSplit.getDividerLocation();
    }

    public int getVerticalDividerLocation() {
        return verticalSplit.getDividerLocation();
    }

    public void resetLayout() {
        verticalSplit.setResizeWeight(0.8);
        verticalSplit.setDividerLocation(450);
        horizontalSplit.setResizeWeight(0.7);
        horizontalSplit.setDividerLocation(600);
        chessboard.revalidate();
        chessboard.repaint();
    }

    private void updatePgnHeaders() {
        HashMap<String, String> pgnHeaders = gameSession.getGame().getPgnHeaders();
        String newGameInfo = "<html><div style='text-align:center;'>" +
                pgnHeaders.get("White") + " - " +
                pgnHeaders.get("Black") + "<br>" +
                pgnHeaders.get("Site");
        if (!pgnHeaders.get("Date").isEmpty()) {
            newGameInfo += ", " + pgnHeaders.get("Date");
        }
        gameHeader.setText(newGameInfo + "</div></html>");
    }

    private void updateHighlightedMove() {
        int id = gameSession.getGame().getCurrentNode().getId();
        HTMLDocument document = (HTMLDocument) viewMoves.getDocument();
        Element element = document.getElement("n" + id);
        Highlighter highlighter = viewMoves.getHighlighter();

        if (element == null) {
            if (gameSession.getGame().getCurrentNode() == gameSession.getGame().getRootNode()
                    && currentHighlight != null) {
                highlighter.removeHighlight(currentHighlight);
                currentHighlight = null;
            }
            return;
        }

        if (currentHighlight != null) {
            highlighter.removeHighlight(currentHighlight);
        }

        try {
            currentHighlight = highlighter.addHighlight(
                    element.getStartOffset(),
                    element.getEndOffset(),
                    new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY)
            );
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Unable to highlight the current move", exception);
        }
    }

    private void updateGameView() {
        int oldCaretPosition = viewMoves.getCaretPosition();
        viewMoves.setText(htmlPrinter.printGame(gameSession.getGame()));
        try {
            viewMoves.setCaretPosition(oldCaretPosition);
        } catch (IllegalArgumentException exception) {
            viewMoves.setCaretPosition(0);
        }
        updateHighlightedMove();
        updatePgnHeaders();
    }

    private void updateEngineSwitch() {
        switch (gameSession.getMode()) {
            case Model_JFXChess.MODE_ANALYSIS:
            case Model_JFXChess.MODE_PLAY_WHITE:
            case Model_JFXChess.MODE_PLAY_BLACK:
            case Model_JFXChess.MODE_PLAYOUT_POSITION:
            case Model_JFXChess.MODE_GAME_ANALYSIS:
                engineSwitch.setText("Stop Engine");
                engineSwitch.setSelected(true);
                break;
            case Model_JFXChess.MODE_ENTER_MOVES:
                engineSwitch.setText("Start Engine");
                engineSwitch.setSelected(false);
                break;
            default:
                break;
        }
    }

    public void updateEngineControls() {
        updateEngineSwitch();
        boolean isThinking = gameSession.isBlockGui();
        int mode = gameSession.getMode();
        boolean isPlayMode = (mode == Model_JFXChess.MODE_PLAY_WHITE
                || mode == Model_JFXChess.MODE_PLAY_BLACK
                || mode == Model_JFXChess.MODE_PLAYOUT_POSITION
                || mode == Model_JFXChess.MODE_GAME_ANALYSIS);

        for (JButton btn : navigationButtons) {
            btn.setEnabled(!isThinking);
        }

        if (isThinking) {
            if (addLineBtn != null) {
                addLineBtn.setEnabled(false);
            }
            if (removeLineBtn != null) {
                removeLineBtn.setEnabled(false);
            }
            if (threadsBtn != null) {
                threadsBtn.setEnabled(false);
            }
            if (enginesBtn != null) {
                enginesBtn.setEnabled(false);
            }
        } else if (isPlayMode) {
            if (addLineBtn != null) {
                addLineBtn.setEnabled(false);
            }
            if (removeLineBtn != null) {
                removeLineBtn.setEnabled(false);
            }
            if (threadsBtn != null) {
                threadsBtn.setEnabled(false);
            }
            if (enginesBtn != null) {
                enginesBtn.setEnabled(false);
            }
        } else {
            if (addLineBtn != null) {
                addLineBtn.setEnabled(true);
            }
            if (removeLineBtn != null) {
                removeLineBtn.setEnabled(true);
            }
            if (threadsBtn != null) {
                threadsBtn.setEnabled(true);
            }
            if (enginesBtn != null) {
                enginesBtn.setEnabled(true);
            }
        }
    }

    public void refreshActiveTab() {
        updateGameView();
        updateEngineControls();
        if (engineOutput != null) {
            engineOutput.refresh();
        }
        if (viewEval != null) {
            viewEval.repaint();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if ("dividerHorizontal".equals(propertyName)) {
            if (event.getNewValue() instanceof Integer loc && loc > 0) {
                if (horizontalSplit.getDividerLocation() != loc) {
                    horizontalSplit.setDividerLocation(loc);
                }
            }
            return;
        }
        if ("dividerVertical".equals(propertyName)) {
            if (event.getNewValue() instanceof Integer loc && loc > 0) {
                if (verticalSplit.getDividerLocation() != loc) {
                    verticalSplit.setDividerLocation(loc);
                }
            }
            return;
        }

        if (event.getSource() == model && model.getGameSession() != gameSession) {
            return;
        }
        if ("pgnHeadersChanged".equals(propertyName)) {
            updatePgnHeaders();
        }
        if ("currentGameNodeChanged".equals(propertyName)) {
            updateHighlightedMove();
        }
        if ("gameChanged".equals(propertyName) || "treeChanged".equals(propertyName)) {
            updateGameView();
        }
        if ("blockGUI".equals(propertyName) || "modeChanged".equals(propertyName) || "activeEngineChanged".equals(propertyName)) {
            updateEngineControls();
        }
    }
}
