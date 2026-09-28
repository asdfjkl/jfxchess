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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class BrowserTabItem extends JPanel {

    private final GameSession session;
    private final JLabel titleLabel;
    private final TabIconButton closeButton;
    private final Consumer<GameSession> onSelect;
    private final Consumer<GameSession> onClose;

    private boolean active = false;
    private boolean hovered = false;

    public BrowserTabItem(GameSession session,
                          Consumer<GameSession> onSelect,
                          Consumer<GameSession> onClose) {
        super(new BorderLayout(4, 0));
        this.session = session;
        this.onSelect = onSelect;
        this.onClose = onClose;

        setOpaque(false);
        double scale = HighDPIHelper.getUIScaleFactor();
        setBorder(BorderFactory.createEmptyBorder(
                (int) Math.round(2 * scale),
                (int) Math.round(8 * scale),
                (int) Math.round(2 * scale),
                (int) Math.round(4 * scale)
        ));

        titleLabel = new JLabel();
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(titleLabel, BorderLayout.CENTER);

        closeButton = new TabIconButton(TabIconButton.Type.CLOSE, "Close tab");
        closeButton.addActionListener(e -> onClose.accept(session));
        add(closeButton, BorderLayout.EAST);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    onSelect.accept(session);
                }
            }
        };

        addMouseListener(mouseHandler);
        titleLabel.addMouseListener(mouseHandler);

        updateTitleAndTooltip();
    }

    public GameSession getSession() {
        return session;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            updateFontAndColor();
            repaint();
        }
    }

    public void updateTitleAndTooltip() {
        String versus = session.getGame().getVersusTitle();
        titleLabel.setText(versus);

        String white = session.getGame().getHeader("White");
        String black = session.getGame().getHeader("Black");
        String event = session.getGame().getHeader("Event");
        String date = session.getGame().getHeader("Date");

        StringBuilder sb = new StringBuilder("<html><b>");
        sb.append(white.isEmpty() ? "N.N." : white);
        sb.append("</b> vs. <b>");
        sb.append(black.isEmpty() ? "N.N." : black);
        sb.append("</b>");
        if (!event.isEmpty()) {
            sb.append("<br><i>Event: ").append(event).append("</i>");
        }
        if (!date.isEmpty()) {
            sb.append("<br>Date: ").append(date);
        }
        sb.append("</html>");

        String tip = sb.toString();
        setToolTipText(tip);
        titleLabel.setToolTipText(tip);
        updateFontAndColor();
    }

    private void updateFontAndColor() {
        if (titleLabel == null) {
            return;
        }
        Color fg = UIManager.getColor("TabbedPane.foreground");
        if (fg == null) {
            fg = UIManager.getColor("Label.foreground");
        }
        if (fg == null) {
            fg = Color.BLACK;
        }

        if (active) {
            titleLabel.setForeground(fg);
            Font f = titleLabel.getFont();
            if (f != null) {
                titleLabel.setFont(f.deriveFont(Font.BOLD));
            }
        } else {
            Color muted = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 190);
            //titleLabel.setForeground(muted);
            //Font f = titleLabel.getFont();
            //if (f != null) {
            //    titleLabel.setFont(f.deriveFont(Font.PLAIN));
            //}
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateFontAndColor();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        double scale = HighDPIHelper.getUIScaleFactor();

        if (active) {
            Color selBg = UIManager.getColor("TabbedPane.selectedBackground");
            if (selBg == null) {
                selBg = UIManager.getColor("control");
            }
            if (selBg != null) {
                g2.setColor(selBg);
                g2.fillRect(0, 0, w, h);
            }

            Color underline = UIManager.getColor("TabbedPane.underlineColor");
            if (underline == null) {
                underline = UIManager.getColor("Component.focusColor");
            }
            if (underline == null) {
                underline = new Color(50, 120, 220);
            }
            int barHeight = (int) Math.max(2, Math.round(2.5 * scale));
            g2.setColor(underline);
            g2.fillRect(0, h - barHeight, w, barHeight);

        } else if (hovered) {
            Color hoverBg = UIManager.getColor("TabbedPane.hoverColor");
            if (hoverBg == null) {
                hoverBg = UIManager.getColor("Button.toolbar.hoverBackground");
            }
            if (hoverBg == null) {
                Color fg = titleLabel.getForeground();
                hoverBg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 25);
            }
            g2.setColor(hoverBg);
            g2.fillRect(0, 0, w, h);
        }

        // Subtle divider on the right edge
        Color divColor = UIManager.getColor("Component.borderColor");
        if (divColor == null) {
            divColor = UIManager.getColor("Separator.foreground");
        }
        if (divColor == null) {
            Color fg = titleLabel.getForeground();
            divColor = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30);
        }
        g2.setColor(divColor);
        int margin = (int) Math.round(5 * scale);
        g2.drawLine(w - 1, margin, w - 1, h - margin);

        g2.dispose();
        super.paintComponent(g);
    }
}
