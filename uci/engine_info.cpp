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


#include "engine_info.h"
#include "chess/board.h"
#include "chess/move.h"
#include <iostream>
#include <QDebug>

EngineInfo::EngineInfo()
{
    this->id = QString("");
    this->strength = -1;
    this->current_fullmove_no = 0; // this stores currmovenumber from uci output, _NOT_ fullmovenr. from game
    this->fullmove_no = 1;
    this->halfmoves = 0;
    this->current_move = QString("");
    this->nps = 0;
    this->depth = -1;
    this->pv = QString("");
    this->flip_eval = false;
    this->pv_list = QStringList();
    for(int i=0;i<MAX_MULTIPV;i++ ) {
        this->pv_san[i] = QString("");
        this->score[i] = 0.0;
        this->mate[i] = 0;
        this->seesMate[i] = false;
    }
    this->turn = chess::WHITE;
    this->fen = QString("");
    this->nrPvLines = 1;
}

void EngineInfo::update(QString engine_feedback, QString fen) {

    int multi_pv = 0;
    //qDebug() << engine_feedback;
    //qDebug() << fen;
    // update turn
    if(!fen.isEmpty()) {
        try{
            chess::Board b = chess::Board(fen);
            this->turn = b.turn;
            this->fen = fen;
            this->halfmoves = b.halfmove_clock;
            this->fullmove_no = b.fullmove_number;
        } catch(std::logic_error e) {
            //qDebug() << "error: chess logic in parsing engine output!";
        }
    }
    QStringList lines = engine_feedback.split("\n");
    for(int i=0;i<lines.length();i++) {

        QString line = lines.at(i);
        //qDebug() << line;

        QRegularExpressionMatch m_multipv = MULTIPV.match(line);
        if(m_multipv.hasMatch()) {
            //qDebug() << "LINE   :";
            //qDebug() << line;
            int len = m_multipv.capturedLength(0);
            QString test1 = m_multipv.captured(0).mid(7,len-1);
            multi_pv = m_multipv.captured(0).mid(8,len-1).toInt() - 1;
            qDebug() << "multipv identified: " << multi_pv;
        }
        // update score value. need to be careful about
        // - vs +, since engine reports always positive values
        QRegularExpressionMatch m_cp = SCORECP.match(line);
        if(m_cp.hasMatch()) {
            int len   = m_cp.capturedLength(0);
            float score = (m_cp.captured(0).mid(9,len-1).toFloat())/100.0;
            if(this->turn == chess::BLACK) {
                this->score[multi_pv] = -score;
            } else {
                this->score[multi_pv] = score;
            }
            this->seesMate[multi_pv] = false;
        }
        QRegularExpressionMatch m_nps = NPS.match(line);
        if(m_nps.hasMatch()) {
            int len = m_nps.capturedLength(0);
            this->nps = m_nps.captured(0).mid(4,len-1).toInt();
        }
        QRegularExpressionMatch m_depth = DEPTH.match(line);
        if(m_depth.hasMatch()) {
            int len = m_depth.capturedLength(0);
            this->depth = m_depth.captured(0).mid(6,len-1).toInt();
        }
        QRegularExpressionMatch m_mate = MATE.match(line);
        if(m_mate.hasMatch()) {
            int len = m_mate.capturedLength(0);
            this->mate[multi_pv] = abs(m_mate.captured(0).mid(11,len-1).toInt());
            this->seesMate[multi_pv] = true;
        }
        QRegularExpressionMatch m_currmove_no = CURRMOVENUMBER.match(line);
        if(m_currmove_no.hasMatch()) {
            int len = m_currmove_no.capturedLength(0);
            this->current_fullmove_no = m_currmove_no.captured(0).mid(15,len).toInt();
        }
        QRegularExpressionMatch m_currmove = CURRMOVE.match(line);
        if(m_currmove.hasMatch()) {
            int len = m_currmove.capturedLength(0);
            this->current_move = m_currmove.captured(0).mid(9,len-1);
        }

        QRegularExpressionMatch m_pv = PV.match(line);
        if(m_pv.hasMatch()) {
            int len = m_pv.capturedLength(0);
            QString moves = m_pv.captured(0).mid(3,len-1);
            this->pv = moves;
            this->pv_list = moves.split(" ");
            this->updateSan(multi_pv);
        }
        QRegularExpressionMatch m_id = IDNAME.match(line);
        if(m_id.hasMatch()) {
            int len = m_id.capturedLength(0);
            this->id = m_id.captured(0).mid(8,len-1).split("\n").at(0);
        }
    }
}

// update san for current pv & fen string
void EngineInfo::updateSan(int multiPvIndex) {

    qDebug() << "update san start";
    if(this->pv_list.count()!=0 && !this->fen.isEmpty()) {

        this->pv_san[multiPvIndex] = QString("");
        chess::Board b = chess::Board(this->fen);
        //std::cout << b << std::endl;
        qDebug() << "update san start";

        bool whiteMoves = true;
        int moveNo = this->fullmove_no;
        if(this->turn == chess::BLACK) {
            whiteMoves = false;
            this->pv_san[multiPvIndex].append(QString::number(moveNo).append(". ..."));
        }
        for(int i=0;i<this->pv_list.count();i++) {
            QString uci = this->pv_list.at(i);
            chess::Move mi = chess::Move(uci);
            QString san = b.san(mi);
            if(whiteMoves) {
                this->pv_san[multiPvIndex].append(" ").append(QString::number(moveNo)).append(". ").append(san);
            } else {
                this->pv_san[multiPvIndex].append(" ").append(san);
                moveNo++;
            }
            whiteMoves = !whiteMoves;
            b.apply(mi);
        }

    }
    qDebug() << "update san stop";
}

QString EngineInfo::toString() {
    QString outstr = QString("<table width=\"100%\"><tr>");

    outstr.append("<th width=10%\" align=\"left\">");
    if(!this->id.isEmpty()) {
        if(this->strength >= 0) {
            outstr.append(this->id);
            if(this->id.contains("Stockfish") && this->strength == 20) {
                outstr.append(" (Level MAX)</th>");
            } else {
                outstr.append(" (Level ").append(QString::number(this->strength));
            }
        } else {
            outstr.append(this->id);
        }
    }
    outstr.append("</th>");

    outstr.append("<th width=10%\" align=\"left\" style=\"font-weight:normal\">");
    if(!this->current_move.isEmpty()) {
        outstr.append(this->current_move);
        outstr.append(" (depth ").append(QString::number(this->current_fullmove_no)).append(")");
    }
    outstr.append("</th>");

    outstr.append("<th width=80%\" align=\"left\" style=\"font-weight:normal\">");
    if(this->nps != 0) {
        outstr.append(QString::number(this->nps/1000)).append(" kn/s");
    }
    outstr.append("</th></tr>");

    // newline
    outstr.append("<tr><td colspan=\"3\"></td></tr>");

    for(int i=0;i<4;i++) {
        outstr.append("<tr><td colspan=\"3\">");
        if(i<this->nrPvLines) {
        if(this->seesMate[i]) {
                outstr.append("(#").append(QString::number(this->mate[i])).append(") ");
        } else {
            //if(this->score != 0.0) {
            outstr.append("(").append(QString::number(this->score[i],'f',2)).append(") ");
        }
        if(!this->pv_san[i].isEmpty()) {
            //if(!this->seesMate) {
                outstr.append(this->pv_san[i]);
            //}
        }
        }
        outstr.append("</td></tr>");
    }
    outstr.append("</table>");
    return outstr;
}
