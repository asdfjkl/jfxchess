#ifndef DATABASE_H
#define DATABASE_H

#include <QVector>
#include "chess/database_row_info.h"
#include "chess/game.h"
#include "model/search_pattern.h"

namespace chess {

class Database
{
public:

    virtual ~Database() = 0;

    virtual void setParentWidget(QWidget *parentWidget) = 0;
    virtual void open(QString &filename) = 0;
    virtual void close() = 0;
    virtual void exportDB(QString &outFilename, QVector<int> &indices, int outType) = 0;
    virtual void search(SearchPattern &sp) = 0;
    virtual QString getFilename() = 0;
    // next functions are w.r.t. the current active index
    virtual int getRowCount() = 0;
    virtual Game* getGameAt(int idx) = 0;
    virtual DatabaseRowInfo getRowInfo(int idx) = 0;
    virtual int countGames() = 0;
    // absolute index: idx is a value from all database entries
    // including currently non-displayed games
    virtual Game* getGameAtAbsoluteIndex(int idx) = 0;

};

}


#endif // DATABASE_H
