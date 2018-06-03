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

#ifndef EDIT_CONTROLLER_H
#define EDIT_CONTROLLER_H

#include <QWidget>
#include "model/game_model.h"

class EditController : public QObject
{
    Q_OBJECT
public:
    explicit EditController(GameModel *gameModel, QWidget *parent);

private:
    GameModel *gameModel;
    QWidget *parentWidget;

signals:

public slots:
    void copyGameToClipBoard();
    void copyPositionToClipBoard();
    void enterPosition();
    void editHeaders();
    void paste();

};

#endif // EDIT_CONTROLLER_H
