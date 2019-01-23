#include "pgn_reader.h"
#include "pgn_database.h"
#include <QDebug>

chess::PgnDatabase::PgnDatabase()
{
    this->parentWidget = nullptr;
    this->filename = "";
    this->cacheSize = 50;

}

chess::PgnDatabase::~PgnDatabase() {

}

void chess::PgnDatabase::setParentWidget(QWidget *parentWidget) {
    this->parentWidget = parentWidget;
}

void chess::PgnDatabase::open(QString &filename) {
    qDebug() << "pgn database: open";

    const char* utf8 = "UTF-8";
    const char* encoding = reader.detect_encoding(filename);
    bool isLatin1 = false;
    int cmp = strcmp(encoding, utf8);
    if(cmp != 0){
        isLatin1 = true;
    }
    this->offsets = this->reader.scanPgn(filename, isLatin1);
    this->filename = filename;
}

void chess::PgnDatabase::close() {

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

        const char* utf8 = "UTF-8";
        chess::PgnHeader h;
        if(idx >= this->offsets.size()) {
            return h;
        } else {

            qint64 offset = this->offsets.at(idx);
            chess::PgnHeader h_idx = this->reader.readSingleHeaderFromPgnAt(this->filename, offset, utf8);
            return h_idx;
        }
    }


int chess::PgnDatabase::countGames() {
    //qDebug() << "pgn database: count games";

    return this->offsets.size();
}
