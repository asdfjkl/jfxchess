#ifndef POLYGLOT_H
#define POLYGLOT_H

#include <QString>
#include <QFile>
#include "move.h"
#include "board.h"

namespace chess {

struct Entry
{
    quint64 key;
    quint16 move;
    quint16 weight;
    quint32 learn;
};

class Polyglot
{
public:
    Polyglot(QString &bookname);
    Moves* findMoves(Board *board);
    bool inBook(Board *board);

private:
    QByteArray* book;
    Entry entryFromOffset(int offset);
    Move moveFromEntry(Entry e);
    bool readFile;

};

}

#endif // POLYGLOT_H
