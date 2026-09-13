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

public class TabIconButton extends JButton {

    public enum Type {
        CLOSE,
        PLUS,
        SCROLL_LEFT,
        SCROLL_RIGHT
    }

    private final Type type;
    private final int baseSize;

    public TabIconButton(Type type, String toolTipText) {
        this(type, toolTipText, type == Type.CLOSE ? 18 : 22);
    }

    public TabIconButton(Type type, String toolTipText, int baseSize) {
        this.type = type;
        this.baseSize = baseSize;
        setToolTipText(toolTipText);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        updatePreferredSize();
    }

    private void updatePreferredSize() {
        double scale = HighDPIHelper.getUIScaleFactor();
        int size = (int) Math.round(baseSize * scale);
        Dimension dim = new Dimension(size, size);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updatePreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();
        ButtonModel buttonModel = getModel();

        double scale = HighDPIHelper.getUIScaleFactor();
        int cornerArc = (int) Math.round(4 * scale);

        if (buttonModel.isPressed()) {
            g2.setColor(getPressedBackground());
            g2.fillRoundRect(1, 1, w - 2, h - 2, cornerArc, cornerArc);
        } else if (buttonModel.isRollover() && isEnabled()) {
            g2.setColor(getHoverBackground());
            g2.fillRoundRect(1, 1, w - 2, h - 2, cornerArc, cornerArc);
        }

        Color symbolColor;
        if (!isEnabled()) {
            Color fg = getNormalForeground();
            symbolColor = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 70);
        } else if (buttonModel.isRollover()) {
            symbolColor = getHoverForeground();
        } else {
            symbolColor = getNormalForeground();
        }
        g2.setColor(symbolColor);

        float strokeWidth = (float) Math.max(1.3f, 1.4f * scale);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        paintSymbol(g2, w, h, scale);

        g2.dispose();
    }

    private void paintSymbol(Graphics2D g2, int w, int h, double scale) {
        switch (type) {
            case CLOSE -> {
                if(w >= h) {
                    int fillUp = (int) ((w-h) / 2.0);
                    int pad = (int) Math.round(5 * scale);
                    int x1 = pad + fillUp;
                    int y1 = pad;
                    int x2 = w - pad - 1 - fillUp;
                    int y2 = h - pad - 1;
                    g2.drawLine(x1, y1, x2, y2);
                    g2.drawLine(x1, y2, x2, y1);
                } else {
                    int fillUp = (int) ((h-w) / 2.0);
                    int pad = (int) Math.round(5 * scale);
                    int x1 = pad;
                    int y1 = fillUp + pad;
                    int x2 = w - pad - 1;
                    int y2 = h -  1 - fillUp - pad;
                    g2.drawLine(x1, y1, x2, y2);
                    g2.drawLine(x1, y2, x2, y1);
                }
            }
            case PLUS -> {
                if(w >= h) {
                    int fillUp = (int) ((w-h) / 2.0);
                    int pad = (int) Math.round(5 * scale);
                    int cx = w / 2;
                    int cy = h / 2;
                    g2.drawLine(pad + fillUp, cy, w - pad - 1 - fillUp, cy);
                    g2.drawLine(cx, pad + fillUp, cx, h - pad - 1 - fillUp);
                } else {
                    int fillUp = (int) ((h-w) / 2.0);
                    int pad = (int) Math.round(5 * scale);
                    int cx = w / 2;
                    int cy = h / 2;
                    g2.drawLine(pad + fillUp, cy, w - pad - fillUp, cy);
                    g2.drawLine(cx, pad + fillUp, cx, h - pad - fillUp);
                }
            }
            case SCROLL_LEFT -> {
                if(w >= h) {
                    int fillUp = (int) ((w-h) / 2.0);
                    int padX = (int) Math.round(6 * scale);
                    int padY = (int) Math.round(5 * scale);
                    int tipX = padX + fillUp;
                    int tipY = h / 2;
                    int rightX = w - padX - 1 - fillUp;
                    int topY = padY;
                    int bottomY = h - padY - 1;
                    g2.drawLine(rightX, topY, tipX, tipY);
                    g2.drawLine(tipX, tipY, rightX, bottomY);
                }
                else {
                    int fillUp = (int) ((h-w) / 2.0);
                    int padX = (int) Math.round(6 * scale);
                    int padY = (int) Math.round(5 * scale);
                    int tipX = padX;
                    int tipY = h / 2;
                    int rightX = w - padX - 1;
                    int topY = padY + fillUp;
                    int bottomY = h - padY - 1 - fillUp;
                    g2.drawLine(rightX, topY, tipX, tipY);
                    g2.drawLine(tipX, tipY, rightX, bottomY);
                }
            }
            case SCROLL_RIGHT -> {
                if(w >= h) {
                    int fillUp = (int) ((h-w) / 2.0);
                    int padX = (int) Math.round(6 * scale);
                    int padY = (int) Math.round(5 * scale);
                    int tipX = w - padX - 1 - fillUp;
                    int tipY = h / 2;
                    int leftX = padX + fillUp;
                    int topY = padY;
                    int bottomY = h - padY - 1;
                    g2.drawLine(leftX, topY, tipX, tipY);
                    g2.drawLine(tipX, tipY, leftX, bottomY);
                } else {
                    int fillUp = (int) ((h-w) / 2.0);
                    int padX = (int) Math.round(6 * scale);
                    int padY = (int) Math.round(5 * scale);
                    int tipX = w - padX - 1;
                    int tipY = h / 2;
                    int leftX = padX;
                    int topY = padY + fillUp;
                    int bottomY = h - padY - 1 - fillUp;
                    g2.drawLine(leftX, topY, tipX, tipY);
                    g2.drawLine(tipX, tipY, leftX, bottomY);
                }
            }
        }
    }

    private Color getHoverBackground() {
        Color c = UIManager.getColor("Button.toolbar.hoverBackground");
        if (c == null) {
            c = UIManager.getColor("TabbedPane.buttonHoverBackground");
        }
        if (c == null) {
            c = UIManager.getColor("TabbedPane.closeHoverBackground");
        }
        if (c == null) {
            Color fg = getNormalForeground();
            c = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 35);
        }
        return c;
    }

    private Color getPressedBackground() {
        Color c = UIManager.getColor("Button.toolbar.pressedBackground");
        if (c == null) {
            c = UIManager.getColor("TabbedPane.buttonPressedBackground");
        }
        if (c == null) {
            c = UIManager.getColor("TabbedPane.closePressedBackground");
        }
        if (c == null) {
            Color fg = getNormalForeground();
            c = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 65);
        }
        return c;
    }

    private Color getNormalForeground() {
        Color c = UIManager.getColor("TabbedPane.foreground");
        if (c == null) {
            c = UIManager.getColor("Label.foreground");
        }
        if (c == null) {
            c = Color.DARK_GRAY;
        }
        if (type == Type.CLOSE) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 180);
        }
        return c;
    }

    private Color getHoverForeground() {
        Color c = UIManager.getColor("TabbedPane.closeHoverForeground");
        if (c == null) {
            c = UIManager.getColor("TabbedPane.foreground");
        }
        if (c == null) {
            c = UIManager.getColor("Label.foreground");
        }
        if (c == null) {
            c = Color.BLACK;
        }
        return c;
    }
}
