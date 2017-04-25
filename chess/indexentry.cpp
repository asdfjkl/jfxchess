#include "indexentry.h"

namespace chess {

IndexEntry::IndexEntry()
{

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

