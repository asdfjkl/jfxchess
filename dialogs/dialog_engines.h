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

#ifndef DIALOG_ENGINES_H
#define DIALOG_ENGINES_H

#include <QList>
#include <QDialog>
#include <QListWidget>
#include "model/engine.h"
#include "model/game_model.h"

class DialogEngines : public QDialog
{
    Q_OBJECT
public:
    explicit DialogEngines(GameModel *gameModel, QWidget *parent = 0);
    QList<Engine*> *engines;
    Engine *active_engine;
    QListWidget *lstEngines;
    QString lastAddedEnginePath;

private:
    QPushButton *btnAdd;
    QPushButton *btnRemove;
    QPushButton *btnParameters;
    QPushButton *btnResetParameters;
    void delay(int ms);
    void clearOptions(Engine *e);

signals:

public slots:
    void onAddEngine();
    void onRemoveEngine();
    void onEditParameters();
    void onResetParameters();
    void onSelectEngine();
};

#endif // DIALOG_ENGINES_H
