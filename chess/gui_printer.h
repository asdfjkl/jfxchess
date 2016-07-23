/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
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


#ifndef GUI_PRINTER_H
#define GUI_PRINTER_H
#include "game.h"

namespace chess {

class GuiPrinter
{
public:
    /**
     * @brief GuiPrinter handles printing of a game in a formatted
     *                   way in order to display it a QTextBrowser.
     *                   Formatting includes colors and links to nodes
     *                   for navigation (using the integer ids from GameNode
     *                   objects)
     */
    GuiPrinter();

    /**
     * @brief printGame returns a formatted for displaying in QTextBrowser
     *                  of the supplied game
     * @param g pointer to a Game
     * @return text string of game using san notation.
     */
    QString printGame(Game *g);

private:
    bool newLine;
    int variationDepth;
    bool forceMoveNumber;
    QString pgn;
    QString currentLine;
    void reset();
    void flushCurrentLine();
    void writeToken(const QString &token);
    void writeLine(const QString &token);
    void printGameContent(GameNode *g, bool onMainLine);
    void printMove(GameNode *g);
    void printComment(const QString &comment);
    void printNag(int nag);
    void printResult(int result);
    void beginVariation();
    void endVariation();
    void flushLine();
};

}

#endif // GUI_PRINTER_H
