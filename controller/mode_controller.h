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


#ifndef MODE_CONTROLLER_H
#define MODE_CONTROLLER_H

#include <QWidget>
#include "uci/uci_controller.h"
#include "model/game_model.h"

class ModeController : public QObject
{
    Q_OBJECT

public:
    ModeController(GameModel *gameModel, UciController *controller, QWidget *parent = 0);

private:
    UciController *uci_controller;
    GameModel *gameModel;
    QWidget *parentWidget;

    void onStateChangeEnterMoves();
    void onStateChangeAnalysis();
    void onStateChangePlayWhiteOrBlack();
    void onStateChangePlayoutPosition();
    void onStateChangeGameAnalysis();


protected:

signals:

public slots:
    void onActivateAnalysisMode();
    void onActivateEnterMovesMode();
    void onActivatePlayWhiteMode();
    void onActivatePlayBlackMode();
    void onActivatePlayoutPositionMode();
    void onActivateGameAnalysisMode();
    void onSetEnginesClicked();
    void onOptionsClicked();
    void onStateChange();
    void onBestMove(QString uci_move);

    void onBestPv(QString pvs);
    void onMateDetected(int mateIn);
    void onEval(float eval);

};

#endif // MODE_CONTROLLER_H
