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


#include "uci_worker.h"
#include <iostream>
#include <QMap>
#include <QDebug>
#include <QThread>

UciWorker::UciWorker(QObject *parent) :
    QObject(parent)
{
    //this->cmd_queue = new QQueue<QString>();
    //this->engine_info = new EngineInfo();
    // this->process = new QProcess();
    //this->go_infinite = false;
    //this->current_fen = QString("");

}


void UciWorker::processCommands() {
    if(this->process->state() == QProcess::NotRunning &&
            !this->cmd_queue->isEmpty()) {
        QString msg = this->cmd_queue->dequeue();
        if(msg.startsWith("start_engine?")) {
            QString path = msg.split("?")[1];
            this->process->start(path.append("\n"));
            this->engine_info->strength = -1;
        }
    }
    else if(this->process->state() == QProcess::Running) {
        QString output = QString(this->process->readAllStandardOutput());
        if(!output.isEmpty()) {
            //qDebug() << "RECEIVING: " << output;
            this->engine_info->update(output, this->current_fen);
            if(this->engine_info->seesMate[0]) {
                emit(mateDetected(this->engine_info->mate[0]));
            }
            if(!this->engine_info->pv.isEmpty()) {
                emit(bestPv(this->engine_info->pv));
            }
            emit(eval(this->engine_info->score[0]));
            emit(info(this->engine_info->toString()));
            QStringList lines = output.split("\n");
            for(int i=0;i<lines.count();i++) {
                QString line = lines.at(i);
                QRegularExpressionMatch bm_match = REG_BESTMOVE.match(line);
                if (bm_match.hasMatch() && bm_match.captured(1) != 0) {
                    QString uci_move = bm_match.captured(1);
                    emit(bestmove(uci_move));
                }
            }
        }
        if(!this->cmd_queue->isEmpty()) {
            // first check if we are in go infinite mode
            // if so, first send a stop command to the engine
            // before processing further commands from the queue
            if(this->go_infinite) {
                this->process->write("stop\n");
                this->process->waitForBytesWritten();
            }
            QString msg = this->cmd_queue->dequeue();
            //qDebug() << "SENDING: " << msg;
            this->go_infinite = false;
            // if the command is "position fen moves", first count the
            // numbers of moves so far to generate move numbers in engine info
            if(msg.startsWith("position")) {
                QRegularExpressionMatch mv_match = REG_MOVES.match(msg);
                int cnt = mv_match.lastCapturedIndex();
                if(cnt > 0) {
                    this->engine_info->halfmoves = cnt;
                }
            }
            if(msg.startsWith("quit")) {
                this->process->write("quit\n");
                this->process->waitForBytesWritten();
                this->process->waitForFinished();
            }
            else if(msg.startsWith("go infinite")) {
                //QThread::sleep(10);
                this->go_infinite = true;
                this->process->write("go infinite\n");
                this->process->waitForBytesWritten();
                //QThread::sleep(1);
            }
            else if(msg.startsWith("setoption name Skill Level")) {                
                QRegularExpressionMatch strength_match = REG_STRENGTH.match(msg);
                if(strength_match.hasMatch()) {
                    int val_i = strength_match.captured(0).remove(0,18).toInt();
                    this->engine_info->strength = val_i;
                }
                this->process->write(msg.append("\n").toLatin1());
                this->process->waitForBytesWritten();
            } else if(msg.startsWith("setoption name MultiPV value")) {
                int nrLines = msg.mid(29,30).toInt();
                this->engine_info->nrPvLines = nrLines;
                this->process->write(msg.append("\n").toLatin1());
                this->process->waitForBytesWritten();
            } else {
                this->process->write(msg.append("\n").toLatin1());
                this->process->waitForBytesWritten();
            }
        }
    }
}

void UciWorker::addCommand(const QString cmd) {
    // if there are just too many unprocessed
    // commands in the queue, then we simply
    // drop the oldest ones...
    // this usually only happens if the user
    // is quickly browsing the game by forward/back
    // keys
    if(this->cmd_queue->size() > 15) {
        for(int i=0;i<8;i++) {
            this->cmd_queue->removeLast();
        }
    }
    this->cmd_queue->enqueue(cmd);
}

void UciWorker::updateFen(const QString fen) {
    this->current_fen = fen;
}


void bestmove(QString uci_move);
//void info(EngineInfo* engine_info);
void error(QString error);
