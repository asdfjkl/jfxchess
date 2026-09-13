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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BrowserTabBar extends JPanel {

    private final List<BrowserTabItem> tabItems = new ArrayList<>();
    private final TabIconButton btnPlus;
    private final TabIconButton btnScrollLeft;
    private final TabIconButton btnScrollRight;
    private final JViewport viewport;
    private final JPanel tabStripPanel;
    private final Runnable onPlusClick;

    public BrowserTabBar(Runnable onPlusClick) {
        super(new BorderLayout());
        this.onPlusClick = onPlusClick;

        setOpaque(true);

        btnScrollLeft = new TabIconButton(TabIconButton.Type.SCROLL_LEFT, "Scroll tabs left");
        btnScrollLeft.setVisible(false);
        btnScrollLeft.addActionListener(e -> scrollBy(-getScrollStep()));

        btnScrollRight = new TabIconButton(TabIconButton.Type.SCROLL_RIGHT, "Scroll tabs right");
        btnScrollRight.setVisible(false);
        btnScrollRight.addActionListener(e -> scrollBy(getScrollStep()));

        btnPlus = new TabIconButton(TabIconButton.Type.PLUS, "New Game");
        btnPlus.addActionListener(e -> {
            if (this.onPlusClick != null) {
                this.onPlusClick.run();
            }
        });

        tabStripPanel = new JPanel();
        tabStripPanel.setOpaque(false);
        tabStripPanel.setLayout(new TabStripLayout());
        tabStripPanel.add(btnPlus);

        viewport = new JViewport();
        viewport.setOpaque(false);
        viewport.setView(tabStripPanel);
        viewport.addChangeListener(e -> updateScrollButtonsState());

        viewport.addMouseWheelListener((MouseWheelEvent e) -> {
            int notches = e.getWheelRotation();
            scrollBy(notches * (getScrollStep() / 2));
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                tabStripPanel.revalidate();
                tabStripPanel.repaint();
            }
        });

        add(btnScrollLeft, BorderLayout.WEST);
        add(viewport, BorderLayout.CENTER);
        add(btnScrollRight, BorderLayout.EAST);

        updateColors();
    }

    public void addTab(GameSession session,
                       Consumer<GameSession> onSelect,
                       Consumer<GameSession> onClose) {
        BrowserTabItem tabItem = new BrowserTabItem(session, onSelect, onClose);
        tabItems.add(tabItem);
        // Add tabItem before btnPlus in tabStripPanel
        tabStripPanel.add(tabItem, tabItems.size() - 1);
        tabStripPanel.revalidate();
        tabStripPanel.repaint();
    }

    public void removeTab(GameSession session) {
        BrowserTabItem target = findTabItem(session);
        if (target != null) {
            tabItems.remove(target);
            tabStripPanel.remove(target);
            tabStripPanel.revalidate();
            tabStripPanel.repaint();
        }
    }

    public void selectTab(GameSession session) {
        BrowserTabItem selectedItem = null;
        for (BrowserTabItem item : tabItems) {
            boolean isMatch = item.getSession() == session;
            item.setActive(isMatch);
            if (isMatch) {
                selectedItem = item;
            }
        }
        if (selectedItem != null) {
            scrollToTab(selectedItem);
        }
        repaint();
    }

    public void updateTab(GameSession session) {
        BrowserTabItem item = findTabItem(session);
        if (item != null) {
            item.updateTitleAndTooltip();
            tabStripPanel.revalidate();
            tabStripPanel.repaint();
        }
    }

    private BrowserTabItem findTabItem(GameSession session) {
        for (BrowserTabItem item : tabItems) {
            if (item.getSession() == session) {
                return item;
            }
        }
        return null;
    }

    private void scrollToTab(BrowserTabItem tab) {
        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = tab.getBounds();
            if (bounds.width > 0) {
                tab.scrollRectToVisible(new Rectangle(0, 0, bounds.width, bounds.height));
                updateScrollButtonsState();
            }
        });
    }

    private int getScrollStep() {
        double scale = HighDPIHelper.getUIScaleFactor();
        return (int) Math.round(100 * scale);
    }

    private void scrollBy(int delta) {
        Point p = viewport.getViewPosition();
        int maxScrollX = Math.max(0, tabStripPanel.getWidth() - viewport.getWidth());
        int newX = Math.max(0, Math.min(maxScrollX, p.x + delta));
        viewport.setViewPosition(new Point(newX, p.y));
        updateScrollButtonsState();
    }

    private void updateScrollButtonsState() {
        if (!btnScrollLeft.isVisible() && !btnScrollRight.isVisible()) {
            return;
        }
        Point p = viewport.getViewPosition();
        int maxScrollX = Math.max(0, tabStripPanel.getWidth() - viewport.getWidth());
        btnScrollLeft.setEnabled(p.x > 0);
        btnScrollRight.setEnabled(p.x < maxScrollX);
    }

    private void updateColors() {
        Color bg = UIManager.getColor("TabbedPane.background");
        if (bg == null) {
            bg = UIManager.getColor("Panel.background");
        }
        setBackground(bg);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateColors();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Bottom border line separating tab bar from content
        Graphics2D g2 = (Graphics2D) g.create();
        Color borderColor = UIManager.getColor("Component.borderColor");
        if (borderColor == null) {
            borderColor = UIManager.getColor("TabbedPane.contentAreaColor");
        }
        if (borderColor == null) {
            borderColor = Color.LIGHT_GRAY;
        }
        g2.setColor(borderColor);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        g2.dispose();
    }

    private class TabStripLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {}

        @Override
        public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            double scale = HighDPIHelper.getUIScaleFactor();
            int tabHeight = (int) Math.round(32 * scale);
            int tabWidth = calculateCurrentTabWidth();
            int plusWidth = btnPlus.getPreferredSize().width;
            int totalWidth = tabItems.size() * tabWidth + plusWidth + (int) Math.round(6 * scale);
            return new Dimension(totalWidth, tabHeight);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            double scale = HighDPIHelper.getUIScaleFactor();
            int tabHeight = (int) Math.round(32 * scale);
            int defaultTabWidth = (int) Math.round(190 * scale);
            int minTabWidth = (int) Math.round(88 * scale);
            int plusWidth = btnPlus.getPreferredSize().width;
            int plusHeight = btnPlus.getPreferredSize().height;

            int viewportW = viewport.getWidth();
            int count = tabItems.size();

            // Calculate tab width based on viewport width
            int tabWidth;
            if (count > 0 && viewportW > 0) {
                int availableForTabs = viewportW - plusWidth - (int) Math.round(6 * scale);
                int calculated = availableForTabs / count;
                tabWidth = Math.max(minTabWidth, Math.min(defaultTabWidth, calculated));
            } else {
                tabWidth = defaultTabWidth;
            }

            int totalRequiredWidth = count * tabWidth + plusWidth + (int) Math.round(6 * scale);
            boolean overflow = totalRequiredWidth > viewportW && viewportW > 0;

            if (overflow != btnScrollLeft.isVisible()) {
                btnScrollLeft.setVisible(overflow);
                btnScrollRight.setVisible(overflow);
                if (!overflow) {
                    viewport.setViewPosition(new Point(0, 0));
                }
            }

            int x = 0;
            for (BrowserTabItem tab : tabItems) {
                tab.setBounds(x, 0, tabWidth, tabHeight);
                x += tabWidth;
            }

            int plusY = Math.max(0, (tabHeight - plusHeight) / 2);
            btnPlus.setBounds(x + (int) Math.round(2 * scale), plusY, plusWidth, plusHeight);
            x += plusWidth + (int) Math.round(4 * scale);

            tabStripPanel.setSize(new Dimension(x, tabHeight));
            updateScrollButtonsState();
        }

        private int calculateCurrentTabWidth() {
            double scale = HighDPIHelper.getUIScaleFactor();
            int defaultTabWidth = (int) Math.round(190 * scale);
            int minTabWidth = (int) Math.round(88 * scale);
            int plusWidth = btnPlus.getPreferredSize().width;

            int viewportW = viewport.getWidth();
            int count = tabItems.size();
            if (count > 0 && viewportW > 0) {
                int availableForTabs = viewportW - plusWidth - (int) Math.round(6 * scale);
                int calculated = availableForTabs / count;
                return Math.max(minTabWidth, Math.min(defaultTabWidth, calculated));
            }
            return defaultTabWidth;
        }
    }
}
