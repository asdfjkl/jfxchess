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

import org.asdfjkl.jfxchess.lib.PolyglotExtEntry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Map;

public class WinDrawLossRenderer extends JPanel implements TableCellRenderer {

    private static final int BAR_V_MARGIN = 2;
    private static final int TEXT_H_PADDING = 4;
    private static final double PERCENT_DIVISOR = 100.0;
    private static final String DESKTOP_HINTS_KEY = "awt.font.desktophints";

    private static final Color COLOR_WIN = new Color(197, 197, 197);
    private static final Color COLOR_DRAW = new Color(107, 107, 107);
    private static final Color COLOR_LOSS = new Color(58, 58, 58);

    private int win;
    private int draw;
    private int loss;

    public WinDrawLossRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (table != null && table.getFont() != null) {
            setFont(table.getFont());
        }

        if (value instanceof PolyglotExtEntry entry) {
            this.win = entry.getWins();
            this.draw = entry.getDraws();
            this.loss = entry.getLosses();
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        int barHeight = Math.max(0, height - (2 * BAR_V_MARGIN));

        // Calculate bar widths in pixels
        int winWidth = (int) (width * (win / PERCENT_DIVISOR));
        int drawWidth = (int) (width * (draw / PERCENT_DIVISOR));
        int lossWidth = width - winWidth - drawWidth;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Draw W/D/L bars
            int x = 0;

            g2.setColor(COLOR_WIN);
            g2.fillRect(x, BAR_V_MARGIN, winWidth, barHeight);
            x += winWidth;

            g2.setColor(COLOR_DRAW);
            g2.fillRect(x, BAR_V_MARGIN, drawWidth, barHeight);
            x += drawWidth;

            g2.setColor(COLOR_LOSS);
            g2.fillRect(x, BAR_V_MARGIN, lossWidth, barHeight);

            // Match desktop text antialiasing used by Swing labels and table cells
            applyTextHints(g2);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();

            g2.setColor(Color.BLACK);
            drawCenteredText(g2, win + "%", 0, winWidth, height, fm);

            g2.setColor(Color.LIGHT_GRAY);
            drawCenteredText(g2, draw + "%", winWidth, drawWidth, height, fm);
            drawCenteredText(g2, loss + "%", winWidth + drawWidth, lossWidth, height, fm);
        } finally {
            g2.dispose();
        }
    }

    // Apply OS font smoothing hints (e.g., LCD/grayscale AA) with AA_ON fallback.
    private void applyTextHints(Graphics2D g2) {
        Object desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty(DESKTOP_HINTS_KEY);
        if (desktopHints instanceof Map<?, ?> hints && !hints.isEmpty()) {
            g2.addRenderingHints(hints);
        }

        Object textAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        boolean aaDisabled = textAa == null
                || textAa == RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
                || textAa == RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        if (aaDisabled) {
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
        }
    }

    // Draw percentage centered in its bar segment; skip when it would overflow adjacent bars.
    private void drawCenteredText(Graphics2D g2, String text, int startX, int width, int height, FontMetrics fm) {
        if (width <= 0) {
            return;
        }

        int textWidth = fm.stringWidth(text);
        if (textWidth + TEXT_H_PADDING > width) {
            return;
        }

        int x = startX + (width - textWidth) / 2;
        int y = (height + fm.getAscent() - fm.getDescent()) / 2;

        g2.drawString(text, x, y);
    }
}