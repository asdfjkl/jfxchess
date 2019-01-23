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

    int getRowCount();
    Game* getGameAt(int idx);
    PgnHeader getRowInfo(int idx);
    int countGames();

private:
    QVector<qint64> offsets;
    QWidget *parentWidget;
    PgnReader reader;
    QString filename;
    QHash<qint64, chess::PgnHeader> headerCache;
    int cacheSize;

};
}

#endif // PGNDATABASE_H
