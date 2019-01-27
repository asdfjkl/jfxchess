#include "pgn_reader.h"
#include "pgn_database.h"
#include <QDebug>
#include <QFile>
#include <QProgressDialog>
#include "chess/pgn_printer.h"
#
#include <iostream>

chess::PgnDatabase::PgnDatabase()
{
    this->parentWidget = nullptr;
    this->filename = "";
    //this->cacheSize = 50;
    this->isUtf8 = true;
    this->currentlyOpen = false;
}

chess::PgnDatabase::~PgnDatabase() {

}

bool chess::PgnDatabase::isOpen() {
    return this->currentlyOpen;
}

int chess::PgnDatabase::createNew(QString &filename) {

    QFile file(filename);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        return -1;
    } else {
        QTextStream out(&file);
        out << "\n";
        file.close();

        this->filename = filename;
        this->offsets.clear();
        this->currentlyOpen = true;
        return 0;
    }
}

int chess::PgnDatabase::appendCurrentGame(chess::Game &game) {

    QFile file(this->filename);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Append)) {
        return -1;
    } else {
        QTextStream out(&file);
        out << "\n";
        qint64 gamePos = out.pos();
        chess::PgnPrinter printer;
        QStringList pgn = printer.printGame(game);
        for (int i = 0; i < pgn.size(); ++i) {
            out << pgn.at(i) << '\n';
        }
        file.close();
        this->offsets.append(gamePos);
        return 0;
    }


}

void chess::PgnDatabase::setParentWidget(QWidget *parentWidget) {
    this->parentWidget = parentWidget;
}

QVector<qint64> chess::PgnDatabase::scanPgn(QString &filename, bool isLatin1) {

    QVector<qint64> offsets;
    QFile file(filename);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return offsets;

    bool inComment = false;

    qint64 game_pos = -1;

    QByteArray byteLine;
    QString line("");
    qint64 last_pos = file.pos();

    int size = file.size();
    QProgressDialog progress(this->parentWidget->tr("scanning PGN file..."), this->parentWidget->tr("Cancel"), 0, size, this->parentWidget);
    progress.setMinimumDuration(400);
    progress.setWindowModality(Qt::WindowModal);
    progress.setCancelButton(0);
    progress.show();

    quint64 stepCounter = 0;

    int i= 0;
    while(!file.atEnd()) {

        if(stepCounter %50 == 0) {
            progress.setValue(last_pos);
            stepCounter = 0;
        }
        stepCounter += 1;

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


void chess::PgnDatabase::open(QString &filename) {
    qDebug() << "pgn database: open";

    this->isUtf8 = reader.detectUtf8(filename);
    /*
    const char* utf8 = "UTF-8";
    const char* encoding = reader.detect_encoding(filename);
    int cmp = strcmp(encoding, utf8);
    if(cmp != 0){
        isLatin1 = true;
    }*/
    this->offsets = this->scanPgn(filename, this->isUtf8);
    this->filename = filename;
    this->currentlyOpen = true;

}

void chess::PgnDatabase::close() {
    this->offsets.clear();
    this->filename = "";
    this->currentlyOpen = false;

}

int chess::PgnDatabase::getRowCount() {
    //qDebug() << "pgn database: get row count" << this->offsets.size();

    return this->offsets.size();
}

chess::Game* chess::PgnDatabase::getGameAt(int idx) {
    const char* encoding = reader.detect_encoding(filename);
    chess::Game *g = this->reader.readGameFromFile(this->filename, encoding, this->offsets.at(idx));
    return g;
}

void chess::PgnDatabase::search(SearchPattern &pattern) {

}

/*
chess::PgnHeader chess::PgnDatabase::getRowInfo(int idx) {

    if(this->headerCache.contains(idx)) {
        qDebug() << "in cache";
        chess::PgnHeader header = headerCache.value(idx);
        return header;
    } else {
        int start = std::max(idx - 10, 0);
        int stop = std::min(this->offsets.size(), idx + 30);
        QVector<qint64> cacheOffsets;
        for(int i=start; i<=stop;i++) {
            qint64 oi = this->offsets.at(i);
            cacheOffsets.append(oi);
        }
        const char* utf8 = "UTF-8";
        chess::PgnHeader h;
        if(idx >= this->offsets.size()) {
            return h;
        } else {

            QVector<PgnHeaderOffset> cacheHeaderOffsets = this->reader.readMultipleHeadersFromPgnAround(this->filename, cacheOffsets, utf8);
            for(int i=0;i<cacheHeaderOffsets.size();i++) {
                chess::PgnHeader ci = cacheHeaderOffsets.at(i).header;
                qint64 ii = cacheHeaderOffsets.at(i).offset;
                this->headerCache.insert(ii, ci);
                if(ii == this->offsets.at(idx)) {
                    h = cacheHeaderOffsets.at(i).header;
                }
            }
            return h;

            //qint64 offset = this->offsets.at(idx);
            //chess::PgnHeader h_idx = this->reader.readSingleHeaderFromPgnAt(this->filename, offset, utf8);
            //return h_idx;
        }
    }
}
*/

chess::PgnHeader chess::PgnDatabase::getRowInfo(int idx) {

        chess::PgnHeader h;
        if(idx >= this->offsets.size()) {
            return h;
        } else {

            qint64 offset = this->offsets.at(idx);
            chess::PgnHeader h_idx = this->reader.readSingleHeaderFromPgnAt(this->filename, offset, this->isUtf8);
            return h_idx;
        }
    }


int chess::PgnDatabase::countGames() {
    //qDebug() << "pgn database: count games";

    return this->offsets.size();
}
