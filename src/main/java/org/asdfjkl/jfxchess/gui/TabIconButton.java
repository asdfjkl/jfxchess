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

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

public class TabIconButton extends JButton {

    private static final int BASE_CLOSE_SIZE = 18;
    private static final int BASE_ACTION_SIZE = 22;
    private static final int ICON_CLOSE_SIZE = 14;
    private static final int ICON_ACTION_SIZE = 16;
    private static final int CORNER_ARC_BASE = 4;
    private static final int HIGHLIGHT_INSET = 1;
    private static final int DISABLED_ALPHA = 70;
    private static final int CLOSE_NORMAL_ALPHA = 180;
    private static final int HOVER_FALLBACK_ALPHA = 35;
    private static final int PRESSED_FALLBACK_ALPHA = 65;

    private static final String ICON_CLOSE = "icons/tab_close.svg";
    private static final String ICON_PLUS = "icons/tab_add.svg";
    private static final String ICON_SCROLL_LEFT = "icons/tab_scroll_left.svg";
    private static final String ICON_SCROLL_RIGHT = "icons/tab_scroll_right.svg";

    public enum Type {
        CLOSE,
        PLUS,
        SCROLL_LEFT,
        SCROLL_RIGHT
    }

    private final Type type;
    private final int baseSize;
    private final FlatSVGIcon svgIcon;

    public TabIconButton(Type type, String toolTipText) {
        this(type, toolTipText, type == Type.CLOSE ? BASE_CLOSE_SIZE : BASE_ACTION_SIZE);
    }

    public TabIconButton(Type type, String toolTipText, int baseSize) {
        this.type = type;
        this.baseSize = baseSize;
        this.svgIcon = buildSvgIcon(type);

        setIcon(this.svgIcon);
        setMargin(new Insets(0, 0, 0, 0));
        setHorizontalAlignment(SwingConstants.CENTER);
        setToolTipText(toolTipText);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        updatePreferredSize();
    }

    // FlatSVGIcon scales unscaled icon dimensions internally via FlatLaf UIScale.
    private FlatSVGIcon buildSvgIcon(Type iconType) {
        String resource = switch (iconType) {
            case CLOSE -> ICON_CLOSE;
            case PLUS -> ICON_PLUS;
            case SCROLL_LEFT -> ICON_SCROLL_LEFT;
            case SCROLL_RIGHT -> ICON_SCROLL_RIGHT;
        };
        int iconSize = (iconType == Type.CLOSE) ? ICON_CLOSE_SIZE : ICON_ACTION_SIZE;

        FlatSVGIcon icon = new FlatSVGIcon(resource, iconSize, iconSize);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> resolveSymbolColor()));
        return icon;
    }

    private void updatePreferredSize() {
        if (baseSize <= 0) {
            return;
        }
        int size = HighDPIHelper.scaleUI(baseSize);
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
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            ButtonModel buttonModel = getModel();

            // Paint a centered square highlight even if a parent layout stretches one axis.
            int side = Math.min(w, h);
            int bgX = (w - side) / 2 + HIGHLIGHT_INSET;
            int bgY = (h - side) / 2 + HIGHLIGHT_INSET;
            int bgSide = Math.max(0, side - (2 * HIGHLIGHT_INSET));
            int cornerArc = HighDPIHelper.scaleUI(CORNER_ARC_BASE);

            if (buttonModel.isPressed()) {
                g2.setColor(getPressedBackground());
                g2.fillRoundRect(bgX, bgY, bgSide, bgSide, cornerArc, cornerArc);
            } else if (buttonModel.isRollover() && isEnabled()) {
                g2.setColor(getHoverBackground());
                g2.fillRoundRect(bgX, bgY, bgSide, bgSide, cornerArc, cornerArc);
            }

            if (svgIcon != null) {
                int ix = (w - svgIcon.getIconWidth()) / 2;
                int iy = (h - svgIcon.getIconHeight()) / 2;
                svgIcon.paintIcon(this, g2, ix, iy);
            }
        } finally {
            g2.dispose();
        }
    }

    private Color resolveSymbolColor() {
        if (!isEnabled()) {
            Color fg = getNormalForeground();
            return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), DISABLED_ALPHA);
        }
        if (getModel().isRollover()) {
            return getHoverForeground();
        }
        return getNormalForeground();
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
            c = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), HOVER_FALLBACK_ALPHA);
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
            c = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), PRESSED_FALLBACK_ALPHA);
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
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), CLOSE_NORMAL_ALPHA);
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
