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


#include "chess/pgn_reader.h"
#include "chess/game.h"
#include "chess/game_node.h"
#include <QFile>
#include <QTextStream>
#include <iostream>
#include <QStack>
#include <QDebug>

namespace chess {

/*
QList<HeaderOffset*>* PgnReader::scan_headers(const QString &filename) {

    QList<HeaderOffset*> *header_offsets = new QList<HeaderOffset*>();
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    QMap<QString,QString> *game_header = new QMap<QString,QString>();
    qint64 game_pos = -1;

    QTextStream in(&file);
    QString line = QString("");
    qint64 last_pos = in.pos();

    qDebug() << "before while loop";
    int i= 0;
    while(!in.atEnd()) {

        i++;
        line = in.readLine();

        if(i%1000==0) {
            qDebug() << "read next line: " << i;
        }
        // skip comments
        if(line.startsWith("%")) {
            line = in.readLine();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            if(match_t.hasMatch()) {

                if(game_pos == -1) {
                    game_header->insert("Event","?");
                    game_header->insert("Site","?");
                    game_header->insert("Date","????.??.??");
                    game_header->insert("Round","?");
                    game_header->insert("White","?");
                    game_header->insert("Black","?");
                    game_header->insert("Result","*");

                    game_pos = last_pos;
                }

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);
                game_header->insert(tag,value);

                last_pos = in.pos();
                line = in.readLine();
                continue;
            }
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            HeaderOffset *ho = new HeaderOffset();
            ho->headers = game_header;
            ho->offset = game_pos;

            header_offsets->append(ho);
            game_pos = -1;
            game_header = new QMap<QString,QString>();
        }

        last_pos = in.pos();
        line = in.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        HeaderOffset *ho = new HeaderOffset();
        ho->headers = game_header;
        ho->offset = game_pos;

        header_offsets->append(ho);
        game_pos = -1;
        game_header = new QMap<QString,QString>();
    }

    return header_offsets;
}*/

QString* PgnReader::readFileIntoString(const QString &filename) {

    QFile file(filename);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        throw std::invalid_argument("could readGameIntoString w/ supplied filename");

    QTextStream in(&file);
    QString everything = in.readAll();
    QString *everything_on_heap = new QString(everything);
    return everything_on_heap;
}

QList<HeaderOffset*>* PgnReader::scan_headers(const QString &filename) {

    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        throw std::invalid_argument("could not scan header of file w/ supplied filename");

    QTextStream in(&file);
    QString everything = in.readAll();
    file.close();
    return this->scan_headersFromString(&everything);
}

QList<HeaderOffset*>* PgnReader::scan_headersFromString(QString *contents) {

    QList<HeaderOffset*> *header_offsets = new QList<HeaderOffset*>();

    bool inComment = false;

    QMap<QString,QString> *game_header = new QMap<QString,QString>();
    qint64 game_pos = -1;

    QTextStream in(contents);
    QString line = QString("");
    qint64 last_pos = in.pos();

    int i= 0;
    while(!in.atEnd()) {

        i++;
        line = in.readLine();

        // skip comments
        if(line.startsWith("%")) {
            line = in.readLine();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            if(match_t.hasMatch()) {

                if(game_pos == -1) {
                    game_header->insert("Event","?");
                    game_header->insert("Site","?");
                    game_header->insert("Date","????.??.??");
                    game_header->insert("Round","?");
                    game_header->insert("White","?");
                    game_header->insert("Black","?");
                    game_header->insert("Result","*");
                    game_pos = last_pos;
                }

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);
                game_header->insert(tag,value);


                last_pos = in.pos();
                //line = in.readLine();
                continue;
            }
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            HeaderOffset *ho = new HeaderOffset();
            ho->headers = game_header;
            ho->offset = game_pos;

            header_offsets->append(ho);
            game_pos = -1;
            game_header = new QMap<QString,QString>();
        }

        last_pos = in.pos();
        line = in.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        HeaderOffset *ho = new HeaderOffset();
        ho->headers = game_header;
        ho->offset = game_pos;

        header_offsets->append(ho);
        game_pos = -1;
        game_header = new QMap<QString,QString>();
    }

    return header_offsets;
}


Game* PgnReader::readGameFromFile(const QString &filename) {
    return this->readGameFromFile(filename,0);
}

Game* PgnReader::readGameFromString(QString *pgn_string) {
    QTextStream in(pgn_string);
    return this->readGame(in);
}

Game* PgnReader::readGameFromString(QString *pgn_string, quint64 offset) {
    QString *substring = new QString(pgn_string->mid(offset, pgn_string->size()));
    QTextStream in(substring);
    return this->readGame(in);
}


Game* PgnReader::readGameFromFile(const QString &filename, qint64 offset) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }
    QTextStream in(&file);
    if(offset != 0 && offset > 0) {
        in.seek(offset);
    }
    try {
        chess::Game *g = this->readGame(in);
        file.close();
        return g;
    } catch(std::invalid_argument e) {
        file.close();
        throw e;
    }
}

