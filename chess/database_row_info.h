#ifndef DATABASE_ROW_INFO_H
#define DATABASE_ROW_INFO_H

#include <QString>

namespace chess {

class DatabaseRowInfo
{
public:
    DatabaseRowInfo();

    QString whiteName;
    QString blackName;
    QString whiteElo;
    QString blackElo;
    QString event;
    QString round;
    QString eco;
    QString year;
    QString result;
    bool isDeleted;
};

}

#endif // DATABASE_ROW_INFO_H
