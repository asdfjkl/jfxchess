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


#ifndef UCI_CONTROLLER_H
#define UCI_CONTROLLER_H

#include <QObject>
#include <QThread>
#include <QTimer>
#include <QMap>
#include "uci_worker.h"
#include "engine_info.h"
#include "model/engine_option.h"

class UciController : public QObject
{
    Q_OBJECT

public:
    explicit UciController(QObject *parent = 0);
    void stopEngine();
    void startEngine(const QString &path);
    void uciNewgame();
    void uciSendCommand(const QString &command);
    void uciSendPosition(const QString &position);
    void uciOk();
    void uciStrength(int level);
    void uciGoMovetime(int milliseconds);
    void uciGoInfinite();
    void uciSendFen(const QString &fen);
    void sendEngineOptions(QList<EngineOption*> *optList);

    // todo
    // void uciSendEngineOptions();
    //void resetEngine()

private:
    QThread* thread;
    UciWorker* worker;
    QTimer* timer;

signals:
    void newCommand(QString cmd);
    void newFen(QString fen);
    void updateInfo(QString engine_info);
    void bestmove(QString uci_move);
    void bestPv(QString pvs);
    void mateDetected(int mateIn);
    void eval(float eval);

public slots:
    void onBestmove(QString uci_move);
    void onInfo(QString engine_info);
    void onError(QString error);
    void onBestPv(QString pvs);
    void onMateDetected(int mateIn);
    void onEval(float eval);
};

#endif // UCI_CONTROLLER_H
