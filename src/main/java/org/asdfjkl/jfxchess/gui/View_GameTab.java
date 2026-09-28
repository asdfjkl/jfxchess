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
import java.util.HashMap;

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
        updateEngineSwitch();
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

        View_Eval viewEval = new View_Eval(model, 6.0f);
        model.addListener(viewEval);

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
        navigationPanel.add(createNavigationButton(
                "icons/fast_rewind.svg",
                "Seek To Beginning",
                controllerBoard.seekToBeginning()
        ));
        navigationPanel.add(createNavigationButton(
                "icons/arrow_back.svg",
                "Move Back",
                controllerBoard.moveBack()
        ));
        navigationPanel.add(createNavigationButton(
                "icons/play_arrow.svg",
                "Move Forward",
                controllerBoard.moveForward()
        ));
        navigationPanel.add(createNavigationButton(
                "icons/fast_forward.svg",
                "Seek to End",
                controllerBoard.seekToEnd()
        ));
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
        engineSwitch.addActionListener(e -> {
            if (!engineSwitch.isSelected()) {
                controllerEngine.activateEnterMovesMode();
            } else {
                controllerEngine.activateAnalysisMode();
            }
        });

        JButton addLine = new JButton("+");
        addLine.addActionListener(controllerEngine.incMultiPV());
        JButton removeLine = new JButton("-");
        removeLine.addActionListener(controllerEngine.decMultiPV());
        JButton threads = new JButton("Set # Threads");
        threads.addActionListener(controllerEngine.changeNrThreads());

        engineSwitch.setFocusable(false);
        addLine.setFocusable(false);
        removeLine.setFocusable(false);
        threads.setFocusable(false);

        leftGroup.add(engineSwitch);
        leftGroup.add(addLine);
        leftGroup.add(removeLine);
        leftGroup.add(threads);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton engines = new JButton();
        engines.setIcon(new FlatSVGIcon("icons/engine_18px.svg"));
        engines.setToolTipText("Select Engine");
        engines.setFocusable(false);
        engines.addActionListener(controllerEngine.editEngines());
        rightGroup.add(engines);

        bottomControlBar.add(leftGroup, BorderLayout.WEST);
        bottomControlBar.add(rightGroup, BorderLayout.EAST);

        View_EngineOutput engineOutput = new View_EngineOutput(model);
        model.addListener(engineOutput);

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
        if ("modeChanged".equals(propertyName)) {
            updateEngineSwitch();
        }
    }
}
