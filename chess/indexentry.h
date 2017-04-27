#ifndef INDEXENTRY_H
#define INDEXENTRY_H

#include "game.h"
#include <QDataStream>

namespace chess {

class IndexEntry
{
public:
    IndexEntry();
    quint64 gameOffset;
    quint8 deleted;
    quint32 whiteOffset;
    quint32 blackOffset;
    quint16 round;
    quint32 siteRef;
    quint32 eventRef;
    quint16 eloWhite;
    quint16 eloBlack;
    quint8 result;
    QString eco;
    quint16 year;
    quint8 month;
    quint8 day;

private:

    friend std::ostream& operator<<(std::ostream& strm, const IndexEntry &entry);
    friend QDataStream& operator>>(QDataStream& strm, IndexEntry &entry);


};

}

#endif // INDEXENTRY_H
