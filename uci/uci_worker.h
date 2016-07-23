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


#ifndef UCI_WORKER_H
#define UCI_WORKER_H

#include <QObject>
#include <QMap>
#include <QQueue>
#include <QProcess>
#include <QRegularExpression>
#include "uci_worker.h"
#include "engine_info.h"

const QRegularExpression REG_MOVES = QRegularExpression("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
const QRegularExpression REG_BESTMOVE = QRegularExpression("bestmove\\s([a-z]\\d[a-z]\\d[a-z]{0,1})");
const QRegularExpression REG_STRENGTH = QRegularExpression("Skill Level value \\d+");

class UciWorker : public QObject
{
    Q_OBJECT
public:
    explicit UciWorker(QObject *parent = 0);
    QProcess* process;
    QQueue<QString>* cmd_queue;
    EngineInfo* engine_info;
    bool go_infinite;
    QString current_fen;

private:

signals:
    void bestmove(QString uci_move);
    void info(QString engine_info);
    void error(QString error);
    void bestPv(QString pvs);
    void mateDetected(int mateIn);
    void eval(float eval);

public slots:

    void processCommands();
    void addCommand(const QString cmd);
    void updateFen(const QString fen);

};

#endif // UCI_WORKER_H
