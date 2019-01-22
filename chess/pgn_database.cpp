#include "pgn_database.h"
#include "pgn_reader.h"

chess::PgnDatabase::PgnDatabase()
{
    this->parentWidget = nullptr;
    this->filename = "";
}

chess::PgnDatabase::~PgnDatabase() {

}

void chess::PgnDatabase::setParentWidget(QWidget *parentWidget) {
    this->parentWidget = parentWidget;
}

void chess::PgnDatabase::open(QString &filename) {
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
    return this->offsets.size();
}

chess::Game* chess::PgnDatabase::getGameAt(int idx) {
    chess::Game *g = new chess::Game();
    return g;
}

chess::PgnHeader chess::PgnDatabase::getRowInfo(int idx) {
    const char* utf8 = "UTF-8";
    chess::PgnHeader h;
    if(idx >= this->offsets.size()) {
        return h;
    } else {
        qint64 offset = this->offsets.at(idx);
        chess::PgnHeader h_idx = this->reader.readHeaderFromPgnAt(this->filename, offset, utf8);
        return h_idx;
    }
}

int chess::PgnDatabase::countGames() {
    return 0;
}
