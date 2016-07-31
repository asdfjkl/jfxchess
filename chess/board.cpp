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


#include <QString>
#include <QList>
#include <QDebug>
#include <QStringList>
#include "board.h"
#include <iostream>
#include <bitset>
#include <exception>
#include <algorithm>
#include <assert.h>
#include "move.h"

using namespace std;

namespace chess {

typedef std::bitset<sizeof(uint8_t)*8> IntBits;

Board::Board() {

    this->turn = WHITE;
    for(int i=0;i<120;i++) {
        this->board[i] = EMPTY_POS[i];
        this->old_board[i] = 0xFF;
    }
    this->castling_rights = 0;
    this->en_passent_target = 0;
    this->halfmove_clock = 0;
    this->fullmove_number = 1;
    this->undo_available = false;
    this->last_was_null = false;
    this->prev_inced_hm_clock = false;
}

Board::Board(Board *b) {
    this->turn = b->turn;
    this->castling_rights = 0;
    this->en_passent_target = 0;
    this->halfmove_clock = 0;
    this->fullmove_number = 1;
    this->undo_available = false;
    for(int i=0;i<120;i++) {
        this->board[i] = b->board[i];
    }
    this->last_was_null = false;
    this->prev_inced_hm_clock = false;
}

Board::Board(bool initial_position) {
    this->turn = WHITE;
    if(initial_position) {
        for(int i=0;i<120;i++) {
            this->board[i] = chess::INIT_POS[i];
            this->old_board[i] = 0xFF;
        }
        this->castling_rights = 0x0F;
    } else {
        for(int i=0;i<120;i++) {
            this->board[i] = EMPTY_POS[i];
        }
        this->castling_rights = 0;
    }
    this->en_passent_target = 0;
    this->halfmove_clock = 0;
    this->fullmove_number = 1;
    this->undo_available = false;
    this->last_was_null = false;
    this->prev_inced_hm_clock = false;
}

bool Board::is_initial_position() {
    if(!this->turn == WHITE) {
        return false;
    }
    for(int i=0;i<120;i++) {
        if(this->board[i] != chess::INIT_POS[i]) {
            return false;
        }
    }
    if(this->castling_rights != 0x0F) {
        return false;
    }
    if(this->en_passent_target != 0) {
        return false;
    }
    if(this->halfmove_clock != 0) {
        return false;
    }
    if(this->fullmove_number != 1) {
        return false;
    }
    if(this->undo_available) {
        return false;
    }
    return true;
}

// returns 0 if no e.p. field in current position
uint8_t Board::get_ep_target() {
    return this->en_passent_target;
}


// returns 'empty' if c is no valid piece symbol in
// fen notation
uint8_t Board::piece_from_symbol(QChar c) {
    if(c == QChar('K')) {
        return 0x06;
    }
    if(c == QChar('Q')) {
        return 0x05;
    }
    if(c == QChar('R')) {
        return 0x04;
    }
    if(c == QChar('B')) {
        return 0x03;
    }
    if(c == QChar('N')) {
        return 0x02;
    }
    if(c == QChar('P')) {
        return 0x01;
    }
    if(c == QChar('k')) {
        return 0x86;
    }
    if(c == QChar('q')) {
        return 0x85;
    }
    if(c == QChar('r')) {
        return 0x84;
    }
    if(c == QChar('b')) {
        return 0x83;
    }
    if(c == QChar('n')) {
        return 0x82;
    }
    if(c == QChar('p')) {
        return 0x81;
    }
    return 0x00;
}

void Board::set_piece_at(int x, int y, uint8_t piece) {
    if(x>=0 && x<8 && y>=0 && y <8 &&
            ((piece >= 0x01 && piece <= 0x06) ||
             (piece >= 0x81 && piece <= 0x86) || (piece == 0x00))) {
        int idx = ((y+2)*10) + (x+1);
        this->board[idx] = piece;
    } else {
        throw std::invalid_argument("called set_piece_at with invalid paramters");
    }

}

uint8_t Board::get_piece_at(int x, int y) {
    if(x>=0 && x<8 && y>=0 && y <8) {
        int idx = ((y+2)*10) + (x+1);
        return this->board[idx];
    } else {
        throw std::invalid_argument("called get_piece_at with invalid paramters");
    }
}

uint8_t Board::get_piece_type_at(int x, int y) {
    if(x>=0 && x<8 && y>=0 && y <8) {
        int idx = ((y+2)*10) + (x+1);
        uint8_t piece = this->board[idx];
        if(piece >= 0x80) {
            return piece - 0x80;
        } else {
            return piece;
        }
    } else {
        throw std::invalid_argument("called get_piece_type_at with invalid paramters");
    }
}

bool Board::get_piece_color_at(int x, int y) {
    if(x>=0 && x<8 && y>=0 && y <8) {
        int idx = ((y+2)*10) + (x+1);
        return this->piece_color(idx);
    } else {
        throw std::invalid_argument("called get_piece_color_at with invalid paramters");
    }
}


// returns 'empty' if c is no valid piece symbol in
// fen notation
QChar Board::piece_to_symbol(uint8_t piece) {
    if(piece == 0x06) {
        return QChar('K');
    }
    if(piece == 0x05) {
        return QChar('Q');
    }
    if(piece == 0x04) {
        return QChar('R');
    }
    if(piece == 0x03) {
        return QChar('B');
    }
    if(piece == 0x02) {
        return QChar('N');
    }
    if(piece == 0x01) {
        return QChar('P');
    }
    if(piece == 0x86) {
        return QChar('k');
    }
    if(piece == 0x85) {
        return QChar('q');
    }
    if(piece == 0x84) {
        return QChar('r');
    }
    if(piece == 0x83) {
        return QChar('b');
    }
    if(piece == 0x82) {
        return QChar('n');
    }
    if(piece == 0x81) {
        return  QChar('p');
    }
    throw std::invalid_argument("called piece to symbol, but square contains no piece!");
}

Board::Board(const QString &fen_string) {

    for(int i=0;i<120;i++) {
        this->board[i] = EMPTY_POS[i];
        this->old_board[i] = 0xFF;
    }

    // check that we have six parts in fen, each separated by space
    QStringList fen_parts = fen_string.split(QChar(' '));
    if(fen_parts.size() != 6) {
        throw std::invalid_argument("fen: not 6 fen parts");
    }
    // check that the first part consists of 8 rows, each sep. by /
    QStringList rows = fen_parts.at(0).split(QChar('/'));
    if(rows.size() != 8) {
        throw std::invalid_argument("fen: not 8 rows in 0th part");
    }
    // check that in each row, there are no two consecutive digits
    for(int i=0;i<rows.size();i++) {
        QString row = rows.at(i);
        int field_sum = 0;
        bool previous_was_digit = false;
        for(int j=0;j<row.size();j++) {
            QChar rj = row.at(j);
            QChar rjl = rj.toLower();
            if(rj == QChar('1') || rj == QChar('2') || rj == QChar('3') || rj == QChar('4')
                    || rj == QChar('5') || rj == QChar('6') || rj == QChar('7') || rj == QChar('8')) {
                if(previous_was_digit) {
                    throw std::invalid_argument("fen: two consecutive digits in rows");
                } else {
                    field_sum += rj.digitValue();
                    previous_was_digit = true;
                }
            } else if(rjl == QChar('p') || rjl == QChar('n') || rjl == QChar('b')
                      || rjl == QChar('r') || rjl == QChar('q') || rjl == QChar('k')) {
                field_sum += 1;
                previous_was_digit = false;
            } else {
                throw std::invalid_argument("fen: two consecutive chars in rows");
            }
        }
        // validate that there are 8 alphanums in each row
        if(field_sum != 8) {
            throw std::invalid_argument("fen: field sum is not 8");
        }
    }
    // check that turn part is valid
    if(!(fen_parts.at(1) == QString("w") || fen_parts.at(1) == QString("b"))) {
        throw std::invalid_argument("turn part is invalid");
    }
    // check that castles part in correctly encoded using regex
    QRegularExpressionMatch match = FEN_CASTLES_REGEX.match(fen_parts[2]);
    if(!match.hasMatch()) {
        throw std::invalid_argument("castles encoding is invalid");
    }
    // check correct encoding of en passent squares
    if(fen_parts.at(3) != QChar('-')) {
        if(fen_parts.at(1) == QChar('w')) {
            // should be something like "e6" etc. if white is to move
            // check that int value part is sixth rank
            if(!fen_parts.at(3).size() == 2 || fen_parts.at(3).at(1) != QChar('6')) {
                throw std::invalid_argument("invalid e.p. encoding (white to move)");
            }
        } else {
            if(!fen_parts.at(3).size() == 2 || fen_parts.at(3).at(1) != QChar('3')) {
                throw std::invalid_argument("invalid e.p. encoding (black to move)");
            }
        }
    }
    // half-move counter validity
    if(fen_parts.at(4).toInt() < 0) {
        throw std::invalid_argument("negative half move clock or not a number");
    }
    // full move number validity
    if(fen_parts.at(5).toInt() < 0) {
        throw std::invalid_argument("fullmove number not positive");
    }
    // set pieces
    for(int i=0;i<rows.size();i++) {
        int square_index = 91 - (i*10);
        QString row = rows.at(i);
        for(int j=0;j<row.size();j++) {
            QChar rj = row.at(j);
            QChar rjl = rj.toLower();
            if(rj == QChar('1') || rj == QChar('2') || rj == QChar('3') || rj == QChar('4')
                    || rj == QChar('5') || rj == QChar('6') || rj == QChar('7') || rj == QChar('8')) {
                square_index += rj.digitValue();
            } else if(rjl == QChar('p') || rjl == QChar('n') || rjl == QChar('b')
                      || rjl == QChar('r') || rjl == QChar('q') || rjl == QChar('k')) {
                uint8_t piece = this->piece_from_symbol(rj);
                this->board[square_index] = piece;
                square_index += 1;
            }
        }
    }
    // set turn
    if(fen_parts.at(1) == QString("w")) {
        this->turn = WHITE;
    }
    if(fen_parts.at(1) == QString("b")) {
        this->turn = BLACK;
    }
    this->castling_rights = 0x00;
    for(int i=0;i<fen_parts.at(2).size();i++) {
        QChar ci = fen_parts.at(2).at(i);
        if(ci == QChar('K')) {
            this->set_castle_wking(true);
        }
        if(ci == QChar('Q')) {
            this->set_castle_wqueen(true);
        }
        if(ci == QChar('k')) {
            this->set_castle_bking(true);
        }
        if(ci == QChar('q')) {
            this->set_castle_bqueen(true);
        }
    }
    // set en passent square
    if(fen_parts.at(3) == QString('-')) {
        this->en_passent_target = 0;
    } else {
        int row = 10 + (fen_parts.at(3).at(1).digitValue() * 10);
        int col = 0;
        QChar c = fen_parts.at(3).at(0).toLower();
        if(c == 'a') {
            col = 1;
        }
        if(c == 'b') {
            col = 2;
        }
        if(c == 'c') {
            col = 3;
        }
        if(c == 'd') {
            col = 4;
        }
        if(c == 'e') {
            col = 5;
        }
        if(c == 'f') {
            col = 6;
        }
        if(c == 'g') {
            col = 7;
        }
        if(c == 'h') {
            col = 8;
        }
        this->en_passent_target = row + col;
    }
    this->halfmove_clock = fen_parts.at(4).toInt();
    this->fullmove_number = fen_parts.at(5).toInt();
    this->undo_available = false;
    this->last_was_null = false;
    if(!this->is_consistent()) {
        throw std::invalid_argument("board position from supplied fen is inconsistent");
    }
}

QString Board::idx_to_str(int idx) {
    if(idx<21 || idx>98) {
        throw std::invalid_argument("called idx_to_str but idx is in fringe!");
    } else {
        QChar row = QChar ((idx / 10) + 47);
        QChar col = QChar ((idx % 10) + 96);
        QString str = "";
        str.append(col);
        str.append(row);
        return str;
    }
}

QString Board::fen() {
    // first build board
    QString fen_string = QString("");
    for(int i=90;i>=20;i-=10) {
        int square_counter = 0;
        for(int j=1;j<9;j++) {
            if(this->board[i+j] != 0x00) {
                uint8_t piece = this->board[i+j];
                fen_string.append(this->piece_to_symbol(piece));
                square_counter = 0;
            } else {
                square_counter += 1;
                if(j==8) {
                    fen_string.append(QChar((char) (48+square_counter)));
                } else {
                    if(this->board[i+j+1] != 0x00) {
                        fen_string.append(QChar((char) (48+square_counter)));
                    }
                }
            }
        }
        if(i!=20) {
            fen_string.append(QChar('/'));
        }
    }
    // write turn
    if(this->turn == WHITE) {
        fen_string.append(" w");
    } else {
        fen_string.append(" b");
    }
    // write castling rights
    if(this->castling_rights == 0x00) {
        fen_string.append(" -");
    } else {
        fen_string.append(' ');
        if(this->can_castle_wking()) {
            fen_string.append(('K'));
        }
        if(this->can_castle_wqueen()) {
            fen_string.append(('Q'));
        }
        if(this->can_castle_bking()) {
            fen_string.append(('k'));
        }
        if(this->can_castle_bqueen()) {
            fen_string.append(('q'));
        }
    }
    // write ep target if exists
    if(this->en_passent_target != 0x00) {
        fen_string.append(" "+this->idx_to_str(this->en_passent_target));
    } else {
        fen_string.append(" -");
    }
    // add halfmove clock and fullmove counter
    fen_string.append(" ").append(QString::number(this->halfmove_clock));
    fen_string.append(" ").append(QString::number(this->fullmove_number));
    return fen_string;
}


bool Board::can_castle_wking() {
    IntBits cstle = IntBits(this->castling_rights);
    if(cstle.test(CASTLE_WKING_POS)) {
        return true;
    } else {
        return false;
    }
}

bool Board::can_castle_bking() {
    IntBits cstle = IntBits(this->castling_rights);
    if(cstle.test(CASTLE_BKING_POS)) {
        return true;
    } else {
        return false;
    }
}

bool Board::can_castle_wqueen() {
    IntBits cstle = IntBits(this->castling_rights);
    if(cstle.test(CASTLE_WQUEEN_POS)) {
        return true;
    } else {
        return false;
    }
}

bool Board::can_castle_bqueen() {
    IntBits cstle = IntBits(this->castling_rights);
    if(cstle.test(CASTLE_BQUEEN_POS)) {
        return true;
    } else {
        return false;
    }
}

void Board::set_castle_wking(bool can_do) {
    IntBits cstle = IntBits(this->castling_rights);
    if(can_do) {
        cstle.set(CASTLE_WKING_POS);
    } else {
        cstle.reset(CASTLE_WKING_POS);
    }
    this->castling_rights = static_cast<uint8_t>(cstle.to_ulong());
}

void Board::set_castle_bking(bool can_do) {
    IntBits cstle = IntBits(this->castling_rights);
    if(can_do) {
        cstle.set(CASTLE_BKING_POS);
    } else {
        cstle.reset(CASTLE_BKING_POS);
    }
    this->castling_rights = static_cast<uint8_t>(cstle.to_ulong());
}

void Board::set_castle_wqueen(bool can_do) {
    IntBits cstle = IntBits(this->castling_rights);
    if(can_do) {
        cstle.set(CASTLE_WQUEEN_POS);
    } else {
        cstle.reset(CASTLE_WQUEEN_POS);
    }
    this->castling_rights = static_cast<uint8_t>(cstle.to_ulong());
}

void Board::set_castle_bqueen(bool can_do) {
    IntBits cstle = IntBits(this->castling_rights);
    if(can_do) {
        cstle.set(CASTLE_BQUEEN_POS);
    } else {
        cstle.reset(CASTLE_BQUEEN_POS);
    }
    this->castling_rights = static_cast<uint8_t>(cstle.to_ulong());
}

Moves* Board::pseudo_legal_moves() {
    return this->pseudo_legal_moves_from(0,true,this->turn);
}

bool Board::castles_wking(const Move &m) {
    if(this->piece_type(m.from) == KING && this->piece_color(m.from) == WHITE &&
            m.from == E1 && m.to == G1) {
        return true;
    } else {
        return false;
    }
}


bool Board::castles_wqueen(const Move &m) {
    if(this->piece_type(m.from) == KING && this->piece_color(m.from) == WHITE &&
            m.from == E1 && m.to == C1) {
        return true;
    } else {
        return false;
    }
}


bool Board::castles_bking(const Move &m) {
    if(this->piece_type(m.from) == KING && this->piece_color(m.from) == BLACK &&
            m.from == E8 && m.to == G8) {
        return true;
    } else {
        return false;
    }
}

bool Board::castles_bqueen(const Move &m) {
    if(this->piece_type(m.from) == KING && this->piece_color(m.from) == BLACK &&
            m.from == E8 && m.to == C8) {
        return true;
    } else {
        return false;
    }
}

// to get legal moves, just get list of pseudo
// legals and then filter by checking each move's
// legality
Moves* Board::legal_moves() {
    Moves* pseudo_legals = this->pseudo_legal_moves();
    Moves* legals = new Moves();
    for(int i=0;i<pseudo_legals->size();i++) {
        Move m = pseudo_legals->at(i);
        if(this->pseudo_is_legal_move(m)) {
            legals->append(m);
        }
    }
    pseudo_legals->clear();
    delete(pseudo_legals);
    return legals;
}

Moves* Board::legal_moves_from(int from_square) {
    Moves* pseudo_legals = this->pseudo_legal_moves_from(from_square, true,this->turn);
    Moves* legals = new Moves();
    for(int i=0;i<pseudo_legals->size();i++) {
        Move m = pseudo_legals->at(i);
        if(this->pseudo_is_legal_move(m)) {
            legals->append(m);
        }
    }
    pseudo_legals->clear();
    delete(pseudo_legals);
    return legals;
}

bool Board::is_legal_and_promotes(const Move &m) {
    Moves* legals = this->legal_moves_from(m.from);
    for(int i=0;i<legals->size();i++) {
        Move mi = legals->at(i);
        if(mi.from == m.from && mi.to == m.to && mi.promotion_piece != 0) {
            legals->clear();
            delete legals;
            return true;
        }
    }
    legals->clear();
    delete(legals);
    return false;
}

bool Board::is_legal_move(const Move &m) {
    Moves* pseudo_legals = this->pseudo_legal_moves_from(m.from, true,this->turn);
    for(int i=0;i<pseudo_legals->size();i++) {
        Move mi = pseudo_legals->at(i);
        if(mi == m && this->pseudo_is_legal_move(m)) {
            pseudo_legals->clear();
            delete pseudo_legals;
            return true;
        }
    }
    pseudo_legals->clear();
    delete(pseudo_legals);
    return false;
}

bool Board::pseudo_is_legal_move(const Move &m) {
    // a pseudo legal move is a legal move if
    // a) doesn't put king in check
    // b) if castle, must ensure that 1) king is not currently in check
    //                                2) castle over squares are not in check
    //                                3) doesn't castle into check
    // first find color of mover
    bool color = this->piece_color(m.from);
    // find king with that color
    for(int i= 21;i<99;i++) {
        if(this->piece_type(i) == KING && this->piece_color(i) == color) {
            // if the move is not by the king
            if(i!=m.from) {
                // apply the move, check if king is attacked, and decide
                bool legal = false;
                this->apply(m);
                legal = !this->is_attacked(i,!color);
                this->undo();
                return legal;
            } else {
                // means we move the king
                // first check castle cases
                if(this->castles_wking(m)) {
                    if(!this->is_attacked(E1,BLACK) && !this->is_attacked(F1,BLACK)
                            && !this->is_attacked(G1,BLACK)) {
                        bool legal = false;
                        this->apply(m);
                        legal = !this->is_attacked(G1,BLACK);
                        this->undo();
                        return legal;
                    } else {
                        return false;
                    }
                }
                if(this->castles_bking(m)) {
                    if(!this->is_attacked(E8,WHITE) && !this->is_attacked(F8,WHITE)
                            && !this->is_attacked(G8,WHITE)) {
                        bool legal = false;
                        this->apply(m);
                        legal = !this->is_attacked(G8,WHITE);
                        this->undo();
                        return legal;
                    } else {
                        return false;
                    }
                }
                if(this->castles_wqueen(m)) {
                    if(!this->is_attacked(E1,BLACK) && !this->is_attacked(D1,BLACK)
                            && !this->is_attacked(C1,BLACK) ) {
                        bool legal = false;
                        this->apply(m);
                        legal = !this->is_attacked(C1,BLACK);
                        this->undo();
                        return legal;
                    } else {
                        return false;
                    }
                }
                if(this->castles_bqueen(m)) {
                    if(!this->is_attacked(E8,WHITE) && !this->is_attacked(D8,WHITE)
                            && !this->is_attacked(C8,WHITE) ) {
                        bool legal = false;
                        this->apply(m);
                        legal = !this->is_attacked(C8,WHITE);
                        this->undo();
                        return legal;
                    } else {
                        return false;
                    }
                }
                // if none of the castles cases triggered, we have a standard king move
                // just check if king isn't attacked after applying the move
                bool legal = false;
                this->apply(m);
                legal = !this->is_attacked(m.to,!color);
                this->undo();
                return legal;
            }
        }
    }
    return false;
}

// doesn't account for attacks via en-passent
bool Board::is_attacked(int idx, bool attacker_color) {
    // first check for potential pawn attackers
    // attacker color white, pawn must be white.
    // lower left
    if(attacker_color == WHITE && (this->board[idx-9]!=0xFF)
            && (this->piece_color(idx-9)==WHITE) && (this->piece_type(idx-9)==PAWN)) {
        return true;
    }
    // lower right
    if(attacker_color == WHITE && (this->board[idx-11]!=0xFF)
            && (this->piece_color(idx-11)==WHITE) && (this->piece_type(idx-11)==PAWN)) {
        return true;
    }
    // check black, upper right
    if(attacker_color == BLACK && (this->board[idx+11]!=0xFF)
            && (this->piece_color(idx+11)==BLACK) && (this->piece_type(idx+11)==PAWN)) {
        return true;
    }
    // check black, upper left
    if(attacker_color == BLACK && (this->board[idx+9]!=0xFF)
            && (this->piece_color(idx+9)==BLACK) && (this->piece_type(idx+9)==PAWN)) {
        return true;
    }
    // check all squares (except idx itself)
    // for potential attackers
    for(int i=21;i<99;i++) {
        // skip empty squares
        if(i!=idx && this->board[i] != 0x00) {
            // can't attack yourself
            if(this->piece_color(i) == attacker_color) {
                uint8_t piece = this->piece_type(i);
                int distance = idx - i;
                if(distance < 0) {
                    distance = -distance;
                }
                IntBits pot_attackers = IntBits(ATTACK_TABLE[distance]);
                if((piece == KNIGHT && pot_attackers.test(0)) ||
                        (piece == BISHOP && pot_attackers.test(1)) ||
                        (piece == ROOK && pot_attackers.test(2)) ||
                        (piece == QUEEN && pot_attackers.test(3)) ||
                        (piece == KING && pot_attackers.test(4))) {
                    // the target could be a potential attacker
                    // now just get all pseudo legal moves from i,
                    // excluding castling. If a move contains
                    // target idx, then we have an attacker
                    Moves* targets = this->pseudo_legal_moves_from(i,false,attacker_color);
                    for(int j=0;j<targets->size();j++) {
                        if(targets->at(j).to == idx) {
                            targets->clear();
                            delete(targets);
                            return true;
                        }
                    }
                    targets->clear();
                    delete(targets);
                }
            }
        }
    }
    return false;
}

// calling with from_square = 0 means all possible moves
// will find all pseudo legal move for supplied player (turn must be
// either WHITE or BLACK)
Moves* Board::pseudo_legal_moves_from(int from_square, bool with_castles, bool turn) {

    Moves* moves = new Moves();

    for(int i=21;i<99;i++) {
        if(from_square == 0 || from_square == i) {
            // skip offboard's left and right
            if(!(this->board[i] == 0xFF)) {
                // get piece type & color
                bool color = this->piece_color(i);
                if(color == turn) {
                    uint8_t piece = this->piece_type(i);
                    // handle case of PAWN
                    if(piece == PAWN) {
                        uint8_t piece_idx = IDX_WPAWN;
                        if(color == BLACK) {
                            piece_idx = IDX_BPAWN;
                        }
                        // take up right, or up left
                        for(int j=3;j<=4;j++) {
                            uint8_t idx = i + DIR_TABLE[piece_idx][j];
                            if(!this->is_offside(idx)) {
                                if((!this->is_empty(idx) && color==BLACK && this->is_white_at(idx)) ||
                                        (!this->is_empty(idx) && color==WHITE && !this->is_white_at(idx))) {
                                    // if it's a promotion square, add four moves
                                    if((color==WHITE && (idx / 10 == 9)) || (color==BLACK && (idx / 10 == 2))) {
                                        moves->append(Move(i,idx,QUEEN));
                                        moves->append(Move(i,idx,ROOK));
                                        moves->append(Move(i,idx,BISHOP));
                                        moves->append(Move(i,idx,KNIGHT));
                                    } else {
                                        moves->append(Move(i,idx));
                                    }
                                }
                            }
                        }
                        // move one (j=1) or two (j=2) up (or down in the case of black)
                        for(int j=1;j<=2;j++) {
                            uint8_t idx = i + DIR_TABLE[piece_idx][j];
                            if(!this->is_offside(idx)) {
                                if(j==2 && ((color == WHITE && (i/10==3)) || (color==BLACK && (i/10==8)))) {
                                    // means we have a white/black pawn in inital position, direct square
                                    // in front is empty => allow to move two forward
                                    if(this->is_empty(idx)) {
                                        moves->append(Move(i,idx));
                                    }
                                }
                                else if(j==1) {
                                    // case of one-step move forward
                                    if(!this->is_empty(idx)) {
                                        break;
                                    } else {
                                        // if it's a promotion square, add four moves
                                        if((color==WHITE && (idx / 10 == 9)) || (color==BLACK && (idx / 10 == 2))) {
                                            moves->append(Move(i,idx,QUEEN));
                                            moves->append(Move(i,idx,ROOK));
                                            moves->append(Move(i,idx,BISHOP));
                                            moves->append(Move(i,idx,KNIGHT));
                                        } else {
                                            moves->append(Move(i,idx));
                                        }
                                    }
                                }
                            }
                        }
                        // finally, potential en-passent capture is handled
                        // left up
                        if(color == WHITE && (this->en_passent_target - i)==9) {
                            Move m = (Move(i,this->en_passent_target));
                            moves->append(m);
                        }
                        // right up
                        if(color == WHITE && (this->en_passent_target - i)==11) {
                            Move m = (Move(i,this->en_passent_target));
                            moves->append(m);
                        }
                        // left down
                        if(color == BLACK && (this->en_passent_target - i)==-9) {
                            Move m = (Move(i,this->en_passent_target));
                            moves->append(m);
                        }
                        if(color == BLACK && (this->en_passent_target - i)==-11) {
                            Move m = (Move(i,this->en_passent_target));
                            moves->append(m);
                        }
                    }
                    // handle case of knight
                    if(piece == KNIGHT || piece == KING) {
                        int lookup_idx;
                        if(piece == KNIGHT) {
                            lookup_idx = IDX_KNIGHT;
                        }
                        if(piece == KING) {
                            lookup_idx = IDX_KING;
                        }
                        for(int j=1;j<=DIR_TABLE[lookup_idx][0];j++) {
                            uint8_t idx = i + DIR_TABLE[lookup_idx][j];
                            if(!this->is_offside(idx)) {
                                if(this->is_empty(idx) ||
                                        (this->piece_color(idx) != color)) {
                                    moves->append(Move(i,idx));
                                }
                            }
                        }
                    }
                    // handle case of bishop, rook, queen
                    if(piece == ROOK || piece == BISHOP || piece == QUEEN) {
                        int lookup_idx = IDX_ROOK;
                        if(piece == QUEEN) {
                            lookup_idx = IDX_QUEEN;
                        }
                        if(piece == BISHOP) {
                            lookup_idx = IDX_BISHOP;
                        }
                        for(int j=1;j<=DIR_TABLE[lookup_idx][0] ;j++) {
                            uint8_t idx = i + DIR_TABLE[lookup_idx][j];
                            bool stop = false;
                            while(!stop) {
                                if(!this->is_offside(idx)) {
                                    if(this->is_empty(idx)) {
                                        moves->append(Move(i,idx));
                                    } else {
                                        stop = true;
                                        if(this->piece_color(idx) != color) {
                                            moves->append(Move(i,idx));
                                        }
                                    }
                                    idx = idx + DIR_TABLE[lookup_idx][j];
                                } else {
                                    stop = true;
                                }
                            }
                        }
                    }
                }
            }
            if(with_castles) {
                if(this->turn == WHITE) {
                    // check for castling
                    // white kingside
                    if(i==E1 && !this->is_empty(E1) && this->can_castle_wking() &&
                            this->piece_color(E1) == WHITE && this->piece_color(H1) == WHITE
                            && this->piece_type(E1) == KING && this->piece_type(H1) == ROOK
                            && this->is_empty(F1) && this->is_empty(G1)) {
                        moves->append(Move(E1,G1));
                    }
                    // white queenside
                    if(i==E1 && !this->is_empty(E1) && this->can_castle_wqueen() &&
                            this->piece_color(E1) == WHITE && this->piece_color(A1) == WHITE
                            && this->piece_type(E1) == KING && this->piece_type(A1) == ROOK
                            && this->is_empty(D1) && this->is_empty(C1) && this->is_empty(B1)) {
                        moves->append(Move(E1,C1));
                    }
                }
                if(this->turn == BLACK) {
                    // black kingside
                    if(i==E8 && !this->is_empty(E8) && this->can_castle_bking() &&
                            this->piece_color(E8) == BLACK && this->piece_color(H8) == BLACK
                            && this->piece_type(E8) == KING && this->piece_type(H8) == ROOK
                            && this->is_empty(F8) && this->is_empty(G8)) {
                        moves->append(Move(E8,G8));
                    }
                    // black queenside
                    if(i==E8 && !this->is_empty(E8) && this->can_castle_bqueen() &&
                            this->piece_color(E8) == BLACK && this->piece_color(A8) == BLACK
                            && this->piece_type(E8) == KING && this->piece_type(A8) == ROOK
                            && this->is_empty(D8) && this->is_empty(C8) && this->is_empty(B8)) {
                        moves->append(Move(E8,C8));
                    }
                }
            }
        }
    }
    return moves;
}

bool Board::movePromotes(const Move&m) {
    if(this->piece_type(m.from) == chess::PAWN) {
        if(this->piece_color(m.from) == chess::WHITE && ((m.to / 10)==9)) {
            return true;
        }
        if(this->piece_color(m.from) == chess::BLACK && ((m.to / 10)==2)) {
            return true;
        }
    }
    return false;
}


bool Board::piece_color(uint8_t idx) {
    IntBits piece = IntBits(this->board[idx]);
    if(piece.test(COLOR_FLAG) == WHITE) {
        return WHITE;
    } else {
        return BLACK;
    }
}

uint8_t Board::piece_type(uint8_t idx) {
    IntBits piece = IntBits(this->board[idx]);
    piece.set(7,0);
    return static_cast<uint8_t>(piece.to_ulong());
}

uint8_t Board::piece_at(uint8_t idx) {
    if(idx >= 21 && idx <= 98) {
        return this->board[idx];
    } else {
        throw std::invalid_argument("called get_piece_at with invalid paramters");
    }
}

// returns true if square is not empty
bool Board::is_empty(uint8_t idx) {
    if(this->board[idx] == 0x00) {
        return true;
    } else {
        return false;
    }
}


// returns true if square is in fringe
bool Board::is_offside(uint8_t idx) {
    if(this->board[idx] == 0xFF) {
        return true;
    } else {
        return false;
    }
}

bool Board::can_claim_fifty_moves() {
    return this->halfmove_clock >= 50;
}



// returns true (== Black) if not occupied!
bool Board::is_white_at(uint8_t idx) {
    IntBits square = IntBits(this->board[idx]);
    if(square.test(COLOR_FLAG)) {
        return false;
    } else {
        return true;
    }
}


// doesn't check legality
void Board::apply(const Move &m) {
    assert(m.promotion_piece <= 5);
    if(m.is_null) {
        //std::cout << "applying null move: " << m.uci_string.toStdString() << std::endl;
        //std::cout << (*this) << std::endl;
        this->turn = !this->turn;
        this->prev_en_passent_target = this->en_passent_target;
        this->en_passent_target = 0;
        this->last_was_null = true;
        this->undo_available = true;
    } else {
        this->last_was_null = false;
    this->turn = !this->turn;
    this->prev_en_passent_target = this->en_passent_target;
    this->prev_castling_rights = this->castling_rights;
    this->en_passent_target = 0;
    if(this->turn == BLACK) {
        this->fullmove_number++;
    }
    for(int i=0;i<120;i++) {
        this->old_board[i] = this->board[i];
    }
    uint8_t old_piece_type = this->piece_type(m.from);
    bool color = this->piece_color(m.from);
    // increase halfmove clock only if no capture or pawn advance
    // happended
    if(old_piece_type != PAWN && this->board[m.to] != EMPTY) {
        this->halfmove_clock++;
        this->prev_inced_hm_clock = true;
    } else {
        this->prev_inced_hm_clock = false;
    }
    // if we move a pawn two steps up, set the en_passent field
    if(old_piece_type == PAWN) {
        // white pawn moved two steps up
        if((m.to - m.from) == 20) {
            this->en_passent_target = m.from + 10;
        }
        // black pawn moved two steps up (down)
        if((m.to - m.from == -20)) {
            this->en_passent_target = m.from - 10;
        }
    }
    // if the move is an en-passent capture,
    // remove the (non-target) corresponding pawn
    // move is an en passent move, if
    // a) color is white, piece type is pawn, target
    // is up left or upright and empty
    // b) color is black, piece type is pawn, target
    // is down right or down left and empty
    // also set last_move_was_ep to true
    if(old_piece_type == PAWN) {
        if(this->board[m.to] == EMPTY) {
            if(color == WHITE && ((m.to-m.from == 9) || (m.to-m.from)==11)) {
                // remove captured pawn
                this->board[m.to-10] = 0x00;
            }
            if(color == BLACK && ((m.from -m.to == 9) || (m.from - m.to)==11)) {
                // remove captured pawn
                this->board[m.to+10] = 0x00;
            }
        }
    }
    // if the move is a promotion, the target
    // field becomes the promotion choice
    if(m.promotion_piece != EMPTY) {
        // true means black
        if(color == BLACK) {
            // +128 sets 7th bit to true (means black)
            this->board[m.to] = m.promotion_piece +128;
        }
        else {
            this->board[m.to] = m.promotion_piece;
        }
    } else {
        // otherwise the target is the piece on the from field
        this->board[m.to] = this->board[m.from];
    }
    this->board[m.from] = EMPTY;
    // check if the move is castles, i.e. 0-0 or 0-0-0
    // then we also need to move the rook
    // white kingside
    if(old_piece_type == KING) {
        if(color==WHITE) {
            if(m.from == E1 && m.to == G1) {
                this->board[F1] = this->board[H1];
                this->board[H1] = EMPTY;
                this->set_castle_wking(false);
            }
            // white queenside
            if(m.from == E1 && m.to == C1) {
                this->board[D1] = this->board[A1];
                this->board[A1] = EMPTY;
                this->set_castle_wqueen(false);
            } }
        else if(color==BLACK) {
            // black kingside
            if(m.from == E8 && m.to == G8) {
                this->board[F8] = this->board[H8];
                this->board[H8] = EMPTY;
                this->set_castle_bking(false);
            }
            // black queenside
            if(m.from == E8 && m.to == C8) {
                this->board[D8] = this->board[A8];
                this->board[A8] = EMPTY;
                this->set_castle_bqueen(false);
            }
        }
    }
    // check if someone loses castling rights
    // by moving king or by moving rook
    // or if one of the rooks is captured by the
    // opposite side
    if(color == WHITE) {
        if(old_piece_type == KING) {
            if(m.from == E1 && m.to !=G1) {
                this->set_castle_wking(false);
            }
            if(m.from == E1 && m.to != C1) {
                this->set_castle_wqueen(false);
            }
        }
        if(old_piece_type == ROOK) {
            if(m.from == A1) {
                this->set_castle_wqueen(false);
            }
            if(m.from == A8) {
                this->set_castle_wking(false);
            }
        }
        // white moves a piece to H8 or A8
        // means either white captures rook
        // or black has moved rook prev.
        // [even though: in the latter case, should be already
        // done by check above in prev. moves]
        if(m.to == H8) {
            this->set_castle_bking(false);
        }
        if(m.to == A8) {
            this->set_castle_bqueen(false);
        }
    }
    // same for black
    if(color == BLACK) {
        if(old_piece_type == KING) {
            if(m.from == E8 && m.to !=G8) {
                this->set_castle_bking(false);
            }
            if(m.from == E8 && m.to != C8) {
                this->set_castle_bqueen(false);
            }
        }
        if(old_piece_type == ROOK) {
            if(m.from == A8) {
                this->set_castle_bqueen(false);
            }
            if(m.from == H8) {
                this->set_castle_bking(false);
            }
        }
        // black moves piece to A1 or H1
        if(m.to == H1) {
            this->set_castle_wking(false);
        }
        if(m.to == A1) {
            this->set_castle_wqueen(false);
        }
    }
    // after move is applied, can revert to the previous position
    this->undo_available = true;
    }
}

void Board::undo() {
    if(!this->undo_available) {
        throw std::logic_error("must call board.apply(move) each time before calling undo() ");

    } else {
        if(this->last_was_null) {
            this->turn = !this->turn;
            this->en_passent_target = this->prev_en_passent_target;
            this->prev_en_passent_target = 0;
            this->last_was_null = false;
            this->undo_available = true;
        } else {
            for(int i=0;i<120;i++) {
                this->board[i] = this->old_board[i];
            }
            this->undo_available = false;
            this->en_passent_target = this->prev_en_passent_target;
            this->prev_en_passent_target = 0;
            this->castling_rights = this->prev_castling_rights;
            this->turn = !this->turn;
            if(this->prev_inced_hm_clock) {
                this->halfmove_clock--;
            }
            this->prev_inced_hm_clock = false;
            if(this->turn == BLACK) {
                this->fullmove_number--;
            }
        }
    }
}

// doesn't check legality
Board* Board::copy_and_apply(const Move &m) {
    Board *b = new Board();
    b->turn = this->turn;
    b->castling_rights = this->castling_rights;
    b->turn = this->turn;
    b->en_passent_target = this->en_passent_target;
    b->halfmove_clock = this->halfmove_clock;
    b->fullmove_number = this->fullmove_number;
    b->undo_available = this->undo_available;
    b->last_was_null = this->last_was_null;
    for(int i=0;i<120;i++) {
        b->board[i] = this->board[i];
        b->old_board[i] = this->old_board[i];
    }
    b->apply(m);
    return b;
}

bool Board::is_stalemate() {
    // search for king of player with current turn
    // check whether king is attacked
    for(int i=21;i<99;i++) {
        if((this->piece_type(i)==KING) && (this->piece_color(i)==this->turn)){
            if(!this->is_attacked(i,!this->turn)) {
                Moves* legals = this->legal_moves();
                int c = legals->count();
                legals->clear();
                delete legals;
                if(c==0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    return false;
}

bool Board::is_checkmate() {
    // search for king of player with current turn
    // check whether king is attacked
    for(int i=21;i<99;i++) {
        if((this->piece_type(i)==KING) && (this->piece_color(i)==this->turn)){
            if(this->is_attacked(i,!this->turn)) {
                Moves* legals = this->legal_moves();
                int c = legals->count();
                legals->clear();
                delete legals;
                if(c==0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    return false;
}

bool Board::is_check() {
    for(int i=21;i<99;i++) {
        if((this->piece_type(i)==KING) && (this->piece_color(i)==this->turn)){
            if(this->is_attacked(i,!this->turn)) {
                return true;
            } else {
                return false;
            }
        }
    }
    return false;
}


uint8_t Board::alpha_to_pos(QChar alpha) {
    if(alpha == QChar('A')) {
        return 1;
    } else if(alpha == QChar('B')) {
        return 2;
    } else if(alpha == QChar('C')) {
        return 3;
    } else if(alpha == QChar('D')) {
        return 4;
    } else if(alpha == QChar('E')) {
        return 5;
    } else if(alpha == QChar('F')) {
        return 6;
    } else if(alpha == QChar('G')) {
        return 7;
    } else if(alpha == QChar('H')) {
        return 8;
    }
    return 0;
}

// assumes supplied move is correct
// otherwise might mess up the whole
// current board
QString Board::san(const Move &m) {

    QString san = QString("");
    // first check for null move
    if(m.is_null) {
        san = QString("--");
        return san;
    }

    // first test for checkmate and check (to be appended later)
    // create temp board, since appyling move and
    // testing for checkmate (which again needs
    // application of a move) makes it impossible
    // to undo (undo can only be done once, not twice in a row)
    Board *b_temp = this->copy_and_apply(m);
    bool is_check = b_temp->is_check();
    bool is_checkmate = b_temp->is_checkmate();

    if(this->castles_wking(m) || this->castles_bking(m)) {
        san.append("O-O");
        if(is_checkmate) {
            san.append("#");
        }
        if(is_check) {
            san.append("+");
        }
        return san;
    } else if(this->castles_wqueen(m) || this->castles_bqueen(m)) {
        san.append("O-O-O");
        if(is_checkmate) {
            san.append("#");
        } else if(is_check) {
            san.append("+");
        }
        return san;
    } else {
        uint8_t piece_type = this->piece_type(m.from);
        Moves* legals = this->legal_moves();
        if(piece_type == KNIGHT) {
            san.append("N");
        }
        if(piece_type == BISHOP) {
            san.append("B");
        }
        if(piece_type == ROOK) {
            san.append("R");
        }
        if(piece_type == QUEEN) {
            san.append("Q");
        }
        if(piece_type == KING) {
            san.append("K");
        }
        Moves* col_disambig = new Moves();
        Moves* row_disambig = new Moves();
        int this_row = (m.from / 10) - 1;
        int this_col = m.from % 10;

        // find amibguous moves (except for pawns)
        if(piece_type != PAWN) {
            for(int i=0;i<legals->count();i++) {
                Move mi = legals->at(i);
                if(this->piece_type(mi.from) == piece_type && mi.to == m.to && mi.from != m.from) {
                    // found pontential amibg. move
                    if((mi.from % 10) != this_col) {
                        // can be resolved via row
                        col_disambig->append(mi);
                    } else { // otherwise resolve by col
                        row_disambig->append(mi);
                    }
                }
            }
            int cnt_col_disambig = col_disambig->count();
            //cout << "ambig cols: " << +cnt_col_disambig << endl;
            int cnt_row_disambig = row_disambig->count();
            //cout << "ambig rows: " << +cnt_row_disambig << endl;
            row_disambig->clear();
            col_disambig->clear();
            delete row_disambig;
            delete col_disambig;
            // if there is an ambiguity
            if(cnt_col_disambig != 0 || cnt_row_disambig != 0) {
                // preferred way: resolve via column
                if(cnt_col_disambig>0 && cnt_row_disambig==0) {
                    san.append(QChar(this_col + 96));
                    // if not try to resolve via row
                } else if(cnt_row_disambig>0 && cnt_col_disambig==0) {
                    san.append(QChar(this_row + 48));
                } else {
                    // if that also fails (think three queens)
                    // resolve via full coordinate
                    san.append(QChar(this_col + 96));
                    san.append(QChar(this_row + 48));
                }
            }
        } else {
            delete col_disambig;
            delete row_disambig;
        }
        // handle a capture, i.e. if destination field
        // is not empty
        if(this->piece_type(m.to) != EMPTY) {
            if(piece_type == PAWN) {
                san.append(QChar(this_col + 96));
            }
            san.append(QString("x"));
        }
        //qDebug() << "calling idx to str: ";
        //qDebug() << "san append: " << m.to;
        //qDebug() << m.uci_string;
        //qDebug() << "--";
        san.append(this->idx_to_str(m.to));
        if(m.promotion_piece == KNIGHT) {
            san.append(("=N"));
        }
        if(m.promotion_piece == BISHOP) {
            san.append(("=B"));
        }
        if(m.promotion_piece == ROOK) {
            san.append(("=R"));
        }
        if(m.promotion_piece == QUEEN) {
            san.append(("=Q"));
        }
    }
    if(is_checkmate) {
        san.append("#");
    } else if(is_check) {
        san.append("+");
    }
    return san;
}

Move Board::parse_san(QString san) {

    // first check if null move
    if(san==QString("--")) {
        Move m = Move();
        return m;
    }

    Move m = Move(0,0);
    Moves* legals = this->legal_moves();

    // check for castling moves
    if(san==QString("O-O") || san == QString("O-O+") || san==QString("O-O#")) {
        for(int i=0;i<legals->count();i++) {
            Move m = legals->at(i);
            if(this->castles_wking(m)) {
                legals->clear();
                delete legals;
                return Move(E1,G1);
            } else if(this->castles_bking(m)) {
                delete legals;
                return Move(E8,G8);
            }
        }
    } else if(san==QString("O-O-O") || san == QString("O-O-O+") || san==QString("O-O-O#")) {
        //qDebug() << "castles long";
        for(int i=0;i<legals->count();i++) {
            Move m = legals->at(i);
            if(this->castles_wqueen(m)) {
                legals->clear();
                delete legals;
                return Move(E1,C1);
            } else if(this->castles_bqueen(m)) {
                delete legals;
                return Move(E8,C8);
            }
        }
    } else { // we don't have a castles move
        QRegularExpressionMatch match = SAN_REGEX.match(san);
        if(!match.hasMatch()) {
            throw std::invalid_argument("invalid san: "+san.toStdString());
        }
        // get target square
        QString str_target = match.captured(4).toUpper();
        uint8_t target_col = this->alpha_to_pos(str_target.at(0));
        // -49 for ascii(1) -> int 0
        uint8_t target_row = (uint8_t) ((str_target.at(1).toLatin1()-49)+1);
        uint8_t target = ((target_row+1) * 10)+target_col;

        // get promotion piece
        QString str_prom = match.captured(5);
        if(!str_prom.isNull()) {
            //std::cout << match.captured(5).toStdString() << std::endl;
            if(match.captured(5)==QString("=N")) {
                m.promotion_piece = KNIGHT;
            } else if(match.captured(5)==QString("=B")) {
                m.promotion_piece = BISHOP;
            } else if(match.captured(5)==QString("=R")) {
                m.promotion_piece = ROOK;
            } else if(match.captured(5)==QString("=Q")) {
                m.promotion_piece = QUEEN;
            } else {
                throw std::invalid_argument("invalid san / promotion: "+match.captured(5).toStdString());
            }
            //if(this->turn == BLACK) {
            //    m.promotion_piece += 0x80;
            //}  NO: promotion piece _only_ encodes piece, _not_ color
        }
        // get piece type
        uint8_t piece_type = 0;
        if(match.captured(1) == QString("B")) {
            piece_type = BISHOP;
        } else if(match.captured(1) == QString("N")) {
            piece_type = KNIGHT;
        } else if(match.captured(1) == QString("R")) {
            piece_type = ROOK;
        } else if(match.captured(1) == QString("Q")) {
            piece_type = QUEEN;
        } else if(match.captured(1) == QString("K")) {
            piece_type = KING;
        } else {
            piece_type = PAWN;
        }

        // get target square
        uint8_t src_col = 0;
        uint8_t src_row = 0;
        QString str_amb_col = match.captured(2).toUpper();
        if(!str_amb_col.isNull()) {
            src_col = this->alpha_to_pos(str_amb_col.at(0));
        }
        QString str_amb_row = match.captured(3).toUpper();
        if(!str_amb_row.isNull()) {
            src_row = (uint8_t) ((str_amb_row.at(0).toLatin1()-49) +1);
        }

        if(m.promotion_piece!=0) {
            //std::cout << "is WHITE: " << +(this->turn==WHITE) << std::endl;
        }
        // filter all moves
        Moves lgl_piece = Moves();
        for(int i=0;i<legals->count();i++) {
            Move mi = legals->at(i);
            if(m.promotion_piece!=0) {
             //std::cout << mi.uci().toStdString() << std::endl;
             //std::cout << "  mi: " << +mi.promotion_piece << std::endl;
             //std::cout << "  m: " << +m.promotion_piece << std::endl;
            }
            uint8_t mi_row = (mi.from / 10) - 1;
            uint8_t mi_col = mi.from % 10;
            if(target == mi.to && this->piece_type(mi.from) == piece_type
                    && mi.promotion_piece == m.promotion_piece) {
                if(src_col == 0 && src_row == 0) {
                    lgl_piece.append(mi);
                } else if(src_col !=0 && src_row ==0 && mi_col == src_col) {
                    lgl_piece.append(mi);
                } else if(src_col ==0 && src_row !=0 && mi_row == src_row) {
                    lgl_piece.append(mi);
                } else if(src_col !=0 && src_row !=0 && mi_row == src_row && mi_col == src_col) {
                    lgl_piece.append(mi);
                }
            }
        }
        if(m.promotion_piece != 0 && lgl_piece.count() > 1) {
            //std::cout << "there are too many moves!" << std::endl;
        }

        // now lgl_piece should contain only one move, since
        // all ambigiuous have been filtered. otherwise san is wrong
        if(lgl_piece.count() > 1 || lgl_piece.count() == 0) {
            //std::cout << *this << std::endl;
            //std::cout << +this->fullmove_number << std::endl;
            throw std::invalid_argument("invalid san / ambiguous: "+san.toStdString());
        } else {
            Move mi = lgl_piece.at(0);
            m.from = mi.from;
            m.to = mi.to;
            m.promotion_piece = mi.promotion_piece;
            m.uci_string = mi.uci_string;
        }
    }
    return m;
}

/**
 * @brief Board::is_black_castle_right_lost
 * @return true if black king and kingside rook
 *         are on initial position, false otherwise
 *         i.e. checks the _possibility_ whether
 *         castling could be possible (to check consistency when
 *         entering a board position)
 *         to call board status, use can_castle_* functions
 */
bool Board::is_black_king_castle_right_lost() {
    if(this->board[E8] == BLACK_KING &&
            this->board[H8] == BLACK_ROOK) {
        return false;
    } else {
        return true;
    }
}

bool Board::is_black_queen_castle_right_lost() {
    if(this->board[E8] == BLACK_KING &&
            this->board[A8] == BLACK_ROOK) {
        return false;
    } else {
        return true;
    }
}

bool Board::is_white_king_castle_right_lost() {
    if(this->board[E1] == WHITE_KING &&
            this->board[H1] == WHITE_ROOK) {
        return false;
    } else {
        return true;
    }
}

bool Board::is_white_queen_castle_right_lost() {
    if(this->board[E1] == WHITE_KING &&
            this->board[A1] == WHITE_ROOK) {
        return false;
    } else {
        return true;
    }
}

bool Board::is_consistent() {
    int white_king_pos = -1;
    int black_king_pos = -1;

    int cnt_white_king = 0;
    int cnt_black_king = 0;

    int cnt_white_queens = 0;
    int cnt_white_rooks = 0;
    int cnt_white_bishops = 0;
    int cnt_white_knights = 0;
    int cnt_white_pawns = 0;

    int cnt_black_queens = 0;
    int cnt_black_rooks = 0;
    int cnt_black_bishops = 0;
    int cnt_black_knights = 0;
    int cnt_black_pawns = 0;

    for(int i=21;i<99;i++) {
        uint8_t piece_type = this->piece_type(i);
        bool piece_color = this->piece_color(i);
        if(piece_type != EMPTY) {
            if(piece_type == KING) {
                if(piece_color == WHITE) {
                    white_king_pos = i;
                    cnt_white_king++;
                } else {
                    black_king_pos = i;
                    cnt_black_king++;
                }
            } else if(piece_type == QUEEN) {
                if(piece_color == WHITE) {
                    cnt_white_queens++;
                } else {
                    cnt_black_queens++;
                }
            } else if(piece_type == ROOK) {
                if(piece_color == WHITE) {
                    cnt_white_rooks++;
                } else {
                    cnt_black_rooks++;
                }
            } else if(piece_type == BISHOP) {
                if(piece_color == WHITE) {
                    cnt_white_bishops++;
                } else {
                    cnt_black_bishops++;
                }
            } else if(piece_type == KNIGHT) {
                if(piece_color == WHITE) {
                    cnt_white_knights++;
                } else {
                    cnt_black_knights++;
                }
            } else if(piece_type == PAWN) {
                if(piece_color == WHITE) {
                    if((i / 10) == 2) { // white pawn in first rank
                        return false;
                    } else {
                        cnt_white_pawns++;
                    }
                } else {
                    if((i / 10) == 9) { // black pawn in 8th rank
                        return false;
                    } else {
                        cnt_black_pawns++;
                    }
                }
            }
        }
    }
    // exactly one white and black king exist on board
    if(white_king_pos < 21 || white_king_pos >= 99
            || black_king_pos < 21 || black_king_pos >= 99
            || cnt_white_king != 1 || cnt_black_king != 1) {
        return false;
    }
    // white and black king at least on field apart
    int larger = white_king_pos;
    int smaller = black_king_pos;
    if(black_king_pos > white_king_pos) {
        larger = black_king_pos;
        smaller = white_king_pos;
    }
    int diff = larger - smaller;
    if(diff == 10 || diff == 1 || diff == 11 || diff == 9) {
        return false;
    }
    // side not to move must not be in check
    bool not_to_move = !this->turn;
    bool to_move = this->turn;
    int idx_king_not_to_move = white_king_pos;
    if(not_to_move == BLACK) {
        idx_king_not_to_move = black_king_pos;
    }
    if(this->is_attacked(idx_king_not_to_move, to_move)) {
        return false;
    }
    // each side has 8 pawns or less
    if(cnt_white_pawns > 8 || cnt_black_pawns > 8) {
        return false;
    }
    // check whether no. of promotions and pawn count fits for white
    int white_extra_pieces = std::max(0, cnt_white_queens-1) + std::max(0, cnt_white_rooks-2)
            + std::max(0, cnt_white_bishops - 2) + std::max(0, cnt_white_knights - 2);
    if(white_extra_pieces > (8-cnt_white_pawns)) {
        return false;
    }
    // ... for black
    int black_extra_pieces = std::max(0, cnt_black_queens-1) + std::max(0, cnt_black_rooks-2)
            + std::max(0, cnt_black_bishops - 2) + std::max(0, cnt_black_knights - 2);
    if(black_extra_pieces > (8-cnt_black_pawns)) {
        return false;
    }
    // compare encoded castling rights of this board w/ actual
    // position of king and rook
    if(this->can_castle_wking() && this->is_white_king_castle_right_lost()) {
        return false;
    }
    if(this->can_castle_wqueen() && this->is_white_queen_castle_right_lost()) {
        return false;
    }
    if(this->can_castle_bking() && this->is_black_king_castle_right_lost()) {
        return false;
    }
    if(this->can_castle_bqueen() && this->is_black_queen_castle_right_lost()) {
        return false;
    }
    return true;
}


/*
 * \brief operator <<
 * \param strm
 * \param b
 * \return stream prints board representation into stream
 */
/**
 * @brief operator <<
 * @param strm
 * @param b
 * @return
 *
 * prints board as ascii output.
 *
 * Example:
 * Board b();
 * std::cout << b << std::endl;
 *
 */
std::ostream& operator<<(std::ostream &strm, const Board &b) {
    for(uint8_t i=90;i>=20;i-=10) {
        for(uint8_t j=1;j<=9;j++) {
            //uint8_t piece = b.piece_type(i+j);
            IntBits piece = IntBits(b.board[i+j]);
            bool color = WHITE;
            if(piece.test(COLOR_FLAG) == WHITE) {
                color = WHITE;
            } else {
                color = BLACK;
            }
            piece.set(3,0);
            piece.set(4,0);
            piece.set(7,0);
            if(piece == PAWN) {
                if(color == WHITE) {
                    strm << "P ";
                }
                else {
                    strm << "p ";
                }
            }
            if(piece == KNIGHT) {
                if(color == WHITE) {
                    strm << "N ";
                }
                else {
                    strm << "n ";
                }
            }
            if(piece == BISHOP) {
                if(color == WHITE) {
                    strm << "B ";
                }
                else {
                    strm << "b ";
                }
            }
            if(piece == ROOK) {
                if(color == WHITE) {
                    strm << "R ";
                }
                else {
                    strm << "r ";
                }
            }
            if(piece == QUEEN) {
                if(color == WHITE) {
                    strm << "Q ";
                }
                else {
                    strm << "q ";
                }
            }
            if(piece == KING) {
                if(color == WHITE) {
                    strm << "K ";
                }
                else {
                    strm << "k ";
                }
            }
            if(b.en_passent_target == (i+j)) {
                strm << ": ";
            } else if(piece == EMPTY) {
                strm << ". ";
            }
        }
        strm << std::endl;
    }
    return strm;
}

}
