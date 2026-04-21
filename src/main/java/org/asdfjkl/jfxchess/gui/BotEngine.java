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

import java.awt.image.BufferedImage;
import java.io.IOException;

public class BotEngine extends Engine {

    private String bio = "";
    private BufferedImage image;
    private String elo = "";

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setElo(String s) {
        elo = s;
    }

    public String getElo() {
        return elo;
    }

    @Override
    public int getUciElo() {
        return Integer.parseInt(elo);
    }

    public String getBio() {
        return bio;
    }

    public void loadImage(String resourcePath) {
        try {
            image = ImageLoader.loadImage(resourcePath);
        } catch (IOException e) {
            image = null;
        }
    }

    public BufferedImage getImage() {
        return image;
    }

}