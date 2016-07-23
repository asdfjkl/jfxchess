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


#ifndef PGN_PRINTER_H
#define PGN_PRINTER_H

#include "game.h"

namespace chess {

class PgnPrinter
{

public:

    /**
     * @brief PgnPrinter prints a game to PGN format
     */
    PgnPrinter();

    /**
     * @brief printGame prints the supplied game to PGN format and saves the
     *                         generated lines a string list
     * @param g game to print
     * @return string list of lines of the generated PGN
     */
    QStringList* printGame(Game *g);

    /**
     * @brief writeGame prints the supplied game to PGN format and saves
     *                  the game as filename on disk. Throws
     *                  std::invalid_argument if impossible to save
     *                  game with the supplied filename
     * @param g game to print/save
     * @param filename filename to save to
     */
    void writeGame(Game *g, const QString &filename);

private:

    int variationDepth;
    bool forceMoveNumber;
    QStringList *pgn;
    QString currentLine;
    void reset();
    void flushCurrentLine();
    void writeToken(const QString &token);
    void writeLine(const QString &token);
    void printGameContent(GameNode *g);
    void printMove(Board *board, Move *m);
    void printComment(const QString &comment);
    void printNag(int nag);
    void printHeaders(QStringList *pgn, Game *g);
    void printResult(int result);
    void beginVariation();
    void endVariation();
    void flushLine();

};

}

#endif // PGN_PRINTER_H
