#ifndef PGNDATABASE_H
#define PGNDATABASE_H

#include "chess/database.h"

namespace chess {

class PgnDatabase : public Database
{
public:
    PgnDatabase();
    ~PgnDatabase();

    void setParentWidget(QWidget *parentWidget);
    void open(QString &filename);
    void close();
    int createNew(QString &filename);
    int appendCurrentGame(chess::Game &game);

    int getRowCount();
    Game* getGameAt(int idx);
    PgnHeader getRowInfo(int idx);
    int countGames();
    bool isOpen();

private:
    QVector<qint64> offsets;
    QWidget *parentWidget;
    PgnReader reader;
    QString filename;
    QHash<qint64, chess::PgnHeader> headerCache;
    QVector<qint64> scanPgn(QString &filename, bool isLatin1);
    int cacheSize;
    bool isUtf8;
    bool currentlyOpen;

};
}

#endif // PGNDATABASE_H
