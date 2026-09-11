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

import java.io.File;
import java.util.prefs.Preferences;

public class ApplicationModel {

    public static final String PREFERENCES_NODE = "/org/asdfjkl/jfxchess";

    private final Workspace workspace = new Workspace();
    private final Preferences preferences =
            Preferences.userRoot().node(PREFERENCES_NODE);

    private String lookAndFeel = "com.formdev.flatlaf.FlatIntelliJLaf";
    private BoardStyle defaultBoardStyle = new BoardStyle();
    private File lastOpenedDirPath;
    private File lastSaveDirPath;

    public Workspace getWorkspace() {
        return workspace;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public String getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    public BoardStyle getDefaultBoardStyle() {
        return defaultBoardStyle;
    }

    public void setDefaultBoardStyle(BoardStyle defaultBoardStyle) {
        this.defaultBoardStyle = defaultBoardStyle;
    }

    public File getLastOpenedDirPath() {
        return lastOpenedDirPath;
    }

    public void setLastOpenedDirPath(File lastOpenedDirPath) {
        this.lastOpenedDirPath = lastOpenedDirPath;
    }

    public File getLastSaveDirPath() {
        return lastSaveDirPath;
    }

    public void setLastSaveDirPath(File lastSaveDirPath) {
        this.lastSaveDirPath = lastSaveDirPath;
    }
}
