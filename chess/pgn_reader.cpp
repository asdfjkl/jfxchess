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


bool PgnReader::detectUtf8(const QString &filename) {
    // very simple way to detecting majority of encodings:
    // first try ISO 8859-1
    // open the file and read a max of 100 first bytes
    // if conversion to unicode works, try some more bytes (at most 40 * 100)
    // if conversion errors occur, we simply assume UTF-8
    //const char* iso = "ISO 8859-1";
    //const char* utf8 = "UTF-8";
    QFile file(filename);
    if(!file.open(QFile::ReadOnly)) {
        return true;
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
            break;
        }
        i++;
    }
    return isUtf8;
}


QVector<PgnHeaderOffset> PgnReader::scan_headers(const QString &filename, const char* encoding) {

    QVector<PgnHeaderOffset> header_offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    PgnHeader game_header;
    qint64 game_pos = -1;

    QByteArray line; //QString("");
    qint64 last_pos = file.pos();

    int i= 0;
    while(!file.atEnd()) {

        i++;
        line = file.readLine();

        // skip comments
        if(line.startsWith("%")) {
            line = file.readLine();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            if(match_t.hasMatch()) {

                if(game_pos == -1) {

                    /*
                    game_header.insert("Event","?");
                    game_header.insert("Site","?");
                    game_header.insert("Date","????.??.??");
                    game_header.insert("Round","?");
                    game_header.insert("White","?");
                    game_header.insert("Black","?");
                    game_header.insert("Result","*");
                    */
                    game_pos = last_pos;
                }

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);

                if(tag == "Event") {
                    game_header.event = value;
                }
                if(tag == "Site") {
                    game_header.site = value;
                }
                if(tag == "Date") {
                    game_header.date = value;
                }
                if(tag == "Round") {
                    game_header.round = value;
                }
                if(tag == "White") {
                    game_header.white = value;
                }
                if(tag == "Black") {
                    game_header.black = value;
                }
                if(tag == "Result") {
                    game_header.white = value;
                }

                last_pos = file.pos();
                line = file.readLine();
                continue;
            }
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            PgnHeaderOffset ho;
            ho.header = game_header;
            ho.offset = game_pos;

            header_offsets.append(ho);
            game_pos = -1;
            PgnHeader temp;
            game_header = temp; //new QMap<QString,QString>();
        }

        last_pos = file.pos();
        line = file.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        PgnHeaderOffset ho;
        ho.header = game_header;
        ho.offset = game_pos;

        header_offsets.append(ho);
        game_pos = -1;
        PgnHeader temp;
        game_header = temp;
    }

    return header_offsets;
}


QVector<PgnHeaderOffset> PgnReader::scan_headers_foo(const QString &filename, const char* encoding) {

    QVector<PgnHeaderOffset> header_offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    //QMap<QString,QString> game_header;
    qint64 game_pos = -1;

    //QTextStream in(&file);
    //QTextCodec *codec = QTextCodec::codecForName(encoding);
    //in.setCodec(codec);
    QString line = QString("");
    //qint64 last_pos = in.pos();

    int i= 0;
    int gameCounter = 0;
    bool foundGame = false;

    PgnHeaderOffset ho;
    quint64 offset = 0;

    while(!file.atEnd()) {
        i++;
        QByteArray line = file.readLine();
        offset += line.length();

        // skip comments
        if(line.startsWith("%")) {
            line = file.readLine();
            offset += line.length();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            if(foundGame == false) {
                header_offsets.append(ho);
                PgnHeaderOffset temp;
                ho = temp;
                ho.offset = file.pos();
                foundGame = true;
            }

            //QString text_line = QString::fromLatin1(line.data());
            QString text_line = QString::fromUtf8(line.data());
            QRegularExpressionMatch match_t = TAG_REGEX.match(text_line);

            if(match_t.hasMatch()) {

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);

                if(tag == "Event") {
                    ho.header.event = value;
                }
                if(tag == "Site") {
                    ho.header.site = value;
                }
                if(tag == "Date") {
                    ho.header.date = value;
                }
                if(tag == "Round") {
                    ho.header.round = value;
                }
                if(tag == "White") {
                    ho.header.white = value;
                }
                if(tag == "Black") {
                    ho.header.black = value;
                }
                if(tag == "Result") {
                    ho.header.white = value;
                }

                //game_headerinsert(tag,value);
                gameCounter += 1;
                //if(gameCounter % 100000 == 0) {
                //    std::cout << gameCounter << std::endl;
                //}

                // last_pos = in.pos();
                line = file.readLine();
                offset += line.length();
                continue;
            }
        } else {
            foundGame = false;
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }


        if(game_pos != -1) {
            PgnHeader game_header;
            PgnHeaderOffset ho;
            ho.header = game_header;
            ho.offset = game_pos;

            header_offsets.append(ho);
            game_pos = -1;
            //game_header = QMap<QString,QString>(); //new QMap<QString,QString>();
        }

        //last_pos = in.pos();
        line = file.readLine();
        offset += line.length();
    }
    // for the last game
    if(game_pos != -1) {
        HeaderOffset ho;
        //ho.headers = game_header;
        //ho.offset = game_pos;

        //header_offsets.append(ho);
        //game_pos = -1;
        //game_header = QMap<QString,QString>();
    }
    return header_offsets;
}


