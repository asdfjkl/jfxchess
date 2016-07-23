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


#ifndef DIALOG_NEXTMOVE_H
#define DIALOG_NEXTMOVE_H

#include <QDialog>
#include <QListWidget>
#include <QPushButton>
#include "chess/game_node.h"

class DialogNextMove : public QDialog
{
    Q_OBJECT
public:
    QListWidget *moveList;
    int selectedIndex;
    explicit DialogNextMove(chess::GameNode *node, QWidget *parent = 0);

protected:
    void keyPressEvent(QKeyEvent *e);

signals:
    void okSelected();
    void cancelSelected();

public slots:
    void onItemChanged();

};

#endif // DIALOG_NEXTMOVE_H
