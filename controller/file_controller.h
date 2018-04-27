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

#ifndef FILE_CONTROLLER_H
#define FILE_CONTROLLER_H

#include <QWidget>
#include "model/game_model.h"

class FileController : public QObject
{
    Q_OBJECT
public:
    explicit FileController(GameModel *gameModel, QWidget *parent = 0);

private:
    GameModel *gameModel;
    QWidget *parentWidget;
    void saveGameTo(QString &filename);
    WId board;
    void setupNewGame(std::unique_ptr<chess::Game> g);


signals:
    void newGamePlayBlack();
    void newGamePlayWhite();
    void newGameEnterMoves();

public slots:

    void printGame();
    void printPosition();
    void newGame();
    void openGame();
    void saveGame();
    void saveAsNewGame();
    void toolbarSaveGame();
};

#endif // FILE_CONTROLLER_H