int PgnReader::readNextHeader(const QString &filename, const char* encoding,
                              quint64 offset, HeaderOffset &headerOffset) {

    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return -1;

    file.seek(offset);

    bool inComment = false;

    QMap<QString,QString> game_header;
    qint64 game_pos = -1;

    QTextCodec *codec = QTextCodec::codecForName(encoding);

    QString line = QString("");
    qint64 last_pos = file.pos();

    QByteArray raw_line;

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
        game_header.insert("Event","?");
        game_header.insert("Site","?");
        game_header.insert("Date","????.??.??");
        game_header.insert("Round","?");
        game_header.insert("White","?");
        game_header.insert("Black","?");
        game_header.insert("Result","*");
        while(!file.atEnd() && !stop) {
            if(line.startsWith("[")) {
                QRegularExpressionMatch match_t = TAG_REGEX.match(line);
                if(match_t.hasMatch()) {
                    QString tag = match_t.captured(1);
                    QString value = match_t.captured(2);
                    game_header.insert(tag,value);
                }
            } else {
                stop = true;
                continue;
            }
            raw_line = file.readLine();
            line = codec->toUnicode(raw_line);
        }
        headerOffset.headers = game_header;
        headerOffset.offset = game_pos;
    } else {
        return -1;
    }
    // set offset to last encountered line
    offset = file.pos();
    return 0;
}

QList<HeaderOffset> PgnReader::scan_headers_fast(const QString &filename, const char* encoding) {

    QList<HeaderOffset> header_offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return header_offsets;

    bool inComment = false;

    QMap<QString,QString> game_header;
    qint64 game_pos = -1;

    QTextCodec *codec = QTextCodec::codecForName(encoding);

    QString line = QString("");
    qint64 last_pos = file.pos();

    int i= 0;

    QByteArray raw_line;

    while(!file.atEnd()) {
        i++;
        raw_line = file.readLine();
        line = codec->toUnicode(raw_line);

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
                    game_header.insert("Event","?");
                    game_header.insert("Site","?");
                    game_header.insert("Date","????.??.??");
                    game_header.insert("Round","?");
                    game_header.insert("White","?");
                    game_header.insert("Black","?");
                    game_header.insert("Result","*");

                    game_pos = last_pos;
                }

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);

                game_header.insert(tag,value);

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
            HeaderOffset ho;
            ho.headers = game_header;
            ho.offset = game_pos;

            header_offsets.append(ho);
            game_pos = -1;
            game_header = QMap<QString,QString>();
        }

        last_pos = file.pos();
        raw_line = file.readLine();
        line = codec->toUnicode(raw_line);

    }
    // for the last game
    if(game_pos != -1) {
        HeaderOffset ho;
        ho.headers = game_header;
        ho.offset = game_pos;

        header_offsets.append(ho);
        game_pos = -1;
        game_header = QMap<QString,QString>();
    }

    return header_offsets;
}


QString PgnReader::readFileIntoString(const QString &filename, const char* encoding) {

    QFile file(filename);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        throw std::invalid_argument("could readGameIntoString w/ supplied filename");

    QTextStream in(&file);
    QTextCodec *codec = QTextCodec::codecForName(encoding);
    in.setCodec(codec);
    QString everything = in.readAll();
    return everything;
}

QList<HeaderOffset> PgnReader::scan_headersFromString(QString &contents) {

    QList<HeaderOffset> header_offsets;

    bool inComment = false;

    QMap<QString,QString> game_header;
    qint64 game_pos = -1;

    QTextStream in(&contents);
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
                    game_header.insert("Event","?");
                    game_header.insert("Site","?");
                    game_header.insert("Date","????.??.??");
                    game_header.insert("Round","?");
                    game_header.insert("White","?");
                    game_header.insert("Black","?");
                    game_header.insert("Result","*");
                    game_pos = last_pos;
                }

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);
                game_header.insert(tag,value);


                last_pos = in.pos();
                continue;
            }
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            HeaderOffset ho;
            ho.headers = game_header;
            ho.offset = game_pos;

            header_offsets.append(ho);
            game_pos = -1;
            game_header = QMap<QString,QString>();
        }

        last_pos = in.pos();
        line = in.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        HeaderOffset ho;
        ho.headers = game_header;
        ho.offset = game_pos;

        header_offsets.append(ho);
        game_pos = -1;
        game_header = QMap<QString,QString>();
    }

    return header_offsets;
}


