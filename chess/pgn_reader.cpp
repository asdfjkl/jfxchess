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
#include <QTextCodec>
#include <QDataStream>

namespace chess {

const char* PgnReader::detect_encoding(const QString &filename) {
    // very simple way to detecting majority of encodings:
    // first try ISO 8859-1
    // open the file and read a max of 100 first bytes
    // if conversion to unicode works, try some more bytes (at most 40 * 100)
    // if conversion errors occur, we simply assume UTF-8
    const char* iso = "ISO 8859-1";
    const char* utf8 = "UTF-8";
    QFile file(filename);
    if(!file.open(QFile::ReadOnly)) {
        return utf8;
    }
    QDataStream in(&file);
    // init some char array to read bytes
    char first100arr[100];
    for(int i=0;i<100;i++) {
        first100arr[i] = 0x00;
    }
    char *first100 = first100arr;
    // prep conversion tools
    QTextCodec::ConverterState state;
    QTextCodec *codec = QTextCodec::codecForName("UTF-8");

    int iterations = 40;
    int i=0;
    int l = 100;
    bool isUtf8 = true;
    while(i<iterations && l>=100) {
        l = in.readRawData(first100, 100);
        const QString text = codec->toUnicode(first100, 100, &state);
        if (state.invalidChars > 0) {
            isUtf8 = false;
            //qDebug() << "I think this is not UTF-8";
            break;
        }
        i++;
    }
    if(isUtf8) {
        return utf8;
    } else {
        return iso;
    }
}

QList<HeaderOffset*>* PgnReader::scan_headers(const QString &filename, const char* encoding) {

    QList<HeaderOffset*> *header_offsets = new QList<HeaderOffset*>();
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    QMap<QString,QString> *game_header = new QMap<QString,QString>();
    qint64 game_pos = -1;

    QTextStream in(&file);
    QTextCodec *codec = QTextCodec::codecForName(encoding);
    in.setCodec(codec);
    QString line = QString("");
    qint64 last_pos = in.pos();

    //qDebug() << "before while loop";
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
}

int PgnReader::readNextHeader(const QString &filename, const char* encoding,
                              quint64 *offset, HeaderOffset* headerOffset) {

    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return -1;

    file.seek(*offset);

    bool inComment = false;

    //qDebug() << "kk0";
    QMap<QString,QString> *game_header = new QMap<QString,QString>();
    qint64 game_pos = -1;

    QTextCodec *codec = QTextCodec::codecForName(encoding);

    QString line = QString("");
    qint64 last_pos = file.pos();

    QByteArray raw_line;
    //qDebug() << "kk";

    // first seek until we have new tags
    bool foundTag = false;
    while(!file.atEnd() && !foundTag) {
        last_pos = file.pos();
        raw_line = file.readLine();
        line = codec->toUnicode(raw_line);
        if(line.startsWith("%")) {
            raw_line = file.readLine();
            line = codec->toUnicode(raw_line);
            continue;
        }
        if(!inComment && line.startsWith("[")) {
            QRegularExpressionMatch match_t = TAG_REGEX.match(line);
            if(match_t.hasMatch()) {
                foundTag = true;
                game_pos = last_pos;
                continue;
            }
        }
    }
    // if foundTag is true, we need to scan all remaining headers
    // read until we encounter a comment line
    if(foundTag) {
        bool stop = false;
        game_header->insert("Event","?");
        game_header->insert("Site","?");
        game_header->insert("Date","????.??.??");
        game_header->insert("Round","?");
        game_header->insert("White","?");
        game_header->insert("Black","?");
        game_header->insert("Result","*");
        while(!file.atEnd() && !stop) {
            if(line.startsWith("[")) {
                QRegularExpressionMatch match_t = TAG_REGEX.match(line);
                if(match_t.hasMatch()) {
                    QString tag = match_t.captured(1);
                    QString value = match_t.captured(2);
                    game_header->insert(tag,value);
                }
            } else {
                stop = true;
                continue;
            }
            raw_line = file.readLine();
            line = codec->toUnicode(raw_line);
        }
        headerOffset->headers = game_header;
        headerOffset->offset = game_pos;
        //headerOffset = ho;
    } else {
        delete game_header;
        return -1;
    }
    // set offset to last encountered line
    *offset = file.pos();
    return 0;
}

QList<HeaderOffset*>* PgnReader::scan_headers_fast(const QString &filename, const char* encoding) {

    QList<HeaderOffset*> *header_offsets = new QList<HeaderOffset*>();
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    QMap<QString,QString> *game_header = new QMap<QString,QString>();
    qint64 game_pos = -1;

    QTextCodec *codec = QTextCodec::codecForName(encoding);

    QString line = QString("");
    qint64 last_pos = file.pos();

    qDebug() << "before while loop";
    int i= 0;

    QByteArray raw_line;

    while(!file.atEnd()) {
        i++;
        raw_line = file.readLine();
        line = codec->toUnicode(raw_line);

        if(i%1000==0) {
            qDebug() << "read next line: " << i;
        }
        // skip comments
        if(line.startsWith("%")) {
            raw_line = file.readLine();
            line = codec->toUnicode(raw_line);
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

                last_pos = file.pos();
                // line = in.readLine();
                raw_line = file.readLine();
                line = codec->toUnicode(raw_line);
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

        last_pos = file.pos();
        raw_line = file.readLine();
        line = codec->toUnicode(raw_line);

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


QString* PgnReader::readFileIntoString(const QString &filename, const char* encoding) {

    QFile file(filename);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        throw std::invalid_argument("could readGameIntoString w/ supplied filename");

    QTextStream in(&file);
    QTextCodec *codec = QTextCodec::codecForName(encoding);
    in.setCodec(codec);
    QString everything = in.readAll();
    QString *everything_on_heap = new QString(everything);
    return everything_on_heap;
}

/*
QList<HeaderOffset*>* PgnReader::scan_headers(const QString &filename) {

    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        throw std::invalid_argument("could not scan header of file w/ supplied filename");

    QTextStream in(&file);
    QString everything = in.readAll();
    file.close();
    return this->scan_headersFromString(&everything);
}*/

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


Game* PgnReader::readGameFromFile(const QString &filename, const char* encoding) {
    return this->readGameFromFile(filename, encoding, 0);
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


Game* PgnReader::readGameFromFile(const QString &filename, const char* encoding, qint64 offset) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }
    QTextStream in(&file);
    QTextCodec *codec = QTextCodec::codecForName(encoding);
    in.setCodec(codec);
    if(offset != 0 && offset > 0) {
        in.seek(offset);
    }
    try {
        chess::Game *g = this->readGame(in);
        file.close();
        return g;
    } catch(std::invalid_argument e) {
        qDebug() << "stuff happened";
        file.close();
        throw e;
    }
}

Game* PgnReader::readGame(QTextStream& in) {

    //qDebug() << "read game 1";
    Game* g = new Game();
    QString starting_fen = QString("");

    QStack<GameNode*> *game_stack = new QStack<GameNode*>();
    game_stack->push(g->getRootNode());
    GameNode* current = g->getRootNode();

    QString line = in.readLine();
    //qDebug() << "line @ offset: " << line;
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
    //qDebug() << "tags ok";
    // set starting fen, if available
    if(!starting_fen.isEmpty()) {
        chess::Board *b_fen = new chess::Board(starting_fen);
        if(!b_fen->is_consistent()) {
            if(b_fen != 0) {
                delete b_fen;
            }
            throw std::invalid_argument("starting fen position is not consistent");
        } else {
            current->setBoard(b_fen);
        }
    }
    //qDebug() << "initial board ok";
    // Get the next non-empty line.
    while(line.trimmed() == QString("") && !line.isEmpty()) {
        line = in.readLine();
    }

    bool foundContent = false;
    bool last_line = false;
    while(!in.atEnd() || !last_line || !line.isEmpty()) {
        //qDebug() << line;
        //qDebug() << +(line.isEmpty());
        if(in.atEnd()) {
            last_line = true;
            //qDebug() << "last line";
        }
        bool readNextLine = true;
        if(line.trimmed().isEmpty() && foundContent) {
            delete game_stack;
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
                delete comment_lines;
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
                Move *m = 0;
                GameNode *next = new GameNode();
                Board *b_next = 0;
                try {
                    Board *b = current->getBoard();
                    //qDebug() << "token: " << token;
                    m = new Move(b->parse_san(token));
                    //qDebug() << "uci: " << m->uci_string;
                    //qDebug() << "san:" << b->san(*m);
                    //qDebug() << "--";
                    b_next = b->copy_and_apply(*m);
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
                    // just silently fail... but first clean up
                    if(m!=0) {
                        //delete m;
                    }
                    //delete next;
                    if(b_next!=0) {
                        //delete b_next;
                    }
                    qDebug() << "error catch";
                    delete g;
                    game_stack->clear();
                    delete game_stack;
                    std::cout << a.what() << std::endl;
                    throw std::invalid_argument("unable to parse game fen@ " + token.toStdString());
                }
            }
        }
        if(readNextLine) {
            line = in.readLine();
        }
    }
    //qDebug() << "standard return";
    game_stack->clear();
    delete game_stack;
    return g;
}
}
