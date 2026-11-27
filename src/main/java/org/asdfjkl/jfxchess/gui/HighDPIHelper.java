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

import com.formdev.flatlaf.util.UIScale;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class HighDPIHelper {

    private static final float DEFAULT_LOGICAL_SCALE = 1.0f;
    private static final float MIN_LOGICAL_SCALE = 0.5f;

    public static double getUIScaleFactor() {
        if (GraphicsEnvironment.isHeadless()) {
            return 1.0;
        }
        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration();

        AffineTransform tx = gc.getDefaultTransform();
        return tx.getScaleX(); // Usually 1.0, 1.25, 1.3, 1.5, etc.
    }

    // Logical Swing UI scale via FlatLaf (e.g., ~1.25 on Linux Xft/DPI=120 where device scale is 1.0).
    // Returns 1.0 when JRE HiDPI already scales Swing coordinates (e.g., Windows 125% or macOS Retina).
    public static float getLogicalScale() {
        try {
            float factor = UIScale.getUserScaleFactor();
            if (Float.isNaN(factor) || Float.isInfinite(factor) || factor < MIN_LOGICAL_SCALE) {
                return DEFAULT_LOGICAL_SCALE;
            }
            return factor;
        } catch (HeadlessException e) {
            return DEFAULT_LOGICAL_SCALE;
        }
    }

    // Scale logical pixel sizes (row heights, column widths, paddings) for Swing layouts.
    public static int scaleUI(int value) {
        return Math.round(value * getLogicalScale());
    }

    // Scale logical float metrics (e.g., stroke widths) for Swing custom painting.
    public static float scaleUI(float value) {
        return value * getLogicalScale();
    }
}