chess::Game* PgnReader::readGameFromFile(const QString &filename, const char* encoding) {
    return this->readGameFromFile(filename, encoding, 0);
}

chess::Game* PgnReader::readGameFromString(QString &pgn_string) {
    QTextStream in(&pgn_string);
    return this->readGame(in);
}

chess::Game* PgnReader::readGameFromString(QString &pgn_string, quint64 offset) {
    QString substring = QString(pgn_string.mid(offset, pgn_string.size()));
    QTextStream in(&substring);
    return this->readGame(in);
}


chess::Game* PgnReader::readGameFromFile(const QString &filename, const char* encoding, qint64 offset) {

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
    //qDebug() << "reading game at: " << offset;
    chess::Game* g = this->readGame(in);
    file.close();
    return g;
}

chess::Game* PgnReader::readGame(QTextStream& in) {

    chess::Game* g = new Game();
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
            // don't add FEN tag explicitly,
            // will be always automatically generated and added
            // when printing a game later...
            if(tag == QString("FEN")) {
                starting_fen = value;
            } else {
                //g->headers.insert(tag,value);
                g->setHeader(tag, value);
            }
        } else {
            break;
        }

        line = in.readLine();
    }
    // try to set starting fen, if available
    if(!starting_fen.isEmpty()) {
        //chess::Board b_fen = 0;
        try {
            chess::Board b_fen(starting_fen);
            if(!b_fen.is_consistent()) {
                std::cerr << "starting fen position is not consistent" << std::endl;
                game_stack->clear();
                delete game_stack;
                return g;
            } else {
                current->setBoard(b_fen);
            }
        }
        catch(std::invalid_argument a) {
            // just silently fail... but first clean up
            //if(b_fen != 0) {
            //    delete b_fen;
            //}
            game_stack->clear();
            delete game_stack;
            std::cerr << a.what() << std::endl;
            return g;
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
            delete game_stack;
            return g;
        }
        QRegularExpressionMatchIterator i = MOVETEXT_REGEX.globalMatch(line);
        while (i.hasNext()) {
            QRegularExpressionMatch match = i.next();
            QString token = match.captured(0);
            if(token.startsWith("%")) {
                line = in.readLine();
                continue;
            }
            if(token.startsWith("{")) {
                line = token.remove(0,1);
                QStringList *comment_lines = new QStringList();
                // get comments - possibly over multiple lines
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
                //Move *m = 0;
                GameNode *next = new GameNode();
                // Board *b_next = 0;
                try {
                    Board b = current->getBoard();
                    //Move m = Move(b.parse_san(token));
                    Move m = Move(b.parse_san_fast(token));
                    Board b_next = Board(b);
                    b_next.apply(m);
                    next->setMove(m);
                    next->setBoard(b_next);
                    next->setParent(current);
                    current->addVariation(next);
                    current = next;
                }
                catch(std::invalid_argument a) {
                    // just silently fail... but first clean up
                    //if(m!=0) {
                    //    delete m;
                    //}
                    //if(b_next!=0) {
                    //    delete b_next;
                    //}
                    delete next;
                    current = g->getRootNode();
                    game_stack->clear();
                    delete game_stack;
                    std::cerr << a.what() << std::endl;
                    return g;
                }
            }
        }
        if(readNextLine) {
            line = in.readLine();
        }
    }
    game_stack->clear();
    delete game_stack;
    return g;
}

