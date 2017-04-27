#include "indexentry.h"
#include "constants.h"

namespace chess {

IndexEntry::IndexEntry()
{

}

QDataStream& operator>>(QDataStream& in, IndexEntry &entry) {

    quint8 status;
    in >> status;

    if(status == GAME_DELETED) {
        entry.deleted = true;
    } else {
        entry.deleted = false;
    }
    in >> entry.gameOffset;
    in >> entry.whiteOffset;
    in >> entry.blackOffset;
    in >> entry.round;
    in >> entry.siteRef;
    in >> entry.eventRef;
    in >> entry.eloWhite;
    in >> entry.eloBlack;
    in >> entry.result;

    // next is slightly cumbersome but works
    quint8 eco_0;
    quint8 eco_1;
    quint8 eco_2;
    in >> eco_0;
    in >> eco_1;
    in >> eco_2;
    entry.eco = QString("A00");
    entry.eco[0] = QChar(eco_0);
    entry.eco[1] = QChar(eco_1);
    entry.eco[2] = QChar(eco_2);
    // yy mm dd
    in >> entry.year;
    in >> entry.month;
    in >> entry.day;

    return in;
}


std::ostream& operator<<(std::ostream &strm, const IndexEntry &entry) {

    strm << "Offset        : " << entry.gameOffset << std::endl;
    strm << "id deleted    : " << entry.deleted << std::endl;
    strm << "white offset  : " << entry.whiteOffset << std::endl;
    strm << "black offset  : " << entry.blackOffset << std::endl;
    strm << "round         : " << entry.round << std::endl;
    strm << "site          : " << entry.siteRef << std::endl;
    strm << "event         : " << entry.eventRef << std::endl;
    strm << "elo white     : " << entry.eloWhite << std::endl;
    strm << "elo black     : " << entry.eloBlack << std::endl;
    strm << "result        : " << entry.result << std::endl;
    strm << "eco           : " << entry.eco.toStdString() << std::endl;
    strm << "year          : " << entry.month << std::endl;
    strm << "day           : " << entry.day << std::endl;
    strm << std::endl;

    return strm;
}



}

