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


#ifndef FUNCT_H
#define FUNCT_H
#include <QObject>
#include "chess/board.h"
#include "uci/engine_info.h"


namespace chess {

class FuncT : public QObject
{
    Q_OBJECT

public:
    explicit FuncT(QObject *parent = 0);
    void run_pgnt();
    void run_sant();
    void run_pertf();
    void run_pgn_scant();
    void run_ucit();
    void run_zobrist_test();
    void run_polyglot();
    void run_rand();

    void run_pgn_speedtest();
    void run_pgn_parse_speedtest();


private:
    int count_moves(Board b, int depth);
    void rwrw_pgn(const QString &fn_in, const QString &fn_out);

signals:

public slots:
    void printInfo(QString info);
    void printBestmove(const QString &move);

};

}
#endif // PERFT_H