QVector<qint64> PgnReader::scanPgn(QString &filename, bool isLatin1) {

    QVector<qint64> offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return offsets;

    bool inComment = false;

    qint64 game_pos = -1;

    QByteArray byteLine;
    QString line("");
    qint64 last_pos = file.pos();

    int i= 0;
    while(!file.atEnd()) {

        //if(offsets.length() % 100000 == 0) {
        //    std::cout << offsets.length() << std::endl;
        //}

        i++;
        byteLine = file.readLine();
        if(isLatin1) {
            line = QString::fromLatin1(byteLine);
        } else {
            line = QString::fromUtf8(byteLine);
        }

        // skip comments
        if(line.startsWith("%")) {
            byteLine = file.readLine();
            continue;
        }

        if(!inComment && line.startsWith("[")) {
            //QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            //if(match_t.hasMatch()) {

                if(game_pos == -1) {
                    game_pos = last_pos;
                }
                last_pos = file.pos();
                byteLine = file.readLine();
                continue;
            //}
        }
        if((!inComment && line.contains("{"))
                || (inComment && line.contains("}"))) {
            inComment = line.lastIndexOf("{") > line.lastIndexOf("}");
        }

        if(game_pos != -1) {
            offsets.append(game_pos);
            game_pos = -1;
        }

        last_pos = file.pos();
        byteLine = file.readLine();
    }
    // for the last game
    if(game_pos != -1) {
        offsets.append(game_pos);
        game_pos = -1;
    }

    return offsets;
}

chess::Game* PgnReader::readGameFromPgnAt(QString &filename, qint64 offset, bool isUtf8) {
    return nullptr;
}


QVector<PgnHeaderOffset> PgnReader::readMultipleHeadersFromPgnAround(QString &filename, QVector<qint64> &offsets, bool isUtf8) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }
    QTextStream in(&file);
    QTextCodec *codec;
    if(isUtf8) {
        codec = QTextCodec::codecForName("UTF-8");
    } else {
        codec = QTextCodec::codecForName("ISO 8859-1");
    }
    in.setCodec(codec);

    QVector<PgnHeaderOffset> HeaderOffsets;

    for(int i=0;i<offsets.size();i++) {

        qint64 offset = offsets.at(i);
        if(offset != 0 && offset > 0) {
            in.seek(offset);
        }

        PgnHeader header;
        bool foundHeader = false;
        bool continueSearch = true;

        QString line = in.readLine();
        while(!in.atEnd() && continueSearch) {
            line = in.readLine();
            if(line.startsWith("%") || line.isEmpty()) {
                line = in.readLine();
                continue;
            }

            QRegularExpressionMatch match_t = TAG_REGEX.match(line);

            if(match_t.hasMatch()) {

                foundHeader = true;

                QString tag = match_t.captured(1);
                QString value = match_t.captured(2);

                if(tag == "Event") {
                    header.event = value;
                }
                if(tag == "Site") {
                    header.site = value;
                }
                if(tag == "Date") {
                    header.date = value;
                }
                if(tag == "Round") {
                    header.round = value;
                }
                if(tag == "White") {
                    header.white = value;
                }
                if(tag == "Black") {
                    header.black = value;
                }
                if(tag == "Result") {
                    header.result = value;
                }
            } else {
                if(foundHeader) {
                    continueSearch = false;
                    break;
                }
            }
        }
        PgnHeaderOffset ho;
        ho.offset = offset;
        ho.header = header;
        HeaderOffsets.append(ho);
    }
    file.close();
    return HeaderOffsets;
}



PgnHeader PgnReader::readSingleHeaderFromPgnAt(QString &filename, qint64 offset, bool isUtf8) {

    QFile file(filename);

    if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        throw std::invalid_argument("unable to open file w/ supplied filename");
    }
    QTextStream in(&file);
    QTextCodec *codec;
    if(isUtf8) {
        codec = QTextCodec::codecForName("UTF-8");
    } else {
        codec = QTextCodec::codecForName("ISO 8859-1");
    }
    in.setCodec(codec);
    if(offset != 0 && offset > 0) {
        in.seek(offset);
    }

    PgnHeader header;
    bool foundHeader = false;
    bool continueSearch = true;

    QString line = in.readLine();
    while(!in.atEnd() && continueSearch) {
        line = in.readLine();
        if(line.startsWith("%") || line.isEmpty()) {
            line = in.readLine();
            continue;
        }

        QRegularExpressionMatch match_t = TAG_REGEX.match(line);

        if(match_t.hasMatch()) {

            foundHeader = true;

            QString tag = match_t.captured(1);
            QString value = match_t.captured(2);

            if(tag == "Event") {
                header.event = value;
            }
            if(tag == "Site") {
                header.site = value;
            }
            if(tag == "Date") {
                header.date = value;
            }
            if(tag == "Round") {
                header.round = value;
            }
            if(tag == "White") {
                header.white = value;
            }
            if(tag == "Black") {
                header.black = value;
            }
            if(tag == "Result") {
                header.result = value;
            }
            if(tag == "ECO") {
                header.eco = value;
            }
        } else {
            if(foundHeader) {
                continueSearch = false;
                break;
            }
        }
    }

    file.close();
    return header;
}



}
