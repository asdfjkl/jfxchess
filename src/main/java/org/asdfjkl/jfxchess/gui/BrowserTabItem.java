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

    private static final int HGAP_BASE = 4;
    private static final int PAD_V_BASE = 2;
    private static final int PAD_LEFT_BASE = 8;
    private static final int PAD_RIGHT_BASE = 4;
    private static final float UNDERLINE_BASE = 2.5f;
    private static final int MIN_UNDERLINE_PX = 2;
    private static final int DIVIDER_MARGIN_BASE = 5;

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
        super(new BorderLayout(HighDPIHelper.scaleUI(HGAP_BASE), 0));
        this.session = session;
        this.onSelect = onSelect;
        this.onClose = onClose;

        setOpaque(false);
        applyScaledBorder();

        titleLabel = new JLabel();
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(titleLabel, BorderLayout.CENTER);

        closeButton = new TabIconButton(TabIconButton.Type.CLOSE, "Close tab");
        closeButton.addActionListener(e -> onClose.accept(session));

        // Center closeButton vertically so BorderLayout.EAST does not stretch it out of square.
        JPanel closeWrapper = new JPanel(new GridBagLayout());
        closeWrapper.setOpaque(false);
        closeWrapper.add(closeButton);
        add(closeWrapper, BorderLayout.EAST);

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

    private void applyScaledBorder() {
        setBorder(BorderFactory.createEmptyBorder(
                HighDPIHelper.scaleUI(PAD_V_BASE),
                HighDPIHelper.scaleUI(PAD_LEFT_BASE),
                HighDPIHelper.scaleUI(PAD_V_BASE),
                HighDPIHelper.scaleUI(PAD_RIGHT_BASE)
        ));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyScaledBorder();
        updateFontAndColor();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

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
            int barHeight = Math.max(MIN_UNDERLINE_PX, Math.round(HighDPIHelper.scaleUI(UNDERLINE_BASE)));
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
        int margin = HighDPIHelper.scaleUI(DIVIDER_MARGIN_BASE);
        g2.drawLine(w - 1, margin, w - 1, h - margin);

        g2.dispose();
        super.paintComponent(g);
    }
}
