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


#ifndef ENGINE_INFO_H
#define ENGINE_INFO_H

#include <QString>
#include <QList>
#include <QRegularExpression>

const QRegularExpression READYOK = QRegularExpression("readyok");
const QRegularExpression SCORECP = QRegularExpression("score\\scp\\s-{0,1}(\\d)+");
const QRegularExpression NPS = QRegularExpression("nps\\s(\\d)+");
const QRegularExpression DEPTH = QRegularExpression("depth\\s(\\d)+");
const QRegularExpression MATE = QRegularExpression("score\\smate\\s-{0,1}(\\d)+");
const QRegularExpression CURRMOVENUMBER = QRegularExpression("currmovenumber\\s(\\d)+");
const QRegularExpression CURRMOVE = QRegularExpression("currmove\\s[a-z]\\d[a-z]\\d[a-z]{0,1}");
const QRegularExpression BESTMOVE = QRegularExpression("bestmove\\s[a-z]\\d[a-z]\\d[a-z]{0,1}");
const QRegularExpression PV = QRegularExpression("pv(\\s[a-z]\\d[a-z]\\d[a-z]{0,1})+");
const QRegularExpression POS = QRegularExpression("position\\s");
const QRegularExpression IDNAME = QRegularExpression("id\\sname ([^\n]+)");
const QRegularExpression MOVE = QRegularExpression("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})\\s");
const QRegularExpression MOVES = QRegularExpression("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
const QRegularExpression MULTIPV = QRegularExpression("multipv\\s(\\d)+");

const int MAX_MULTIPV = 4;

class EngineInfo
{
public:

    QString id;
    float score[MAX_MULTIPV];
    int strength;
    int mate[MAX_MULTIPV];
    int depth;
    int current_fullmove_no;
    int fullmove_no;
    int halfmoves;
    int maxPvSeen;
    bool seesMate[MAX_MULTIPV];
    QString current_move;
    int nps;
    QString pv;
    bool flip_eval;
    QStringList pv_list;
    QString pv_san[MAX_MULTIPV];
    bool turn;
    QString fen;

    EngineInfo();
    void update(QString engine_feedback, QString fen);
    QString toString();

private:

    void updateSan(int multiPvIndex);

};

#endif // ENGINE_INFO_H
