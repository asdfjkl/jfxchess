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
    this->score = 0.0;
    this->strength = -1;
    this->mate = 0;
    this->seesMate = false;
    this->current_fullmove_no = 0; // this stores currmovenumber from uci output, _NOT_ fullmovenr. from game
    this->fullmove_no = 1;
    this->halfmoves = 0;
    this->current_move = QString("");
    this->nps = 0;
    this->depth = -1;
    this->pv = QString("");
    this->flip_eval = false;
    this->pv_list = QStringList();
    this->pv_san = QString("");
    this->turn = chess::WHITE;
    this->fen = QString("");
}

void EngineInfo::update(QString engine_feedback, QString fen) {
    // update turn
    if(!fen.isEmpty()) {
        try{
            chess::Board b = chess::Board(fen);
            this->turn = b.turn;
            this->fen = fen;
            this->halfmoves = b.halfmove_clock;
            this->fullmove_no = b.fullmove_number;
        } catch(std::logic_error e) {

        }
    }
    // revert lines, to get the latest output first
    QStringList lines = engine_feedback.split("\n");
    QString line = QString("");
    for(int i=0;i<lines.count();i++) {
        QString line_i = lines.at(i);
        line.append(line_i.append("\n"));
    }
    // update score value. need to be careful about
    // - vs +, since engine reports always positive values
    QRegularExpressionMatch m_cp = SCORECP.match(line);
    if(m_cp.hasMatch()) {
        int len   = m_cp.capturedLength(0);
        float score = (m_cp.captured(0).mid(9,len-1).toFloat())/100.0;
        if(this->turn == chess::BLACK) {
            this->score = -score;
        } else {
            this->score = score;
        }
        this->seesMate = false;
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
        this->mate = abs(m_mate.captured(0).mid(11,len-1).toInt());
        this->seesMate = true;
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
        this->updateSan();
    }
    QRegularExpressionMatch m_id = IDNAME.match(line);
    if(m_id.hasMatch()) {
        int len = m_id.capturedLength(0);
        this->id = m_id.captured(0).mid(8,len-1).split("\n").at(0);
    }
}

// update san for current pv & fen string
void EngineInfo::updateSan() {

    if(this->pv_list.count()!=0 && !this->fen.isEmpty()) {

        this->pv_san = QString("");
        chess::Board b = chess::Board(this->fen);

        bool whiteMoves = true;
        int moveNo = this->fullmove_no;
        if(this->turn == chess::BLACK) {
            whiteMoves = false;
            this->pv_san.append(QString::number(moveNo).append(". ..."));
        }
        for(int i=0;i<this->pv_list.count();i++) {
            QString uci = this->pv_list.at(i);
            chess::Move mi = chess::Move(uci);
            QString san = b.san(mi);
            if(whiteMoves) {
                this->pv_san.append(" ").append(QString::number(moveNo)).append(". ").append(san);
            } else {
                this->pv_san.append(" ").append(san);
                moveNo++;
            }
            whiteMoves = !whiteMoves;
            b.apply(mi);
        }

    }
}

QString EngineInfo::toString() {
    QString outstr = QString("<table width=\"100%\"><tr>");
    if(!this->id.isEmpty()) {
        if(this->strength >= 0) {
            outstr.append("<th colspan=\"3\" align=\"left\">").append(this->id);
            if(this->id.contains("Stockfish") && this->strength == 20) {
                outstr.append(" (Level MAX)</th>");
            } else {
                outstr.append(" (Level ").append(QString::number(this->strength)).append(")</th>");
            }
        } else {
            outstr.append("<th colspan=\"3\" align=\"left\">").append(this->id).append("</th>");
        }
    }
    outstr.append("</tr><tr></tr><tr><td width=\"33%\">");
    if(this->seesMate) {
            outstr.append("#").append(QString::number(this->mate));
    } else {
        //if(this->score != 0.0) {
        outstr.append(QString::number(this->score,'f',2));
    }
    outstr.append("</td><td width=\"36%\">");
    if(!this->current_move.isEmpty()) {
        outstr.append(this->current_move);
        outstr.append(" (depth ").append(QString::number(this->current_fullmove_no)).append(")");
    }
    outstr.append("</td><td>");
    if(this->nps != 0) {
        outstr.append(QString::number(this->nps/1000)).append(" kn/s");
    }
    outstr.append("</td></tr><tr></tr><tr><td colspan=\"3\" align=\"left\">");
    if(!this->pv_san.isEmpty()) {
        //if(!this->seesMate) {
            outstr.append(this->pv_san);
        //}
    }
    outstr.append("</tr></table>");
    return outstr;
}
