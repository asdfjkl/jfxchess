/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

package org.asdfjkl.jerryfx.gui;

public class PgnSTR {

    private long offset = 0;
    private long index = 0;
    private String event = "";
    private String site = "";
    private String date = "";
    private String round = "";
    private String white = "";
    private String black = "";
    private String result = "";
    private String eco = "";

    private boolean foundAtLeast1Tag = false;

    public long getOffset() { return offset; };
    public void setOffset(long offset) { this.offset = offset; }

    public long getIndex() { return index; }
    public void setIndex(long index) { this.index = index; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRound() { return round; }
    public void setRound(String round ) { this.round = round; }

    public String getWhite() { return white; }
    public void setWhite(String white) { this.white = white; }

    public String getBlack() { return black; }
    public void setBlack(String black) { this.black = black; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getEco() { return eco; }
    public void setEco(String eco) { this.eco = eco; }

    public void markValid() {
        foundAtLeast1Tag = true;
    }

    public boolean isValid() {
        return foundAtLeast1Tag;
    }

}