Game* PgnReader::readGame(QTextStream& in) {

    Game* g = new Game();
    QString starting_fen = QString("");

    QStack<GameNode*> *game_stack = new QStack<GameNode*>();
    game_stack->push(g->getRootNode());
    GameNode* current = g->getRootNode();

    QString line = in.readLine();
    while (!in.atEnd()) {
        if(line.startsWith("%") || line.isEmpty()) {
            line = in.readLine();
            continue;
        }

        QRegularExpressionMatch match_t = TAG_REGEX.match(line);

        if(match_t.hasMatch()) {
            QString tag = match_t.captured(1);
            QString value = match_t.captured(2);
            g->headers->insert(tag,value);
            if(tag == QString("FEN")) {
                starting_fen = value;
            }
        } else {
            break;
        }

        line = in.readLine();
    }
    // set starting fen, if available
    if(!starting_fen.isEmpty()) {
        chess::Board *b_fen = new chess::Board(starting_fen);
        if(!b_fen->is_consistent()) {
            throw std::invalid_argument("starting fen position is not consistent");
        } else {
            current->setBoard(b_fen);
        }
    }

    // Get the next non-empty line.
    while(line.trimmed() == QString("") && !line.isEmpty()) {
        line = in.readLine();
    }

    bool foundContent = false;
    bool last_line = false;
    while(!in.atEnd() || !last_line || !line.isEmpty()) {
        if(in.atEnd()) {
            last_line = true;
        }
        bool readNextLine = true;
        if(line.trimmed().isEmpty() && foundContent) {
            return g;
        }
        QRegularExpressionMatchIterator i = MOVETEXT_REGEX.globalMatch(line);
        while (i.hasNext()) {
            QRegularExpressionMatch match = i.next();
            QString token = match.captured(0);
            // qDebug() << token;
            if(token.startsWith("%")) {
                line = in.readLine();
                continue;
            }
            if(token.startsWith("{")) {
                line = token.remove(0,1);
                QStringList *comment_lines = new QStringList();
                // get comments - possibly over multiple lines
                // qDebug() << "line after token cut: " << line;
                if(line.isEmpty()) {
                    line = in.readLine();
                }
                while(!line.isEmpty() && !line.contains("}")) {
                    comment_lines->append(line.trimmed());
                    line = in.readLine();
                }
                int end_index = line.indexOf("}");
                if(end_index != -1) {
                    // create a copy, so that we don't
                    // destroy original line...
                    QString comment_line = QString(line);
                    comment_line.remove(end_index,line.length()-end_index);
                    comment_lines->append(comment_line);
                }
                if(line.contains("}")) {
                    end_index = line.indexOf("}");
                    line = line.remove(0,end_index+1);
                } else {
                    line = QString("");
                }
                QString comment_joined = comment_lines->join(QString("\n"));
                current->setComment(comment_joined);
                // if the line didn't end with }, we don't want to read the next line yet
                if(!line.trimmed().isEmpty()) {
                    readNextLine = false;
                }
                break;
            }
            else if(token.startsWith("$")) {
                // found a nag
                int nag = token.remove(0,1).toInt();
                current->addNag(nag);
            }
            else if(token == QString("?")) {
                current->addNag(NAG_MISTAKE);
            }
            else if(token == QString("??")) {
                current->addNag(NAG_BLUNDER);
            }
            else if(token == QString("!")) {
                current->addNag(NAG_GOOD_MOVE);
            }
            else if(token == QString("!!")) {
                current->addNag(NAG_BRILLIANT_MOVE);
            }
            else if(token == QString("!?")) {
                current->addNag(NAG_SPECULATIVE_MOVE);
            }
            else if(token == QString("?!")) {
                current->addNag(NAG_DUBIOUS_MOVE);
            }
            else if(token == QString("(")) {
                // put current node on stack, so that we don't forget it.
                game_stack->push(current);
                current = current->getParent();
            }
            else if(token == QString(")")) {
                // pop from stack. but always leave root
                if(game_stack->size() > 1) {
                    current = game_stack->pop();
                }
            }
            else if(token == QString("1-0")) {
                g->setResult(RES_WHITE_WINS);
                foundContent = true;
            }
            else if(token == QString("0-1")) {
                g->setResult(RES_BLACK_WINS);
                foundContent = true;
            }
            else if(token == QString("1/2-1/2")) {
                g->setResult(RES_DRAW);
                foundContent = true;
            }
            else if(token == QString("*")) {
                g->setResult(RES_UNDEF);
                foundContent = true;
            }
            else { // this should be a san token
                foundContent = true;

                // zeros in castling (common bug)
                if(token==QString("0-0")) {
                    token = QString("O-O");
                } else if(token ==QString("0-0-0")) {
                    token = QString("O-O-O");
                }
                try {
                    Board *b = current->getBoard();
                    //qDebug() << "token: " << token;
                    Move *m = new Move(b->parse_san(token));
                    //qDebug() << "uci: " << m->uci_string;
                    //qDebug() << "san:" << b->san(*m);
                    //qDebug() << "--";
                    Board *b_next = b->copy_and_apply(*m);
                    GameNode *next = new GameNode();
                    next->setMove(m);
                    next->setBoard(b_next);
                    next->setParent(current);
                    current->addVariation(next);
                    current = next;
                    /*
                    if(token==QString("O-O-O")) {
                        Move mtest = b->parse_san(token);
                        std::cout << "castles, returned: " << +mtest.from << + mtest.to << std::endl;
                    }*/
                }
                catch(std::invalid_argument a) {
                    // just silently fail...
                    std::cout << a.what() << std::endl;
                    throw std::invalid_argument("unable to parse game fen@ " + token.toStdString());
                }
            }
        }
        if(readNextLine) {
            line = in.readLine();
        }
    }
    return g;
}
}
