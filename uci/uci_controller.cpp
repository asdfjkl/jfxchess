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


#include "uci_controller.h"
#include "uci_worker.h"
#include <iostream>
#include <QDebug>
#include "model/engine_option.h"

UciController::UciController(QObject *parent) :
   QObject(parent)
{
    this->thread = new QThread();

    this->worker = new UciWorker();
    this->worker->process = new QProcess();
    this->worker->cmd_queue = new QQueue<QString>();
    this->worker->engine_info = new EngineInfo();
    this->worker->process = new QProcess();
    this->worker->go_infinite = false;
    this->worker->current_fen = QString("");

    this->timer = new QTimer();
    QObject::connect(this->timer, &QTimer::timeout, this->worker, &UciWorker::processCommands);
    this->timer->start(40);

    this->timer->moveToThread(this->thread);
    this->worker->moveToThread(this->thread);
    this->worker->process->moveToThread(this->thread);
    this->thread->start(QThread::LowestPriority);

    QObject::connect(this->worker,&UciWorker::bestmove,this,&UciController::onBestmove);
    QObject::connect(this->worker,&UciWorker::error,this,&UciController::onError);
    QObject::connect(this->worker,&UciWorker::info,this,&UciController::onInfo);
    QObject::connect(this->worker,&UciWorker::bestPv,this,&UciController::onBestPv);
    QObject::connect(this->worker,&UciWorker::mateDetected,this,&UciController::onMateDetected);
    QObject::connect(this->worker,&UciWorker::eval,this,&UciController::onEval);
    QObject::connect(this,&UciController::newCommand,this->worker,&UciWorker::addCommand);
    QObject::connect(this,&UciController::newFen,this->worker,&UciWorker::updateFen);



}

void UciController::onError(QString error) {
    std::cerr << error.toStdString() << std::endl;
}

void UciController::onBestmove(QString uci_move) {
    emit(bestmove(uci_move));
}

void UciController::onInfo(QString engine_info) {
    emit(updateInfo(engine_info));
}

void UciController::onBestPv(QString pvs) {
    emit(bestPv(pvs));
}

void UciController::onMateDetected(int mateIn) {
    emit(mateDetected(mateIn));
}

void UciController::onEval(float ev) {
    emit(eval(ev));
}

void UciController::stopEngine() {
    emit(newCommand(QString("quit")));
}

void UciController::startEngine(const QString &path) {
    emit(newCommand(QString("start_engine?").append(path)));
}

void UciController::uciNewgame() {
    emit(newCommand(QString("ucinewgame")));
}

void UciController::uciSendCommand(const QString &command) {
    emit(newCommand(command));
}

void UciController::uciSendPosition(const QString &position) {
    emit(newCommand(position));
}

void UciController::uciOk() {
    emit(newCommand(QString("uci")));
}

void UciController::uciStrength(int level) {
    QString lvl = QString::number(level);
    emit(newCommand(QString("setoption name Skill Level value ").append(lvl)));
}

void UciController::sendEngineOptions(QList<EngineOption*> *optList) {
    for(int i=0;i<optList->size();i++) {
        QString cmd = optList->at(i)->toUciCommandString();
        if(!cmd.isEmpty()) {
            emit(newCommand(cmd));
        }
    }
}

void UciController::uciGoMovetime(int milliseconds) {
    QString ms = QString::number(milliseconds);
    emit(newCommand(QString("go movetime ").append(ms)));
}

void UciController::uciGoInfinite() {
    emit(newCommand(QString("go infinite")));
}

void UciController::uciSendFen(const QString &fen) {
    emit(newFen(fen));
}
