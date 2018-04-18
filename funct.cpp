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


#include <QList>
#include <Qt>
#include <QFile>
#include <QTextStream>
#include <iostream>
#include "chess/board.h"
#include "chess/move.h"
#include "funct.h"
#include "chess/pgn_reader.h"
#include "chess/pgn_printer.h"
#include "uci/uci_controller.h"
#include "chess/polyglot.h"
#include "various/resource_finder.h"
#include <QMutex>
#include <QDir>
#include <QDebug>

namespace chess {

FuncT::FuncT(QObject *parent) :
    QObject(parent)
{
    //std::cout << "starting up..." << std::endl;
}

void FuncT::printInfo(QString info) {
    std::cout << info.toStdString() << std::endl;
    /*
    std::cout << "id             : " << info->id.toStdString() << std::endl;
    std::cout << "score          : " << info->score << std::endl;
    std::cout << "strength       : " << info->strength << std::endl;
    std::cout << "mate           : " << info->mate << std::endl;
    std::cout << "depth          : " << info->depth << std::endl;
    std::cout << "curr full move : " << info->current_fullmove_no << std::endl;
    std::cout << "halfmoves      : " << info->halfmoves << std::endl;
    std::cout << "curr move      : " << info->current_move.toStdString() << std::endl;
    std::cout << "nps            : " << info->nps << std::endl;
    std::cout << "flip eval      : " << info->flip_eval << std::endl;
    std::cout << "turn           : " << info->turn << std::endl;
    std::cout << "fen            : " << info->fen.toStdString() << std::endl;
    std::cout << "pv             : " << info->pv_san.toStdString() << std::endl;
    */
}

void FuncT::printBestmove(const QString &move) {
    std::cout << move.toStdString() << std::endl;
}


void FuncT::run_ucit() {

    UciController *u = new UciController();
    connect(u,&UciController::updateInfo, this, &FuncT::printInfo);
    connect(u,&UciController::bestmove, this, &FuncT::printBestmove);

    u->startEngine("/usr/games/stockfish");
    u->uciNewgame();
    u->uciSendCommand("uci id");
    u->uciSendFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    u->uciSendPosition("position startpos moves e2e4 e7e5");
    u->uciGoInfinite();
    QMutex mut;
    mut.lock();
    mut.tryLock(1000);
    mut.unlock();
    //u->stopEngine();


}


int FuncT::count_moves(Board b, int depth) {

    int count = 0;
    QVector<Move> mvs = b.legal_moves();
    for(int i=0;i<mvs.count();i++) {
        Move mi = mvs.at(i);
        QString mv_uci = mi.uci();
        //qDebug() << "foo: "<< mv_uci;
    }
    if(depth == 0) {
        int cnt = mvs.count();
        return cnt;
    } else {
        // recursive case: for each possible move, apply
        // the move, do the recursive call and undo the move
        for(int i=0;i<mvs.count();i++) {
            Move m = mvs.at(i);
            QString old_fen = b.fen();
            b.apply(m);
            int cnt_i = count_moves(b, depth - 1);
            count += cnt_i;
            b.undo();
            QString old_fen1 = b.fen();
            if(QString::compare(old_fen, old_fen1, Qt::CaseInsensitive) != 0) {
                int foobar = QString::compare(old_fen, old_fen1, Qt::CaseInsensitive);
                std::cout << "MAJOR FOOBAR: " << foobar << std::endl;
                std::cout << "MOVE: " << m.uci().toStdString() << std::endl;
                std::cout << "OLD: " << old_fen.toStdString() << std::endl;
                std::cout << "NEW: " << old_fen1.toStdString() << std::endl;
            }            
        }
        return count;
    }

}

void FuncT::rwrw_pgn(const QString &fn_in, const QString &fn_out) {

    PgnReader* pgn_r = new PgnReader();
    Game *g = pgn_r->readGameFromFile(fn_in,0);
    PgnPrinter* pgn_p = new PgnPrinter();
    //QStringList* pgn = pgn_p->printGame(g);
    pgn_p->writeGame(g, fn_out);

}
/*
void FuncT::run_pgn_scant() {

    PgnReader* pgn_r = new PgnReader();
    QString k1 = QString("/home/user/workspace/jerryv3/data/kasparov-deep-blue-1997.pgn");
    QList<HeaderOffset*>* ho = pgn_r->scan_headers(k1);
    for(int i=0;i<ho->count();i++) {
        HeaderOffset *hoi = ho->at(i);
        std::cout << "Offset: " << +hoi->offset << std::endl;
        // first print the headers
        QMapIterator<QString, QString> j(*(hoi->headers));
        while (j.hasNext()) {
            j.next();
            QString key = j.key();
            QString value = j.value();
            std::cout << "[" << key.toStdString() << " \"" << value.toStdString() << "\"]" << std::endl;
        }
        std::cout << std::endl;
    }
    PgnPrinter *pgn_p = new PgnPrinter();
    for(int i=0;i<ho->count();i++) {
        HeaderOffset *hoi = ho->at(i);
        Game *g = pgn_r->readGameFromFile(k1,hoi->offset);
        QString out = k1 + QString::number(i);
        pgn_p->writeGame(g,out);
    }

}
*/

void FuncT::run_pgnt() {

    std::cout << "reading complex.pgn..." << std::endl;
    QString c1 = QString("/home/user/workspace/jerryv3/data/complex.pgn");
    QString c2 = QString("/home/user/workspace/jerryv3/data/complex2.pgn");
    QString c3 = QString("/home/user/workspace/jerryv3/data/complex3.pgn");
    this->rwrw_pgn(c1,c2);
    this->rwrw_pgn(c2,c3);

    std::cout << "reading simple.pgn..." << std::endl;
    QString s1 = QString("/home/user/workspace/jerryv3/data/simple.pgn");
    QString s2 = QString("/home/user/workspace/jerryv3/data/simple2.pgn");
    QString s3 = QString("/home/user/workspace/jerryv3/data/simple3.pgn");
    this->rwrw_pgn(s1,s2);
    this->rwrw_pgn(s2,s3);


    std::cout << "reading nakamura_rybka_ascii_linux.pgn..." << std::endl;
    QString nr1 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_linux.pgn");
    QString nr2 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_linux1.pgn");
    QString nr3 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_linux2.pgn");
    this->rwrw_pgn(nr1,nr2);
    this->rwrw_pgn(nr2,nr3);

    std::cout << "reading nak. ryb ascii winline.pgn..." << std::endl;
    QString nr_a1 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_winline.pgn");
    QString nr_a2 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_winline1.pgn");
    QString nr_a3 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_winline2.pgn");
    this->rwrw_pgn(nr_a1,nr_a2);
    this->rwrw_pgn(nr_a2,nr_a3);

    std::cout << "reading nakamura rybka.pgn..." << std::endl;
    QString nr_b1 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka.pgn");
    QString nr_b2 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka1.pgn");
    QString nr_b3 = QString("/home/user/workspace/jerryv3/data/nakamura_rybka2.pgn");
    this->rwrw_pgn(nr_b1,nr_b2);
    this->rwrw_pgn(nr_b2,nr_b3);

    std::cout << "reading kasp deep blue.pgn..." << std::endl;
    QString k1 = QString("/home/user/workspace/jerryv3/data/kasparov-deep-blue-1997.pgn");
    QString k2 = QString("/home/user/workspace/jerryv3/data/kasparov-deep-blue-19971.pgn");
    QString k3 = QString("/home/user/workspace/jerryv3/data/kasparov-deep-blue-19972.pgn");
    this->rwrw_pgn(k1,k2);
    this->rwrw_pgn(k2,k3);

    std::cout << "reading lichess_test.pgn..." << std::endl;
    QString l1 = QString("/home/user/workspace/jerryv3/data/lichess_test.pgn");
    QString l2 = QString("/home/user/workspace/jerryv3/data/lichess_test1.pgn");
    QString l3 = QString("/home/user/workspace/jerryv3/data/lichess_test2.pgn");
    this->rwrw_pgn(l1,l2);
    this->rwrw_pgn(l2,l3);

}

void FuncT::run_sant() {
    // some san tests
    Board b0 = Board(QString("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1"));
    QVector<Move> lgl_b0 = b0.legal_moves();
    for(int i=0;i<lgl_b0.count();i++) {
        Move mi = lgl_b0.at(i);
        std::cout << "UCI: " << mi.uci().toStdString() << " SAN: " << b0.san(mi).toStdString() << std::endl;
    }
    std::cout << std::endl;
    std::cout << std::endl;


    Board b1 = Board(QString("rnbqkbnr/pppppppp/8/2R2R2/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1"));
    std::cout << b1 << std::endl;
    std::cout << std::endl;
    QVector<Move> lgl_b1 = b1.legal_moves();
    for(int i=0;i<lgl_b1.count();i++) {
        Move mi = lgl_b1.at(i);
        std::cout << "UCI: " << mi.uci().toStdString() << " SAN: " << b1.san(mi).toStdString() << std::endl;
    }

    std::cout << std::endl;
    std::cout << std::endl;

    Board b2 = Board(QString("rnbqkbnr/pppppppp/2Q2Q2/8/8/2Q5/PPPPPPP1/1NBQKBN1 w - - 0 1"));
    std::cout << b2 << std::endl;
    std::cout << std::endl;
    QVector<Move> lgl_b2 = b2.legal_moves();
    for(int i=0;i<lgl_b2.count();i++) {
        Move mi = lgl_b2.at(i);
        std::cout << "UCI: " << mi.uci().toStdString() << std::endl;
        QString san = b2.san(mi);
        std::cout << " SAN: " << b2.san(mi).toStdString() << std::endl;
        Move mi2 = b2.parse_san(san);
        std::cout << "  UCI again: " << mi2.uci().toStdString() << std::endl;
    }
}

void FuncT::run_polyglot() {
    //QString foo = QString("/home/user/workspace/build-jerry3-Desktop-Release/books/varied.bin");
    //

    Board *b = new Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    QString path = ResourceFinder::getPath().append("/books/");
    path = path.append(QString("varied.bin"));
    //path = QString('"').append(path).append('"');
    path = QDir::toNativeSeparators(QDir::cleanPath(path));
    qDebug() << "opening dir path:";
    qDebug() << path;
    chess::Polyglot *p = new chess::Polyglot(path);
    qDebug() << "created polyglot book";
    QVector<Move> mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
    b = new Board(QString("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4"));
    mvs = p->findMoves(b);
    for(int i=0;i<mvs.size();i++) {
        chess::Move mi = mvs.at(i);
        QString uci = mi.uci();
        qDebug() << "move: " << uci;
    }
    delete b;
}

void FuncT::run_rand() {
    int max = 10;
    for(int i = 0;i<100;i++) {
    int sel = (rand() % (int)(max));
    std::cout << sel << std::endl;
    }

}


void FuncT::run_pertf() {

    // initial position tests, perft 1 - 6
    Board b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 1, expected: 20" << std::endl;
    int c = count_moves(b,0);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 2, expected: 400" << std::endl;
    c = count_moves(b,1);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 3, expected: 8902" << std::endl;
    c = count_moves(b,2);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 4, expected: 197281" << std::endl;
    c = count_moves(b,3);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 5, expected: 4865609" << std::endl;
    c = count_moves(b,4);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // "Kiwipete" by Peter McKenzie, great for identifying bugs
    // perft 1 - 5
    b = Board(QString("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 1, expected: 48" << std::endl;
    c = count_moves(b,0);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 2, expected: 2039" << std::endl;
    c = count_moves(b,1);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 3, expected: 97862" << std::endl;
    c = count_moves(b,2);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 4, expected: 4085603" << std::endl;
    c = count_moves(b,3);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // some more pos. for bug-hunting, taken from chessprogramming wiki and
    // Sharper by Albert Bertilsson's homepage
    // perft 1 - 2
    b = Board(QString("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 1, expected: 50" << std::endl;
    c = count_moves(b,0);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 2, expected: 279" << std::endl;
    c = count_moves(b,1);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // perft 5
    b = Board(QString("rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 5, expected: 11139762" << std::endl;
    c = count_moves(b,4);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // perft 1 - 5
    b = Board(QString("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 1, expected: 44" << std::endl;
    c = count_moves(b,0);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 2, expected: 1486" << std::endl;
    c = count_moves(b,1);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 3, expected: 62379" << std::endl;
    c = count_moves(b,2);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 4, expected: 2103487" << std::endl;
    c = count_moves(b,3);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // most computational intensive (i.e. deepest) of the above
    // are at the end here
    b = Board(QString("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 5, expected: 89941194" << std::endl;
    c = count_moves(b,4);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 6, expected: 119060324" << std::endl;
    c = count_moves(b,5);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // perft 6
    b = Board(QString("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 6, expected: 11030083" << std::endl;
    c = count_moves(b,5);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // perft 7
    b = Board(QString("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 7, expected: 178633661" << std::endl;
    c = count_moves(b,6);
    std::cout << "         computed: " << c << "\n" << std::endl;

    // perft 6
    b = Board(QString("8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 6, expected: 38633283" << std::endl;
    c = count_moves(b,5);
    std::cout << "         computed: " << c << "\n" << std::endl;

    b = Board(QString("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0"));
    std::cout << "Testing " << b.fen().toStdString() << std::endl;
    std::cout << "Perft 5, expected: 193690690" << std::endl;
    c = count_moves(b,4);
    std::cout << "         computed: " << c << "\n" << std::endl;

}

void FuncT::run_zobrist_test() {

        chess::Board *b = new Board(QString("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        quint64 key = b->zobrist();
        std::cout << "expected: 463b96181691fc9c" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"));
        key = b->zobrist();
        std::cout << "expected: 823c9b50fd114196" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"));
        key = b->zobrist();
        std::cout << "expected: 756b94461c50fb0" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"));
        key = b->zobrist();
        std::cout << "expected: 662fafb965db29d4" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"));
        key = b->zobrist();
        std::cout << "expected: 22a48b5a8e47ff78" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3"));
        key = b->zobrist();
        std::cout << "expected: 652a607ca3f242c1" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4"));
        key = b->zobrist();
        std::cout << "expected: fdd303c946bdd9" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3"));
        key = b->zobrist();
        std::cout << "expected: 3c8123ea7b067637" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

        b = new Board(QString("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4"));
        key = b->zobrist();
        std::cout << "expected: 5c3f9b829b279560" << std::endl;
        std::cout << "got     : " << std::hex << key << std::dec << std::endl << std::endl;
        delete b;

}

}
